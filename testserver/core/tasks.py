from calendar import timegm
from datetime import datetime
from os import chdir
from os.path import join
from subprocess import call
from subprocess import Popen
from subprocess import PIPE

from django.conf import settings
from django.template.loader import render_to_string
from djcelery import celery


@celery.task
def run_simulation(simulation):
    chdir(settings.BATTLECODE_ROOT)

    def download(robot):
        package = simulation.get_package(robot)
        files = simulation.get_files(robot)
        
        call(['mkdir', join('teams', package)])
        for name, content in files:
            with open(join('teams', package, name), 'w') as f:
                f.write(content)

    def remove(robot):
        call(['rm', '-r', join('teams', simulation.get_package(robot))])

    # download robots
    if simulation.robot_a:
        download(simulation.robot_a)
        robot_a = simulation.robot_a_package
    else:
        robot_a = 'basicplayer'
    if simulation.robot_b:
        download(simulation.robot_b)
        robot_b = simulation.robot_b_package
    else:
        robot_b = 'basicplayer'

    # generate bc.conf

    conf_fields = {
        'map_file': simulation.map_file.name,
        'team_a': robot_a, 
        'team_b': robot_b,
        'results': join('results', '%d.rms' % simulation.id)
    }

    with open('bc.conf', 'w') as f:
        f.write(render_to_string('headless.conf', conf_fields))

    # run the simulation
    process = Popen(['ant', 'file'], stdout=PIPE)
    result, error = process.communicate()

    # cleanup
    if simulation.robot_a:
        remove(simulation.robot_a)
    if simulation.robot_b:
        remove(simulation.robot_b)

    return result, error

