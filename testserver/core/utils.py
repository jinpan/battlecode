from calendar import timegm
from datetime import datetime
from gzip import open as gopen
from os import chdir
from os.path import join
from re import match
from subprocess import call
from subprocess import Popen
from subprocess import PIPE

from django.conf import settings
from django.template.loader import render_to_string

from testserver.core import constants


def parse_result(result, sim_file):
    '''
    Takes in the stdout and compressed sim_file contents
    '''
    result_pattern = r'^\s+\[java\] \[server]\s+(a_\d+|b_\d+|basicplayer) \((A|B)\) wins$'
    tie_pattern = r'^\s+\[java\] Reason: The winning team won on tiebreakers.$'
    round_pattern = r'\s+\[java\] Round: (\d+)'

    tie = False
    print result
    for line in result.split('\n'):
        if match(round_pattern, line):
            re_result = match(round_pattern, line)
            rounds = re_result.group(1)
        elif match(result_pattern, line):
            re_result = match(result_pattern, line)
            winner = re_result.group(2)
        elif match(tie_pattern, line):
            tie = True
    
    return winner, tie, rounds


def run_simulation(simulation_id, map_file,
                   robot_a_files,
                   robot_b_files=[]):


    def download(package, files):
        if package == constants.DEFAULT_AI:
            return
        call(['mkdir', join('teams', package)])
        for name, content in files:
            # remove the original package name
            new_line = content.index('\n')
            content = content[new_line + 1:]

            # add in our package name
            content = 'package %s;\n%s' % (package, content)

            with open(join('teams', package, name), 'w') as f:
                f.write(content)

    def remove(package):
        if package == constants.DEFAULT_AI:
            return
        call(['rm', '-r', join('teams', package)])
        call(['rm', '-r', join('bin', package)])

    chdir(settings.BATTLECODE_ROOT)

    robot_a = 'a_%d' % simulation_id
    robot_b = 'b_%d' % simulation_id if robot_b_files else constants.DEFAULT_AI

    # download robots
    download(robot_a, robot_a_files)
    download(robot_b, robot_b_files)

    # generate bc.conf and build.xml

    conf_name = 'bc_%d.conf' % simulation_id
    build_name = 'build_%d.xml' % simulation_id
    results_name = join('results', '%d.rms' % simulation_id)
    conf_fields = {
        'map_file': map_file,
        'team_a': robot_a, 
        'team_b': robot_b,
        'results': results_name,
        'conf_name': conf_name,
    }

    with open(conf_name, 'w') as f:
        f.write(render_to_string('headless.conf', conf_fields))

    with open(build_name, 'w') as f:
        f.write(render_to_string('build.xml', conf_fields))

    # run the simulation
    process = Popen(['ant', '-buildfile', build_name, 'file'], stdout=PIPE, stderr=PIPE)
    result, error = process.communicate()
    process.stdout.close()
    process.stderr.close()

    # cleanup
    with open(results_name) as f:
        result_file = f.read()

    call(['rm', conf_name, build_name, results_name])
    remove(robot_a)
    remove(robot_b)

    return result, result_file, error

