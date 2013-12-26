from djcelery import celery

from testserver.core.utils import run_simulation


@celery.task
def run_simulation_celery(simulation):
    return run_simulation(simulation)

