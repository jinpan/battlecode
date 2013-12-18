from django.contrib import admin

from testserver.core.models import File
from testserver.core.models import RobotLine
from testserver.core.models import Robot
from testserver.core.models import Simulation


class RobotAdmin(admin.ModelAdmin):
    exclude = ('line_num', )


admin.site.register(File)
admin.site.register(RobotLine)
admin.site.register(Robot, RobotAdmin)
admin.site.register(Simulation)

