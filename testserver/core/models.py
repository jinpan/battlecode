from datetime import datetime
from itertools import chain
from itertools import product
from os import chdir
from os import listdir
from os.path import join
from time import sleep
from xml.etree import ElementTree

from django.conf import settings
from django.contrib.auth.models import User
from django.db import models
from django.db import transaction
from django.utils import timezone

from testserver.core.exceptions import SimulationRaceConditionException
from testserver.core.utils import parse_result
from testserver.core.utils import run_simulation
from testserver.core import constants


class File(models.Model):
    name = models.CharField(max_length=200, default='RobotPlayer.java', db_index=True)
    content = models.TextField()

    robot = models.ForeignKey('Robot')

    def __unicode__(self):
        return '%s [%s]' % (self.name, self.robot.line.name)


    def save(self, *args, **kwargs):

        if not self.name.endswith('.java'):
            self.name += '.java'

        super(File, self).save(*args, **kwargs)


class MapManager(models.Manager):

    def initialize(self):
        files = listdir(join(settings.BATTLECODE_ROOT, 'maps'))
        maps = []

        for filename in files:
            tree = ElementTree.parse(join(settings.BATTLECODE_ROOT, 'maps', filename))
            root = tree.getroot()

            height, width = root.attrib['height'], root.attrib['width']

            size = (float(height) * float(width)) ** 0.5
            maps.append(Map(name=filename, size=size))
        
        self.bulk_create(maps)


class Map(models.Model):
    name = models.CharField(max_length=100, unique=True)
    size = models.IntegerField(default=35, db_index=True)

    objects = MapManager()

    class Meta:
        ordering = ['name']


    def __unicode__(self):
        return self.name


class Robot(models.Model):
    active = models.BooleanField(default=True, db_index=True)
    create_time = models.DateTimeField(auto_now_add=True, db_index=True)
    creator = models.ForeignKey(User)
    line = models.ForeignKey('RobotLine')
    line_num = models.IntegerField(null=True, db_index=True)
    name = models.CharField(max_length=100, blank=True)

    simulated_opponents = models.ManyToManyField('Robot')

    def __unicode__(self):
        return self.name

    def save(self, *args, **kwargs):

        super(Robot, self).save(*args, **kwargs)

        if not self.line_num:
            # TODO: binary search?  Linear search time may be dwarfed by database transaction time though.
            for idx, robot in enumerate(Robot.objects.filter(line=self.line).order_by('create_time')):
                self.line_num = idx + 1
            self.name = '%s_%d' % (self.line.name, self.line_num)
            super(Robot, self).save(*args, **kwargs)  # so simulate only gets called once
            self.simulate()


    def get_predecessor(self):
        potential = Robot.objects.filter(line=self.line, line_num__lt=self.line_num, active=True).order_by('-create_time')[:1]
        try:
            return potential[0]
        except IndexError:
            return None

    
    def get_successor(self):
        potential = Robot.objects.filter(line=self.line, line_num__gt=self.line_num, active=True).order_by('create_time')[:1]
        try:
            return potential[0]
        except IndexError:
            return None


    def get_worthy_opponents(self):
        '''
        Returns a generator of worthy opponents
        '''
        predecessor = self.get_predecessor()
        if predecessor:
            yield predecessor
        for robot in Robot.objects.exclude(line__name=self.line.name).filter(active=True):
            yield robot
        yield None


    def simulate(self):
        for opponent in self.get_worthy_opponents():
            Simulation.objects.create_simulations(self, opponent)


class RobotLine(models.Model):
    name = models.CharField(max_length=100, unique=True)
    alive = models.BooleanField(default=True, db_index=True)

    def __unicode__(self):
        return self.name


