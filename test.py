#! /usr/bin/python

from re import search
from Queue import Empty
from Queue import Queue
from subprocess import Popen
from subprocess import PIPE
from sys import argv
from sys import stdout
from threading import Thread


def enqueue_output(out, queue):
    for line in iter(out.readline, b''):
        queue.put(line)
    out.close()


def output(robot_a, robot_b, a_wins, b_wins, left):
    return '%s won on %d maps; %s won on %d maps.  %d more maps to simulate' % (robot_a, a_wins, robot_b, b_wins, left)


if __name__ == '__main__':
    robot_a = argv[1]
    robot_b = argv[2]

    a_wins, b_wins = 0, 0
    max_maps = 30
    a_win_maps, b_win_maps = [], []

    map_regex = r'%s vs. %s on (\w+)' % (robot_a, robot_b)
    result_regex = r'\((A|B)\) wins'

    p = Popen('seq 0 29 | parallel -j 8 ./test_helper.py %s %s' % (robot_a, robot_b), shell=True, stdout=PIPE)
    q = Queue()
    t = Thread(target=enqueue_output, args=(p.stdout, q))
    t.daemon = True
    t.start()

    output_msg = ''

    while True:
        try:
            line = q.get(timeout=1)
        except Empty:
            if p.poll() is not None:
                break
        else:
            if search(map_regex, line):
                last_map = search(map_regex, line).group(1)
            elif search(result_regex, line):
                if search(result_regex, line).group(1) == 'A':
                    a_wins += 1
                    a_win_maps.append(last_map)
                else:
                    b_wins += 1
                    b_win_maps.append(last_map)
            stdout.write('\r' + ' ' * len(output_msg))
            output_msg = output(robot_a, robot_b, a_wins, b_wins, max_maps - a_wins - b_wins)
            stdout.write('\r' + output_msg)
            stdout.flush()

    print ''
    print '%s won on %s' % (robot_a, sorted(a_win_maps))
    print '%s won on %s' % (robot_b, sorted(b_win_maps))

