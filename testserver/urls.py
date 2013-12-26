from django.conf.urls import patterns, include, url

from django.contrib import admin

from testserver.core.urls import urlpatterns as core_patterns


admin.autodiscover()
urlpatterns = patterns('',
    url(r'^admin/', include(admin.site.urls)),
)

urlpatterns += core_patterns

