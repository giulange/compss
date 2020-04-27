#!/usr/bin/python
#
#  Copyright 2002-2019 Barcelona Supercomputing Center (www.bsc.es)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# -*- coding: utf-8 -*-

"""
PyCOMPSs Persistent Worker Pipe Executor
========================================
    This file contains the code of an executor running the commands that it
    reads from a pipe.
"""

import copy
import signal
import logging
import os
import sys
import time
import thread_affinity

from pycompss.worker.piper.commons.constants import EXECUTE_TASK_TAG
from pycompss.worker.piper.commons.constants import END_TASK_TAG
from pycompss.worker.piper.commons.constants import COMPSS_EXCEPTION_TAG
from pycompss.worker.piper.commons.constants import PING_TAG
from pycompss.worker.piper.commons.constants import PONG_TAG
from pycompss.worker.piper.commons.constants import QUIT_TAG
from pycompss.worker.commons.executor import build_return_params_message

from pycompss.streams.components.distro_stream_client import DistroStreamClientHandler  # noqa: E501

HEADER = "*[PYTHON EXECUTOR] "


def shutdown_handler(signal, frame):
    """
    Shutdown handler (do not remove the parameters).

    :param signal: shutdown signal
    :param frame: Frame
    :return: None
    """
    raise Exception("Received SIGTERM")


class Pipe(object):
    """
    Bi-directional communication channel
    """

    def __init__(self, input_pipe, output_pipe):
        """
        Constructs a new Pipe

        :param input_pipe: Input pipe for the thread. To receive messages from
                           the runtime.
        :param output_pipe: Output pipe for the thread. To send messages to
                            the runtime.
        """
        self.input_pipe = input_pipe
        self.input_pipe_open = None
        self.output_pipe = output_pipe

    def read_command(self, retry_period=0):
        """
        Returns the first command on the pipe

        :param retry_period: time (ms) that the thread sleeps if EOF is read
                             from pipe
        :return: the first command available on the pipe
        """
        if self.input_pipe_open is None:
            self.input_pipe_open = open(self.input_pipe, 'r')
            # Non blocking open:
            # fd = os.open(self.input_pipe, os.O_RDWR)
            # self.input_pipe_open = os.fdopen(fd, 'r')

        line = self.input_pipe_open.readline()
        if line == "":
            time.sleep(0.001 * retry_period)
            line = self.input_pipe_open.readline()

        return line

    def write(self, message):
        """
        Writes a message through the pipe

        :param message: message sent through the pipe
        """
        with open(self.output_pipe, 'w') as out_pipe:
            out_pipe.write(message + "\n")

    def close(self):
        """
        Closes the pipe, if open
        """
        if self.input_pipe_open:
            self.input_pipe_open.close()
            self.input_pipe_open = None

    def __str__(self):
        return "PIPE IN " + self.input_pipe + " OUT " + self.output_pipe


class ExecutorConf(object):
    """
    Executor configuration
    """

    def __init__(self, tracing, storage_conf, logger, storage_loggers,
                 stream_backend, stream_master_ip, stream_master_port):
        """
        Constructs a new executor configuration.

        :param tracing: Enable tracing for the executor.
        :param storage_conf: Storage configuration file.
        :param logger: Main logger.
        :param storage_loggers: List of supported storage loggers
                                (empty if running w/o storage).
        :param stream_backend: Streaming backend type.
        :param stream_master_ip: Streaming master IP.
        :param stream_master_port: Streaming master port.
        """
        self.tracing = tracing
        self.storage_conf = storage_conf
        self.logger = logger
        self.storage_loggers = storage_loggers
        self.stream_backend = stream_backend
        self.stream_master_ip = stream_master_ip
        self.stream_master_port = stream_master_port


######################
#  Processes body
######################

