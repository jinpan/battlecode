from django.contrib import admin
from django.forms import ModelForm

from testserver.core import constants
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
    exclude = ('line_num', 'name', 'simulated_opponents', )

    inlines = (FileInline, )


class SimulationAdmin(admin.ModelAdmin):
    list_display = ('pk', 'robot_a', 'robot_b', 'map_file',
                    'priority', 'status', 'winner', 'rounds')

    list_filter = ('status', 'winner', 'robot_a', 'robot_b',
                   'map_file')

    actions = ('set_open', )

    def get_queryset(self, request):
        return super(SimulationAdmin, self).get_queryset(request).prefetch_related('robot_a', 'robot_b', 'map_file')


    def set_open(modeladmin, request, queryset):
        queryset.update(status=constants.STATUS.OPEN)
    set_open.short_description = 'Mark the selected simulations as open'


class SimulationSetAdmin(admin.ModelAdmin):

    prefetch = (
        'robot_a_win_simulations',
            'robot_a_win_simulations__robot_a', 'robot_a_win_simulations__robot_a__line',
            'robot_a_win_simulations__robot_b', 'robot_a_win_simulations__robot_b__line',
            'robot_a_win_simulations__map_file',
        'robot_a_lose_simulations', 'robot_a_lose_simulations__robot_a', 'robot_a_lose_simulations__robot_b', 'robot_a_lose_simulations__map_file',
        'robot_a_tie_simulations', 'robot_a_tie_simulations__robot_a', 'robot_a_tie_simulations__robot_b', 'robot_a_tie_simulations__map_file',
    )

    filter_horizontal = ('robot_a_win_simulations', 'robot_a_lose_simulations', 'robot_a_tie_simulations')

    def get_queryset(self, request):
        return super(SimulationSetAdmin, self).get_queryset(request).prefetch_related(*self.prefetch)


admin.site.register(File)
admin.site.register(Map)
admin.site.register(Robot, RobotAdmin)
admin.site.register(RobotLine)
admin.site.register(Simulation, SimulationAdmin)
admin.site.register(SimulationSet, SimulationSetAdmin)