class SimulationManager(models.Manager):

    def create_simulations(self, robot_a, robot_b):
        simulations = [Simulation(robot_a=robot_a, robot_b=robot_b, map_file=map_file)
                       for map_file in Map.objects.all()]
        self.bulk_create(simulations)

        if robot_b:
            robot_a.simulated_opponents.add(robot_b)
            robot_a.save()
            robot_b.simulated_opponents.add(robot_a)
            robot_b.save()


    def get_results(self, robot_a, robot_b, since=None):
        '''
        Returns three lists: simulations it won on, lost on, tied on
        Should only take in two different robots
        '''
        if robot_b:
            assert robot_a.pk > robot_b.pk

        results = [[], [], []]
        mapping = {
            constants.RESULT.ROBOT_A: 0,
            constants.RESULT.ROBOT_B: 1,
            constants.RESULT.TIE: 2,
        }
        query_kwargs = {
            'robot_a': robot_a,
            'robot_b': robot_b,
            'status': constants.STATUS.CLOSED,
        }
        if since:
            query_kwargs['finish_time__gte'] = since

        for simulation in self.select_related().filter(**query_kwargs):
            results[mapping[simulation.winner]].append(simulation)
        return results
    

    @transaction.atomic
    def get_next_simulation(self, retry=False):
        if retry:
            potential = self.select_for_update().filter(status__in=(constants.STATUS.FAILED, constants.STATUS.OPEN)).order_by('-priority')[:1]
        else:
            potential = self.select_for_update().filter(status=constants.STATUS.OPEN).order_by('-priority')[:1]
        try:
            simulation = potential[0]
            simulation.status = constants.STATUS.PENDING
            simulation.save()
            return simulation
        except IndexError:
            return None
 

class Simulation(models.Model):
    '''
    Class for running simulations.  Robot_a should be the new robot, and robot_b should be an existing one.
    Robot_a should have a higher id than robot_b.
    '''

    robot_a = models.ForeignKey('Robot', related_name='simulation_a')
    robot_b = models.ForeignKey('Robot', related_name='simulation_b', null=True, blank=True)
    map_file = models.ForeignKey('Map')

    priority = models.FloatField(default=0)
    result = models.TextField(null=True, blank=True, db_index=True)
    error = models.TextField(null=True, blank=True)

    status = models.CharField(max_length=1,
                              choices=constants.STATUS_CHOICES,
                              default=constants.STATUS.OPEN,)
    winner = models.CharField(max_length=1,
                              choices=constants.RESULT_CHOICES,
                              null=True, blank=True)
    tie = models.NullBooleanField(default=None, db_index=True)
    rounds = models.IntegerField(null=True, blank=True)

    create_time = models.DateTimeField(auto_now_add=True, db_index=True)
    finish_time = models.DateTimeField(null=True, blank=True, db_index=True)

    name = models.CharField(max_length=200)

    objects = SimulationManager()


    class Meta:
        ordering = ['map_file__name']


    def __init__(self, *args, **kwargs):
        super(Simulation, self).__init__(*args, **kwargs)

        if not self.priority:
            self.priority = self.calculate_priority()
        if not self.name:
            self.name = '%s vs %s on %s' % (self.robot_a,
                                            self.robot_b or constants.DEFAULT_AI,
                                            self.map_file.name)
    

    def __unicode__(self):
        return self.name
    

    def calculate_priority(self):
        if not self.robot_b:
            prior = 0.3
        elif self.robot_a.line == self.robot_b.line:  # same line
            prior = 0.5
        else:
            prior_results = []
            ancestors = Robot.objects.filter(line=self.robot_a.line).exclude(pk=self.robot_a.pk).order_by('line_num')
            for ancestor in ancestors:
                try:
                    simulations = Simulation.objects.filter(robot_a=ancestor, robot_b=self.robot_b,
                                                            status=constants.STATUS.CLOSED)
                except Simulation.DoesNotExist:
                    simulations = Simulation.objects.filter(robot_a=self.robot_b, robot_b=ancestor,
                                                            status=constants.STATUS.CLOSED)
                
                for simulation in simulations:
                    if simulation.result == 'T':
                        prior_results.append(0.5)
                    elif ((simulation.robot_a == ancestor and simulation.result == 'A')
                            or (simulation.robot_b == ancestor and simulation.result == 'B')):
                        prior_results.append(1)
                    else:
                        prior_results.append(0)
            if len(prior_results):
                weights = range(1, len(prior_results) + 1)
                prior = sum((result * weight) for result, weight in zip(prior_results, weights)) / float(sum(weights))
            else:
                prior = 0.5

        # prior_ranking's should be at least 1
        prior_ranking = -19600. * prior ** 2 + 19600. * prior + 1  # (70 ** 2) * 4* ((x-0.5)^2 + 0.25) + 1

        return prior_ranking / self.map_file.size


    def get_files(self, robot):
        '''
        Returns a list of tuples (name, content)
        '''
        if robot:
            return [(javafile.name, javafile.content) for javafile in robot.file_set.all()]
        else:
            return []


    def save_result(self, result, winner, tie, rounds, error, sim_data):

        self.status = constants.STATUS.CLOSED
        self.result = result
        self.winner = winner
        self.tie = tie
        self.rounds = rounds
        self.error = error
        self.finish_time = timezone.now()

        self.save()

        sim_file = SimulationFile(simulation=self, data=sim_data)
        sim_file.save()


    def set_error(self):
        self.status = constants.STATUS.FAILED
        self.priority = -1
        self.save()


    def run(self):
        result, sim_data, error = run_simulation(self.pk, self.map_file.name,
                                                 self.get_files(self.robot_a),
                                                 self.get_files(self.robot_b))
        winner, tie, rounds = parse_result(result)
        self.save_result(result, winner, tie, rounds, error, sim_data)