def executor(queue, process_name, pipe, conf):
    """
    Thread main body - Overrides Threading run method.
    Iterates over the input pipe in order to receive tasks (with their
    parameters) and process them.
    Notifies the runtime when each task  has finished with the
    corresponding output value.
    Finishes when the "quit" message is received.

    :param queue: Queue where to put exception messages
    :param process_name: Process name (Thread-X, where X is the thread id).
    :param pipe: Pipe to receive and send messages from/to the runtime.
    :param conf: configuration of the executor
    :return: None
    """
    logger = conf.logger
    try:
        # Replace Python Worker's SIGTERM handler.
        signal.signal(signal.SIGTERM, shutdown_handler)

        tracing = conf.tracing
        storage_conf = conf.storage_conf
        storage_loggers = conf.storage_loggers

        # Get a copy of the necessary information from the logger to
        # re-establish after each task
        logger_handlers = copy.copy(logger.handlers)
        logger_level = logger.getEffectiveLevel()
        logger_formatter = logging.Formatter(logger_handlers[0].formatter._fmt)
        storage_loggers_handlers = []
        for storage_logger in storage_loggers:
            storage_loggers_handlers.append(copy.copy(storage_logger.handlers))

        if storage_conf != 'null':
            try:
                from storage.api import initWorkerPostFork
                initWorkerPostFork()
            except ImportError:
                if __debug__:
                    logger.info(HEADER + "[%s] Could not find initWorkerPostFork storage call. Ignoring it." %  # noqa: E501
                                str(process_name))

        # Start the streaming backend if necessary
        streaming = False
        if conf.stream_backend not in [None, 'null', 'NONE']:
            streaming = True

        if streaming:
            # Initialize streaming
            logger.debug(HEADER + "Starting streaming for process " +
                         str(process_name))
            try:
                DistroStreamClientHandler.init_and_start(
                    master_ip=conf.stream_master_ip,
                    master_port=int(conf.stream_master_port))
            except Exception as e:
                logger.error(e)
                raise e

        # Process properties
        alive = True

        if __debug__:
            logger.debug(HEADER + "[%s] Starting process" %
                         str(process_name))

        # MAIN EXECUTOR LOOP
        while alive:
            # Runtime -> pipe - Read command from pipe
            command = pipe.read_command(retry_period=0.5)
            if command != "":
                logger.debug(HEADER + "Received %s" % command)
                # Process the command
                alive = process_task(command,
                                     process_name,
                                     pipe,
                                     queue,
                                     tracing,
                                     logger,
                                     logger_handlers,
                                     logger_level,
                                     logger_formatter,
                                     storage_conf,
                                     storage_loggers,
                                     storage_loggers_handlers)

        # Stop storage
        if storage_conf != 'null':
            try:
                from storage.api import finishWorkerPostFork
                finishWorkerPostFork()
            except ImportError:
                if __debug__:
                    logger.info(
                        HEADER + "[%s] Could not find finishWorkerPostFork storage call. Ignoring it." %  # noqa: E501
                        str(process_name))

        # Stop streaming
        if streaming:
            logger.debug(HEADER + "Stopping streaming for process " +
                         str(process_name))
            DistroStreamClientHandler.set_stop()

        sys.stdout.flush()
        sys.stderr.flush()
        if __debug__:
            logger.debug(HEADER + "[%s] Exiting process " %
                         str(process_name))
        pipe.write(QUIT_TAG)
        pipe.close()
    except BaseException as e:
        logger.error(e)
        raise e


