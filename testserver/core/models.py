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

    def save(self, *args, **kwargs):

        if not self.name.endswith('.java'):
            self.name += '.java'

        super(File, self).save(*args, **kwargs)


    def __unicode__(self):
        return '%s [%s]' % (self.name, self.robot.line.name)


class RobotLine(models.Model):
    name = models.CharField(max_length=100, db_index=True)
    alive = models.BooleanField(default=True, db_index=True)

    def __unicode__(self):
        return self.name


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

    def __unicode__(self):
        return self.name


class Robot(models.Model):
    creator = models.ForeignKey(User)
    line = models.ForeignKey('RobotLine')
    line_num = models.IntegerField(null=True, db_index=True)
    create_time = models.DateTimeField(auto_now_add=True, db_index=True)

    simulated_opponents = models.ManyToManyField('Robot')

    def __unicode__(self):
        return '%s_%d' % (self.line.name, self.line_num)


    def save(self, *args, **kwargs):
        super(Robot, self).save(*args, **kwargs)

        if not self.line_num:
            # TODO: binary search?  Linear search time may be dwarfed by database transaction time though.
            for idx, robot in enumerate(Robot.objects.filter(line=self.line).order_by('create_time')):
                self.line_num = idx + 1
                self.save()
                self.simulate()


    def get_predecessor(self):
        potential = Robot.objects.filter(line_num__lt=self.line_num).order_by('-create_time')[:1]
        if potential.count():
            return potential[0]
        else:
            return None

    
    def get_successor(self):
        potential = Robot.objects.filter(line_num__gt=self.line_num).order_by('create_time')[:1]
        if potential.count():
            return potential[0]
        else:
            return None


    def get_match_result(self, other_robot):
        '''
        Returns three lists: maps it won on, lost on, tied on
        '''
        results = [[], [], []]
        for simulation in Simulation.objects.filter(robot_a=self, robot_b=other_robot,
                                                    status=constants.STATUS.CLOSED).prefetch_related('map_file'):
            mapping = {
                constants.RESULT.ROBOT_A: 0,
                constants.RESULT.ROBOT_B: 1,
                constants.RESULT.TIE: 2,
            }
            results[mapping[simulation.winner]].append(simulation.map_file)
        for simulation in Simulation.objects.filter(robot_a=other_robot, robot_b=self,
                                                    status=constants.STATUS.CLOSED).prefetch_related('map_file'):
            mapping = {
                constants.RESULT.ROBOT_A: 1,
                constants.RESULT.ROBOT_B: 0,
                constants.RESuLT.TIE: 2,
            }
            results[mapping[simulation.winner]].append(simulation.map_file)
        return results


    def get_worthy_opponents(self):
        '''
        Returns a generator of worthy opponents
        '''
        predecessor = self.get_predecessor()
        if predecessor:
            yield predecessor
        for line in RobotLine.objects.exclude(name=self.line.name):
            yield Robot.objects.filter(line=line).order_by('-line_num')[0]
        yield None


    def simulate(self):
        for opponent in self.get_worthy_opponents():
            Simulation.objects.create_simulations(self, opponent)


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
    

    @transaction.atomic
    def get_next_simulation(self):
        potential = self.select_for_update().filter(status=constants.STATUS.OPEN).order_by('-priority')[:1]
        if potential.count():
            simulation = potential[0]
            simulation.status = constants.STATUS.PENDING
            simulation.save()
            return simulation
        else:
            return None
 

    def worker(self, sleep_interval):
        while True:
            simulation = self.get_next_simulation()
            if simulation:
                return simulation.run()
            else:
                sleep(sleep_interval)


