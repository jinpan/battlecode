from itertools import product

from django.contrib.auth.models import User
from django.db import models

from testserver.core.tasks import run_simulation
from testserver.core import constants


class File(models.Model):
    name = models.CharField(max_length=200)
    content = models.TextField()

    robot = models.ForeignKey('Robot')

    def save(self, *args, **kwargs):

        if not self.name.endswith('.java'):
            self.name += '.java'

        super(File, self).save(*args, **kwargs)

    def __unicode__(self):
        return '%s [%s]' % (self.name, self.robot.line.name)


class RobotLine(models.Model):
    name = models.CharField(max_length=100)

    def __unicode__(self):
        return self.name


class Map(models.Model):
    name = models.CharField(max_length=100)

    def __unicode__(self):
        return self.name


class Robot(models.Model):
    user = models.ForeignKey(User)
    line = models.ForeignKey('RobotLine')
    line_num = models.IntegerField()

    def __unicode__(self):
        return '%s_%d' % (self.line.name, self.line_num)

    def save(self, *args, **kwargs):
        if not self.id:  # first time saved
            self.line_num = self.line.robot_set.count() + 1

        super(Robot, self).save(*args, **kwargs)

    def get_worthy_opponents(self):
        return [None]

    def simulate(self):
        opponents = self.get_worthy_opponents()
        for opp, map_file in product(opponents, Map.objects.all()):
            simulation = Simulation(robot_a=self, robot_b=opp, map_file=map_file)
            simulation.save()
            simulation.start()


class Simulation(models.Model):

    robot_a = models.ForeignKey('Robot', related_name='robot_a', null=True, blank=True)
    robot_b = models.ForeignKey('Robot', related_name='robot_b', null=True, blank=True)
    map_file = models.ForeignKey('Map')

    simulated = models.BooleanField(default=False)
    result = models.TextField(null=True, blank=True)
    error = models.TextField(null=True, blank=True)

    def __unicode__(self):
        return '%s vs %s on %s' % (self.robot_a or constants.DEFAULT_AI,
                                   self.robot_b or constants.DEFAULT_AI,
                                   self.map_file.name)


    @property
    def robot_a_package(self):
        return self.get_package(self.robot_a)

    @property
    def robot_b_package(self):
        return self.get_package(self.robot_b)

    def get_package(self, robot):
        if robot:
            return '%s_%d' % (robot.line.name, self.id)
        else:
            return constants.DEFAULT_AI

    def get_files(self, robot):
        # returns a list of tuples (name, content)

        result = []
        for javafile in robot.file_set.all():
            text = javafile.content.lstrip()

            new_line = text.index('\n')
            text = text[new_line + 1:]

            package = self.get_package(robot)

            content = 'package %s;\n%s' % (package, text)
            result.append((javafile.name, content))
        return result

    def start(self):
        self.simulated = True
        process = run_simulation.delay(self)
        self.save()

