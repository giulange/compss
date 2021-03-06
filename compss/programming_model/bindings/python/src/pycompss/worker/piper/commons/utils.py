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
PyCOMPSs Common piper utils
===========================
    This file contains the common pipers methods.
"""

import os
import logging
from pycompss.util.logger.helpers import init_logging_worker
from pycompss.worker.piper.commons.constants import HEADER
from pycompss.worker.piper.commons.executor import Pipe


class PiperWorkerConfiguration(object):
    """
    Description of the configuration parameters for the Piper Worker
    """

    def __init__(self):
        """
        Constructs an empty configuration description for the piper worker
        """
        self.debug = False
        self.tracing = False
        self.storage_conf = None
        self.stream_backend = None
        self.stream_master_name = None
        self.stream_master_port = None
        self.tasks_x_node = 0
        self.pipes = []
        self.control_pipe = None

    def update_params(self, argv):
        """
        Constructs a configuration description for the piper worker using the
        arguments

        :param argv: arguments from the command line
        """
        self.debug = argv[1] == 'true'
        self.tracing = argv[2] == '1'
        self.storage_conf = argv[3]
        self.stream_backend = argv[4]
        self.stream_master_name = argv[5]
        self.stream_master_port = argv[6]
        self.tasks_x_node = int(argv[7])
        in_pipes = argv[8:8 + self.tasks_x_node]
        out_pipes = argv[8 + self.tasks_x_node:-2]
        if self.debug:
            assert self.tasks_x_node == len(in_pipes)
            assert self.tasks_x_node == len(out_pipes)
        self.pipes = []
        for i in range(0, self.tasks_x_node):
            self.pipes.append(Pipe(in_pipes[i], out_pipes[i]))
        self.control_pipe = Pipe(argv[-2], argv[-1])

    def print_on_logger(self, logger):
        """
        Prints the configuration through the logger

        :param logger: logger to output the configuration
        """
        logger.debug(HEADER + "-----------------------------")
        logger.debug(HEADER + "Persistent worker parameters:")
        logger.debug(HEADER + "-----------------------------")
        logger.debug(HEADER + "Debug          : " + str(self.debug))
        logger.debug(HEADER + "Tracing        : " + str(self.tracing))
        logger.debug(HEADER + "Tasks per node : " + str(self.tasks_x_node))
        logger.debug(HEADER + "Pipe Pairs     : ")
        for pipe in self.pipes:
            logger.debug(HEADER + "                 * " + str(pipe))
        logger.debug(HEADER + "Storage conf.  : " + str(self.storage_conf))
        logger.debug(HEADER + "-----------------------------")


def load_loggers(debug, persistent_storage, tracing):
    """
    Loads all the loggers

    :param debug: is Debug enabled
    :param persistent_storage: is persistent storage enabled
    :param tracing: if tracing is enabled
    :return logger: main logger of the application
    :return storage_loggers: loggers for the persistent data engine
    """
    # Load log level configuration file
    worker_path = os.path.dirname(os.path.realpath(__file__))
    if debug:
        # Debug
        init_logging_worker(worker_path +
                            '/../../../../log/logging_worker_debug.json',
                            tracing)
    else:
        # Default
        init_logging_worker(worker_path +
                            '/../../../../log/logging_worker_off.json',
                            tracing)

    # Define logger facilities
    logger = logging.getLogger('pycompss.worker.piper.piper_worker')
    storage_loggers = []
    if persistent_storage:
        storage_loggers.append(logging.getLogger('dataclay'))
        storage_loggers.append(logging.getLogger('hecuba'))
        storage_loggers.append(logging.getLogger('redis'))
        storage_loggers.append(logging.getLogger('storage'))
    return logger, storage_loggers