class SimulationFile(models.Model):

    simulation = models.OneToOneField('Simulation')
    data = models.BinaryField()

    
    def __unicode__(self):
        return u'result_%d.rms' % (self.pk, )


class SimulationSetManager(models.Manager):

    def get_or_create(self, robot_a, robot_b=None):
        if robot_b:
            assert robot_a.pk > robot_b.pk

        result = None
        prefetch = (
            'robot_a_win_simulations', 'robot_a_win_simulations__map_file', 'robot_a_win_simulations__simulationfile',
            'robot_a_lose_simulations', 'robot_a_lose_simulations__map_file', 'robot_a_lose_simulations__simulationfile',
            'robot_a_tie_simulations', 'robot_a_tie_simulations__map_file', 'robot_a_tie_simulations__simulation_file',
        )
        select = ('robot_a', 'robot_b')

        try:
            result = self.select_related(*select).prefetch_related(*prefetch).get(robot_a=robot_a, robot_b=robot_b)
            if result.complete:
                return result
            else:
                update = True
        except SimulationSet.DoesNotExist:
            update = False
            result = SimulationSet(robot_a=robot_a, robot_b=robot_b)
            result.save()

        if update:
            wins, losses, ties = Simulation.objects.get_results(robot_a, robot_b, since=result.updated_time)
        else:
            wins, losses, ties = Simulation.objects.get_results(robot_a, robot_b)

        result.updated_time = timezone.now()

        result.robot_a_win_simulations.add(*wins)
        result.robot_a_lose_simulations.add(*losses)
        result.robot_a_tie_simulations.add(*ties)

        result.complete = (result.total_matches >= constants.NUM_MAPS)
        result.save()

        return result


class SimulationSet(models.Model):
    '''
    Denormalized table for storing aggregated simulations
    '''
    name = models.CharField(max_length=100)

    robot_a = models.ForeignKey('Robot', related_name='simulationset_a')
    robot_b = models.ForeignKey('Robot', related_name='simulationset_b', null=True, blank=True)

    robot_a_win_simulations = models.ManyToManyField('Simulation', related_name='robot_a_win_simulations')
    robot_a_lose_simulations = models.ManyToManyField('Simulation', related_name='robot_a_lose_simulations')
    robot_a_tie_simulations = models.ManyToManyField('simulation', related_name='robot_a_tie_simulations')

    updated_time = models.DateTimeField(auto_now=True)

    complete = models.BooleanField(db_index=True, default=False)

    objects = SimulationSetManager()

    def __unicode__(self):
        return self.name


    def save(self, *args, **kwargs):
        if not self.name:
            self.name = '%s vs %s' % (self.robot_a, self.robot_b or constants.DEFAULT_AI)

        super(SimulationSet, self).save(*args, **kwargs)


    @property
    def total_matches(self):
        if self.complete:
            return constants.NUM_MAPS
        else:
            return self.robot_a_win_simulations.count() + self.robot_a_lose_simulations.count() + self.robot_a_tie_simulations.count()

