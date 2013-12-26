from django.conf.urls import patterns, include, url
from django.contrib.auth import views as auth_views


urlpatterns = patterns('core.views',
    url(r'^$', 'home', name='home'),

    url(r'^line/(?P<line_id>\d+)/$', 'line', name='line'),
    url(r'^robot/(?P<robot_id>\d+)/$', 'robot', name='robot'),

    url(r'^login/$', auth_views.login, name='login',
        kwargs={'template_name': 'login.html'}),
    url(r'^logout/$', auth_views.logout, name='logout',
        kwargs={'next_page': '/bye'}),
    url(r'^bye$', 'bye', name='bye'),
)

