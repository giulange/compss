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
PyCOMPSs Utils - arguments
==========================
    This file contains the common methods to do any argument check
    or management.
"""

from __future__ import print_function
import sys


def warn_if_unexpected_argument(supported_arguments, arguments, where):
    """
    This method looks for unexpected arguments and displays a warning
    if found.
    :param supported_arguments: Set of supported arguments
    :param arguments: List of arguments to check
    :param where: Location of the argument
    :return: None
    """
    for argument in arguments:
        if argument not in supported_arguments:
            message = "WARNING: Unexpected argument: " + str(argument) + \
                      " Found in " + str(where)
            print(message)                   # show the warn through stdout
            print(message, file=sys.stderr)  # also show the warn through stderr
