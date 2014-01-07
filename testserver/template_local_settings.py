def update_settings(settings):

    settings['DEBUG'] = True
    settings['DATABASES'] = {
        'default': {
            'ENGINE': 'django.db.backends.postgresql_psycopg2',
            'NAME': 'testserver',
            'USER': 'postgres',
            'PASSWORD': 'diametrically-substantial-glancing-goods',
            'HOST': 'jinpan.mit.edu',
            'PORT': '5432',
        }
    }
    settings['BATTLECODE_ROOT'] = '/home/jin/Battlecode2014/'

