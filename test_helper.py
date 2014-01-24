#! /usr/bin/python

from os import getpid
from re import search
from subprocess import Popen
from subprocess import PIPE
from sys import argv
from sys import stdout

from templates import bc_template
from templates import build_template


maps = ('backdoor', 'blocky', 'desolation', 'donut', 'flytrap', 'fuzzy', 'magnetism', 'neighbors', 'overcast', 'rushlane', 'smiles', 'stitch', 'temple', 'traffic', 'valve', 'bakedpotato', 'castles', 'divide', 'flagsoftheworld', 'friendly', 'itsatrap', 'meander', 'onramp', 'reticle', 'siege', 'steamedbuns', 'sweetspot', 'terra', 'troll', 'ventilation')


if __name__ == '__main__':
    robot_a = argv[1]
    robot_b = argv[2]
    map_idx = argv[3]

    map_file = maps[int(map_idx)]

    build_file = '%d.xml' % getpid()
    conf_file = '%d.conf' % getpid()

    map_regex = r'%s vs. %s on %s' % (robot_a, robot_b, map_file)
    result_regex = r'\(A|B\) wins'

    with open(build_file, 'wb') as f:
        f.write(build_template % ((conf_file, ) * 4))

    with open(conf_file, 'wb') as f:
        f.write(bc_template % (robot_a, robot_b, map_file))

    p = Popen('ant -buildfile %s file' % build_file, shell=True, stdout=PIPE)
    for line in p.stdout.readlines():
        if search(map_regex, line):
            output = line
        elif search(result_regex, line):
            print output + line,
            stdout.flush()

    p = Popen('rm %s %s' % (build_file, conf_file), shell=True)


