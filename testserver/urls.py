from django.conf.urls import patterns, include, url
from django.contrib import admin
from django.contrib.auth.decorators import login_required

from testserver.core.urls import urlpatterns as core_patterns


admin.autodiscover()
admin.site.login = login_required(admin.site.login)

urlpatterns = patterns('',
    url(r'^admin/', include(admin.site.urls)),
)

urlpatterns += core_patterns

