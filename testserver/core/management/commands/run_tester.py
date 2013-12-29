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
        while True:
            simulation_pk, result, winner, tie, rounds, error, sim_data = self.results.get()
            sim = Simulation.objects.get(pk=simulation_pk)
            sim.save_result(result, winner, tie, rounds, error, sim_data)


class SpawnerThread(threading.Thread):
    
    def __init__(self, workers_semaphore, results, sleep_interval=DEFAULT_SLEEP_INTERVAL):
        super(SpawnerThread, self).__init__()
        self.workers_semaphore = workers_semaphore
        self.results = results
        self.sleep_interval = sleep_interval

    
    def run(self):
        while True:
            self.workers_semaphore.acquire()
            while True:
                simulation = Simulation.objects.get_next_simulation()
                if simulation:
                    break
                else:
                    sleep(sleep_interval)

            args = (simulation.pk, simulation.map_file.name,
                    simulation.get_files(simulation.robot_a), simulation.get_files(simulation.robot_b),
                    self.results, self.workers_semaphore)

            thread = WorkerThread(*args)
            thread.start()


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
        result, sim_data, error = run_simulation(self.simulation_pk, self.map_file, self.robot_a_files, self.robot_b_files)
        winner, tie, rounds = parse_result(result, sim_data)
        self.results.put((self.simulation_pk, result, winner, tie, rounds, error, sim_data))

        self.workers_semaphore.release()


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
        workers_semaphore = Semaphore(num_processes)

        results = Queue()

        saver = SaverThread(results)
        spawner = SpawnerThread(workers_semaphore, results)

        saver.start()
        spawner.start()

        try:
            spawner.join()
        except KeyboardInterrupt:
            # shut down threads
            pass

