from multiprocessing import Process
from multiprocessing import Semaphore
from multiprocessing import cpu_count

from django.core.management.base import BaseCommand

from testserver.core.models import Simulation
from testserver.core.constants import sleep_interval


def spawner(semaphore, sleep_interval=sleep_interval):
    while True:
        semaphore.acquire()

        process = Process(target=worker, args=(semaphore, sleep_interval))
        process.start()


def worker(semaphore, sleep_interval=sleep_interval, *args, **kwargs):
    Simulation.objects.worker(sleep_interval=sleep_interval)
    semaphore.release()


class Command(BaseCommand):
    '''
    Options:
        cores=xx
    '''

    def handle(self, *args, **kwargs):

        data = {}
        for arg in args:
            key, val = arg.split('=')
            data[key] = val

        num_processes = int(data.get('cores', cpu_count() - 1))
        workers = Semaphore(num_processes)

        spawner(workers)

