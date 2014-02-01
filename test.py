#! /usr/bin/python

from Queue import Empty
from Queue import Queue
from curses import echo
from curses import endwin
from curses import initscr
from curses import nocbreak
from curses import noecho
from re import search
from subprocess import PIPE
from subprocess import Popen
from sys import argv
from threading import Thread


def enqueue_output(out, queue):
    for line in iter(out.readline, b''):
        queue.put(line)
    out.close()


def output(robot_a, robot_b, a_win_maps, b_win_maps, left):
    return ('%s won on %d maps (%s); %s won on %d maps (%s).  %d more maps to simulate'
            % (robot_a, len(a_win_maps), str(a_win_maps), robot_b, len(b_win_maps), str(b_win_maps), left))


if __name__ == '__main__':
    stdscr = initscr()
    noecho()

    robot_a = argv[1]
    robot_b = argv[2]

    max_maps = 30
    a_win_maps, b_win_maps = [], []

    map_regex = r'%s vs. %s on (\w+)' % (robot_a, robot_b)
    result_regex = r'\((A|B)\) wins'

    p = Popen('seq 0 51 | parallel -j 8 ./test_helper.py %s %s' % (robot_a, robot_b), shell=True, stdout=PIPE)
    q = Queue()
    t = Thread(target=enqueue_output, args=(p.stdout, q))
    t.daemon = True
    t.start()

    output_msg = output(robot_a, robot_b, a_win_maps, b_win_maps, max_maps - len(a_win_maps) - len(b_win_maps))
    stdscr.addstr(0, 0, output_msg)
    stdscr.refresh()

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
                    a_win_maps.append(last_map)
                else:
                    b_win_maps.append(last_map)
            output_msg = output(robot_a, robot_b, a_win_maps, b_win_maps, max_maps - len(a_win_maps) - len(b_win_maps))
            stdscr.addstr(0, 0, output_msg)
            stdscr.refresh()

    nocbreak()
    echo()
    endwin()

    print output_msg