def process_task(current_line, process_name, pipe, queue, tracing,
                 logger, logger_handlers, logger_level, logger_formatter,
                 storage_conf, storage_loggers, storage_loggers_handlers):
    """
    Process command received from the runtime through a pipe.

    :param current_line: Current command (line) to process
    :param process_name: Process name for logger messages
    :param pipe: Pipe where to write the result
    :param queue: Queue where to drop the process exceptions
    :param tracing: Tracing
    :param logger: Logger
    :param logger_handlers: Logger handlers
    :param logger_level: Logger level
    :param logger_formatter: Logger formatter
    :param storage_conf: Storage configuration
    :param storage_loggers: Storage loggers
    :param storage_loggers_handlers: Storage loggers handlers
    :return: <Boolean> True if processed successfully, False otherwise.
    """
    stdout = sys.stdout
    stderr = sys.stderr

    affinity_ok = True

    if __debug__:
        logger.debug(HEADER + "[%s] Received message: %s" %
                     (str(process_name), str(current_line)))
    current_line = current_line.split()
    if current_line[0] == EXECUTE_TASK_TAG:
        # CPU binding
        cpus = current_line[-3]
        if cpus != "-":
            affinity_ok = bind_cpus(cpus, process_name, logger)

        # GPU binding
        gpus = current_line[-2]
        if gpus != "-":
            bind_gpus(gpus, process_name, logger)

        # Remove the last elements: cpu and gpu bindings
        current_line = current_line[0:-3]

        # task jobId command
        job_id = current_line[1]
        job_out = current_line[2]
        job_err = current_line[3]
        # current_line[4] = <boolean> = tracing
        # current_line[5] = <integer> = task id
        # current_line[6] = <boolean> = debug
        # current_line[7] = <string>  = storage conf.
        # current_line[8] = <string>  = operation type (e.g. METHOD)
        # current_line[9] = <string>  = module
        # current_line[10]= <string>  = method
        # current_line[11]= <string>  = time out
        # current_line[12]= <integer> = Number of slaves (worker nodes)==#nodes
        # <<list of slave nodes>>
        # current_line[12 + #nodes] = <integer> = computing units
        # current_line[13 + #nodes] = <boolean> = has target
        # current_line[14 + #nodes] = <string>  = has return (always 'null')
        # current_line[15 + #nodes] = <integer> = Number of parameters
        # <<list of parameters>>
        #       !---> type, stream, prefix , value

        if __debug__:
            logger.debug(HEADER + "[%s] Received task with id: %s" %
                         (str(process_name), str(job_id)))
            logger.debug(HEADER + "[%s] - TASK CMD: %s" %
                         (str(process_name), str(current_line)))

        # Swap logger from stream handler to file handler
        # All task output will be redirected to job.out/err
        for log_handler in logger_handlers:
            logger.removeHandler(log_handler)
        for storage_logger in storage_loggers:
            for log_handler in storage_logger.handlers:
                storage_logger.removeHandler(log_handler)
        out_file_handler = logging.FileHandler(job_out)
        out_file_handler.setLevel(logger_level)
        out_file_handler.setFormatter(logger_formatter)
        err_file_handler = logging.FileHandler(job_err)
        err_file_handler.setLevel("ERROR")
        err_file_handler.setFormatter(logger_formatter)
        logger.addHandler(out_file_handler)
        logger.addHandler(err_file_handler)
        for storage_logger in storage_loggers:
            storage_logger.addHandler(out_file_handler)
            storage_logger.addHandler(err_file_handler)

        if __debug__:
            logger.debug("Received task in process: %s" %
                         str(process_name))
            logger.debug(" - TASK CMD: %s" %
                         str(current_line))

        try:
            # Setup out/err wrappers
            out = open(job_out, 'a')
            err = open(job_err, 'a')
            sys.stdout = out
            sys.stderr = err

            # Check thread affinity
            if not affinity_ok:
                err.write("WARNING: This task is going to be executed with default thread affinity %s" %  # noqa: E501
                          thread_affinity.getaffinity())

            # Setup process environment
            cn = int(current_line[12])
            cn_names = ','.join(current_line[13:13 + cn])
            cu = current_line[13 + cn]
            os.environ["COMPSS_NUM_NODES"] = str(cn)
            os.environ["COMPSS_HOSTNAMES"] = cn_names
            os.environ["COMPSS_NUM_THREADS"] = cu
            os.environ["OMP_NUM_THREADS"] = cu
            if __debug__:
                logger.debug("Process environment:")
                logger.debug("\t - Number of nodes: %s" % (str(cn)))
                logger.debug("\t - Hostnames: %s" % str(cn_names))
                logger.debug("\t - Number of threads: %s" % (str(cu)))

            # Execute task
            from pycompss.worker.commons.worker import execute_task
            result = execute_task(process_name,
                                  storage_conf,
                                  current_line[9:],
                                  tracing,
                                  logger)
            exit_value = result[0]
            new_types = result[1]
            new_values = result[2]
            timed_out = result[3]
            except_msg = result[4]

            # Restore out/err wrappers
            sys.stdout = stdout
            sys.stderr = stderr
            sys.stdout.flush()
            sys.stderr.flush()
            out.close()
            err.close()

            if exit_value == 0:
                # Task has finished without exceptions
                # endTask jobId exitValue message
                params = build_return_params_message(new_types, new_values)
                message = END_TASK_TAG + " " + str(job_id)
                message += " " + str(exit_value) + " " + str(params) + "\n"
                if __debug__:
                    logger.debug(
                        "%s - Pipe %s END TASK MESSAGE: %s" %
                        (str(process_name),
                         str(pipe.output_pipe),
                         str(message)))
            elif exit_value == 2:
                # Task has finished with a COMPSs Exception
                # compssExceptionTask jobId exitValue message

                except_msg = except_msg.replace(" ", "_")
                message = COMPSS_EXCEPTION_TAG + " " + str(job_id)
                message += " " + str(except_msg) + "\n"
                if __debug__:
                    logger.debug(
                        "%s - Pipe %s COMPSS EXCEPTION TASK MESSAGE: %s" %
                        (str(process_name),
                         str(pipe.output_pipe),
                         str(except_msg)))
            else:
                # An exception other than COMPSsException has been raised
                # within the task
                message = END_TASK_TAG + " " + str(job_id)
                message += " " + str(exit_value) + "\n"

                if __debug__:
                    logger.debug("%s - Pipe %s END TASK MESSAGE: %s" %
                                 (str(process_name),
                                  str(pipe.output_pipe),
                                  str(message)))

            # The return message is:
            #
            # TaskResult ==> jobId exitValue D List<Object>
            #
            # Where List<Object> has D * 2 length:
            # D = #parameters == #task_parameters +
            #                    (has_target ? 1 : 0) +
            #                    #returns
            # And contains a pair of elements per parameter:
            #     - Parameter new type.
            #     - Parameter new value:
            #         - 'null' if it is NOT a PSCO
            #         - PSCOId (String) if is a PSCO
            # Example:
            #     4 null 9 null 12 <pscoid>
            #
            # The order of the elements is: parameters + self + returns
            #
            # This is sent through the pipe with the END_TASK message.
            # If the task had an object or file as parameter and the worker
            # returns the id, the runtime can change the type (and locations)
            # to a EXTERNAL_OBJ_T.
            pipe.write(message)

        except Exception as e:
            logger.exception("%s - Exception %s" % (str(process_name),
                                                    str(e)))
            if queue:
                queue.put("EXCEPTION")

        # Clean environment variables
        if __debug__:
            logger.debug("Cleaning environment.")
        if cpus != "-":
            del os.environ['COMPSS_BINDED_CPUS']
        if gpus != "-":
            del os.environ['COMPSS_BINDED_GPUS']
            del os.environ['CUDA_VISIBLE_DEVICES']
            del os.environ['GPU_DEVICE_ORDINAL']
        del os.environ['COMPSS_HOSTNAMES']

        # Restore loggers
        if __debug__:
            logger.debug("Restoring loggers.")
        logger.removeHandler(out_file_handler)
        logger.removeHandler(err_file_handler)
        for handler in logger_handlers:
            logger.addHandler(handler)
        i = 0
        for storage_logger in storage_loggers:
            storage_logger.removeHandler(out_file_handler)
            storage_logger.removeHandler(err_file_handler)
            for handler in storage_loggers_handlers[i]:
                storage_logger.addHandler(handler)
            i += 1
        if __debug__:
            logger.debug(HEADER + "[%s] Finished task with id: %s" %
                         (str(process_name), str(job_id)))

    elif current_line[0] == PING_TAG:
        pipe.write(PONG_TAG)

    elif current_line[0] == QUIT_TAG:
        # Received quit message -> Suicide
        if __debug__:
            logger.debug(HEADER + "[%s] Received quit." %
                         str(process_name))
        return False

    else:
        if __debug__:
            logger.debug(HEADER + "[%s] Unexpected message: %s" %
                         (str(process_name), str(current_line)))
        raise Exception("Unexpected message: %s" % str(current_line))
    return True


