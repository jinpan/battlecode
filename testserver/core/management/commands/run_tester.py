from Queue import Queue
from multiprocessing import cpu_count
from threading import Semaphore
from time import sleep
import threading

from django.core.management.base import BaseCommand

from testserver.core.models import Simulation
from testserver.core.constants import DEFAULT_SLEEP_INTERVAL
from testserver.core.utils import parse_result
from testserver.core.utils import run_simulation


class SaverThread(threading.Thread):

    def __init__(self, results):
        super(SaverThread, self).__init__()
        self.results = results

    
    def run(self):
        simulation_pk, result, winner, tie, rounds, error, sim_data = self.results.get()
        sim = Simulation.objects.get(pk=simulation_pk)
        try:
            sim.save_result(result, winner, tie, rounds, error, sim_data)
        except:
            sim.set_error()


class SaverThreadSpawner(threading.Thread):

    def __init__(self, savers_semaphore, results):
        super(SaverThreadSpawner, self).__init__()
        self.savers_semaphore = savers_semaphore
        self.results = results

    
    def run(self):
        while True:
            self.savers_semaphore.acquire()

            thread = SaverThread(self.results)
            thread.start()
            thread.join()

            self.savers_semaphore.release()


class WorkerThread(threading.Thread):

    def __init__(self, simulation_pk, map_file, robot_a_files, robot_b_files, results, workers_semaphore):
        super(WorkerThread, self).__init__()
        self.simulation_pk = simulation_pk
        self.map_file = map_file
        self.robot_a_files = robot_a_files
        self.robot_b_files = robot_b_files
        self.results = results
        self.workers_semaphore = workers_semaphore


    def run(self):
        try:
            result, sim_data, error = run_simulation(self.simulation_pk, self.map_file, self.robot_a_files, self.robot_b_files)
            winner, tie, rounds = parse_result(result, sim_data)
            self.results.put((self.simulation_pk, result, winner, tie, rounds, error, sim_data))
        except:
            run_simulation(self.simulation_pk, self.map_file, self.robot_a_files, self.robot_b_files, cleanup_only=True)
            sim = Simulation.objects.get(pk=self.simulation_pk)
            sim.set_error()

        self.workers_semaphore.release()


class WorkerThreadSpawner(threading.Thread):
    
    def __init__(self, workers_semaphore, results, retry=False, sleep_interval=DEFAULT_SLEEP_INTERVAL):
        super(WorkerThreadSpawner, self).__init__()
        self.workers_semaphore = workers_semaphore
        self.results = results
        self.retry = retry
        self.sleep_interval = sleep_interval

    
    def run(self):
        while True:
            self.workers_semaphore.acquire()
            while True:
                simulation = Simulation.objects.get_next_simulation(retry=self.retry)
                if simulation:
                    break
                else:
                    sleep(self.sleep_interval)

            args = (simulation.pk, simulation.map_file.name,
                    simulation.get_files(simulation.robot_a), simulation.get_files(simulation.robot_b),
                    self.results, self.workers_semaphore)

            thread = WorkerThread(*args)
            thread.start()


class Command(BaseCommand):
    '''
    Options:
        cores=xx
        retry=true/false
    '''

    def handle(self, *args, **kwargs):

        data = {}
        for arg in args:
            key, val = arg.split('=')
            data[key] = val

        num_processes = int(data.get('cores', cpu_count() - 1))
        retry = (data.get('retry', 'false').lower() == 'true')

        savers_semaphore = Semaphore(1)
        workers_semaphore = Semaphore(num_processes)
        results = Queue()

        saver_spawner = SaverThreadSpawner(savers_semaphore, results)
        if retry:
            worker_spawner = WorkerThreadSpawner(workers_semaphore, results, retry=True)
        else:
            worker_spawner = WorkerThreadSpawner(workers_semaphore, results)

        saver_spawner.start()
        worker_spawner.start()

        try:
            worker_spawner.join()
        except KeyboardInterrupt:
            # shut down threads
            raise Exception