class Simulation(models.Model):
    '''
    Class for running simulations.  Robot_a should be the new robot, and robot_b should be an existing one.
    '''

    robot_a = models.ForeignKey('Robot', related_name='simulation_a')
    robot_b = models.ForeignKey('Robot', related_name='simulation_b', null=True, blank=True)
    map_file = models.ForeignKey('Map')

    priority = models.FloatField(default=0)
    result = models.TextField(null=True, blank=True, db_index=True)
    sim_file = models.TextField(null=True, blank=True)
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

    objects = SimulationManager()

    def __init__(self, *args, **kwargs):
        super(Simulation, self).__init__(*args, **kwargs)

        if not self.priority:
            self.priority = self.calculate_priority()
    

    def __unicode__(self):
        return '%s vs %s on %s' % (self.robot_a or constants.DEFAULT_AI,
                                   self.robot_b or constants.DEFAULT_AI,
                                   self.map_file.name)
    

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

        prior_ranking = -19600. * prior ** 2 + 19600. * prior  # (70 ** 2) * 4* ((x-0.5)^2 + 0.25)

        return prior_ranking / self.map_file.size


    def get_files(self, robot):
        '''
        Returns a list of tuples (name, content)
        '''
        if robot:
            return [(javafile.name, javafile.content) for javafile in robot.file_set.all()]
        else:
            return []


    def run(self):
        result, sim_file, error = run_simulation(self.pk, self.map_file.name,
                                                 self.get_files(self.robot_a),
                                                 self.get_files(self.robot_b))

        self.status = constants.STATUS.CLOSED
        self.result = result
        winner, tie, rounds = parse_result(result)
        self.winner = winner
        self.tie = tie
        self.rounds = rounds
        self.sim_file = sim_file
        self.error = error
        self.finish_time = timezone.now()

        self.save()


class SimulationSetManager(models.Manager):

    def get_or_create(self, robot_a, robot_b):
        result = list(chain(self.filter(robot_a=robot_a, robot_b=robot_b),
                            self.filter(robot_a=robot_b, robot_b=robot_a)))
        if result:
            if result[0].complete:
                return result[0]
            else:
                result[0].delete()

        simulations_a = Simulation.objects.filter(robot_a=robot_a, robot_b=robot_b, status=constants.STATUS.CLOSED)
        simulations_b = Simulation.objects.filter(robot_a=robot_b, robot_b=robot_a, status=constants.STATUS.CLOSED)

        complete = ((simulations_a.count() + simulations_b.count()) >= Map.objects.all().count())
        result = SimulationSet(robot_a=robot_a, robot_b=robot_b, complete=complete)
        result.save()
        for simulation in simulations_a:
            if simulation.winner == constants.RESULT.ROBOT_A:
                result.robot_a_win_maps.add(simulation.map_file)
            elif simulation.winner == constants.RESULT.ROBOT_B:
                result.robot_a_lose_maps.add(simulation.map_file)
            else:
                result.robot_a_tie_maps.add(simulation.map_file)
        for simulation in simulations_b:
            if simulation.winner == constants.RESULT.ROBOT_A:
                result.robot_a_lose_maps.add(simulation.map_file)
            elif simulation.winner == constants.RESULT.ROBOT_B:
                result.robot_a_win_maps.add(simulation.map_file)
            else:
                result.robot_a_tie_maps.add(simulation.map_file)
        result.save()

        return result


class SimulationSet(models.Model):
    '''
    Denormalized table for storing aggregated simulations
    '''

    robot_a = models.ForeignKey('Robot', related_name='simulationset_a')
    robot_b = models.ForeignKey('Robot', related_name='simulationset_b', null=True, blank=True)

    robot_a_win_maps = models.ManyToManyField('Map', related_name='robot_a_win_maps')
    robot_a_lose_maps = models.ManyToManyField('Map', related_name='robot_a_lose_maps')
    robot_a_tie_maps = models.ManyToManyField('Map', related_name='robot_a_tie_maps')

    complete = models.BooleanField(db_index=True)

    objects = SimulationSetManager()

    def __unicode__(self):
        return '%s vs %s' % (self.robot_a or constants.DEFAULT_AI,
                             self.robot_b or constants.DEFAULT_AI)

    @property
    def robot_b_win_maps(self):
        return self.robot_a_lose_maps


    @property
    def robot_b_lose_maps(self):
        return self.robot_a_win_maps


    @property
    def robot_b_tie_maps(self):
        return self.robot_a_tie_maps


    @property
    def total_matches(self):
        return self.robot_a_win_maps.count() + self.robot_a_lose_maps.count() + self.robot_a_tie_maps.count()

