# Battlecode Repository for INVARIABLY-NECESSARY-STRADDLING-NETWORKS (007)
==========

## Accessing the test server
Currently, the testserver can be reached at http://jinpan.mit.edu:8000.  Later, the testserver will likely have a more pernament url, such as http://battlecode.jin-pan.com.

It's in our best interest to keep this site and our source code private.  We all have usernames equal to our first names and passwords equal to our username reversed with a `007` tacked onto the end for good measure.  For instance, Jin's account would have username `jin` and password `nij007`.  These passwords can and should be changed in the admin interface (go to `/admin`).

## Adding a bot to test
If you wish to add a new robot, hit `Add a new robot line` from the home page and you will be brought to a robot creation page.  Hit the little green plus to the right of `Line` and enter in a unique line name.  Paste in your java files and hit save.  If the bot you wish to add is the natural extension of an already existing robot lineage, simply select an existing line instead of creating a new one.

## Viewing robot stats
On the home page, click on the robot you wish to see detailed information about.  You will then be presented with a page of simulation information.  Each map file is a link to a results file that you can download and play back in the battlecode simulator.  Observe which team is blue/red when viewing.

## Adding moar test nodes
You may have noticed that after adding a new robot, results take some time to populate.  This is due to a pretty slow server, but fear not, the test codebase was designed to be distributed!  To use a machine as a worker node, simply follow these steps:

1. clone this repo (`git clone git@github.com:jinpan/battlecode.git`)
2. cd to the testserver (`cd battlecode/testserver`)
2. create a virtualenv (`virtualenv ENV --prompt='battlecode'`) (might need to do `sudo pip install virtualenv` and `sudo apt-get install python-pip`)
3. edit your python path (`open ENV/bin/activate`; (or `xdg-open ENV/bin/activate)` insert `export PYTHONPATH=$PYTHONPATH:/home/jin/Projects/battlecode` at around line 50ish (replace `/home/jin/Projects/battlecode` with where you cloned the repo))
4. source the virtualenv (`source ENV/bin/activate`)
5. install the requirements (`pip install -r requirements.txt`).  If postgres_psycopg2 won't install, you likely are missing the python-dev and libpq-dev packages (`sudo apt-get install python-dev libpq-dev`)
6. copy `template_local_settings.py` to `local_settings.py` and update `settings['BATTLECODE_ROOT']` to where your battlecode installation is.  Keep the database settings intact.
7. hop on the MIT network (http://ist.mit.edu/vpn)
8. run the tester! (`./manage.py run_tester`).  Optionally, you can specify the number of cores to allocate (`./manage.py run_tester cores=4`); it defaults to one less than the number of cores on the host.  Also optionally, you can specify whether or not to retry failed simulations (retry=true); it defaults to false.

One thing to note - the tester effectively consumes zero cpu cycles if there are no jobs to process; you can leave it on in the background and it will not thrash the host machine.  The worker was designed with special care to avoid memory leaks so it should be okay to leave this process on in the background.

## General navigation
I will refer to really plain HTML as our site, and the somewhat less barebone HTML (with the blue bar at the top) as the Django admin.

Clicking on the (007) link in the upper left hand corner will bring you to the home page of our site.

In our site, clicking on your name in the upper left hand corner will bring you to the admin.

The Django admin has pretty intuitive navigation and is a way to indirectly edit the database.

## Remarks
If simulations start taking a really long time to compute, time to start marking old robots as inactive in the django admin!

## Contributors
* Deniz Oktay
* Jin Pan
* Vickie Ye
* Kevin Zhou
