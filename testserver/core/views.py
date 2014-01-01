from django.contrib.auth.decorators import login_required
from django.http import HttpResponse
from django.shortcuts import render

from testserver.core.models import Robot
from testserver.core.models import RobotLine
from testserver.core.models import SimulationFile
from testserver.core.models import SimulationSet


def bye(request):

    context = {}
    return render(request, 'bye.html', context)


@login_required
def home(request):

    lines = RobotLine.objects.all()

    line_dict = {}
    for robot in Robot.objects.filter(line__alive=True):
        line_dict[robot.line] = line_dict.get(robot.line, []) + [robot]
    length = max(map(len, line_dict.itervalues())) if line_dict else 0

    robots = [[None] * len(line_dict) for _ in range(length)]
    for idx, (line, robot_list) in enumerate(line_dict.iteritems()):
        for idx2 in range(length):
            robots[idx2][idx] = robot_list[idx2] if idx2 < len(robot_list) else None

    context = {'robot_lines': lines, 'robots': robots}
    return render(request, 'home.html', context)


@login_required
def line(request, line_id):
    line = RobotLine.objects.get(pk=line_id)
    robots = Robot.objects.filter(line=line).order_by('line_num')
    simulation_sets = [SimulationSet.objects.get_or_create(robot_a=robot) for robot in robots]

    context = {
        'line': line,
        'robot_simulations': zip(robots, simulation_sets),
    }

    return render(request, 'line.html', context)


@login_required
def robot(request, robot_id):
    this_robot = Robot.objects.get(pk=robot_id)

    simulation_sets = [SimulationSet.objects.get_or_create(robot_a=this_robot)]
    for opponent in this_robot.simulated_opponents.all():
        if this_robot.pk > opponent.pk:
            simulation_sets.append(SimulationSet.objects.get_or_create(robot_a=this_robot, robot_b=opponent))
        elif this_robot.pk < opponent.pk:
            simulation_sets.append(SimulationSet.objects.get_or_create(robot_a=opponent, robot_b=this_robot))


    context = {'robot': this_robot, 'simulation_sets': simulation_sets}
    return render(request, 'robot.html', context)


@login_required
def simulation_file(request, simulationfile_id):

    sim_file = SimulationFile.objects.get(pk=simulationfile_id)

    response = HttpResponse(content_type='application/octet-stream')
    response['Content-Disposition'] = 'attachment; filename="%s"' % (sim_file, )

    response.write(sim_file.data)
    return response

