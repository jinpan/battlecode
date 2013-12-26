from django.contrib import admin
from django.forms import ModelForm

from testserver.core.models import File
from testserver.core.models import Map
from testserver.core.models import RobotLine
from testserver.core.models import Robot
from testserver.core.models import Simulation
from testserver.core.models import SimulationSet


class FileInlineForm(ModelForm):
    class Meta:
        model = File


class FileInline(admin.TabularInline):
    model = File
    extra = 1
    form = FileInlineForm


class RobotAdmin(admin.ModelAdmin):
    exclude = ('line_num', 'simulated_opponents', )

    inlines = (FileInline, )


class SimulationAdmin(admin.ModelAdmin):
    list_display = ('robot_a', 'robot_b', 'map_file',
                    'priority', 'status', 'winner', 'rounds')

    list_filter = ('status', 'winner', 'robot_a', 'robot_b',
                   'map_file')

    def get_queryset(self, request):
        return super(SimulationAdmin, self).get_queryset(request).prefetch_related('robot_a', 'robot_b', 'map_file')


class SimulationSetAdmin(admin.ModelAdmin):

    filter_horizontal = ('robot_a_win_maps', 'robot_a_lose_maps', 'robot_a_tie_maps')


admin.site.register(File)
admin.site.register(Map)
admin.site.register(Robot, RobotAdmin)
admin.site.register(RobotLine)
admin.site.register(Simulation, SimulationAdmin)
admin.site.register(SimulationSet, SimulationSetAdmin)