def bind_cpus(cpus, process_name, logger):
    """
    Bind the given CPUs for core affinity to this process.

    :param cpus: Target CPUs.
    :param process_name: Process name for logger messages
    :param logger: Logger
    :return: <Boolean> True if success, False otherwise
    """
    os.environ['COMPSS_BINDED_CPUS'] = cpus
    if __debug__:
        logger.debug(HEADER + "[%s] Assigning affinity %s" %
                     (str(process_name), str(cpus)))
    cpus = list(map(int, cpus.split(",")))
    try:
        thread_affinity.setaffinity(cpus)
    except Exception:
        if __debug__:
            logger.error(
                HEADER + "[%s] WARNING: could not assign affinity %s" %
                (str(process_name), str(cpus)))
        return False
    return True


def bind_gpus(gpus, process_name, logger):
    """
    Bind the given GPUs to this process.

    :param gpus: Target GPUs.
    :param process_name: Process name for logger messages
    :param logger: Logger
    :return: None
    """
    os.environ['COMPSS_BINDED_GPUS'] = gpus
    os.environ['CUDA_VISIBLE_DEVICES'] = gpus
    os.environ['GPU_DEVICE_ORDINAL'] = gpus
    if __debug__:
        logger.debug(HEADER + "[%s] Assigning GPU %s" %
                     (str(process_name), str(gpus)))
