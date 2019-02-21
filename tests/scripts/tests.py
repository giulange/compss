#!/usr/bin/python

# -*- coding: utf-8 -*-

# For better print formatting
from __future__ import print_function

# Imports
import time

from arguments import ArgumentError
from arguments import get_args
from configuration import ConfigurationError
from configuration import load_configuration_file
from compilation_and_deployment import TestCompilationError
from compilation_and_deployment import TestDeploymentError
from compilation_and_deployment import compile_and_deploy_tests
from execution import TestExecutionError
from execution import execute_tests


def launch_tests():
    """
    Parses the cmd_args, loads the configuration file, and compiles, deploys, and executes the tests
    :return: Exit value indicating whether the tests have failed or not
        + int
    :raise ArgumentError: Error parsing command line arguments
    :raise ConfigurationError: If the provided file path is invalid or there is an error when loading the cfg content
    :raise TestCompilationError: If any error is found during compilation
    :raise TestDeploymentError: If any error is found during deployment
    :raise TestExecutionError: If any error is found during execution (because of infrastructure, not test failure)
    :exit 0: This method exits when test numbering is provided
    """
    # Process command line arguments
    cmd_args = get_args()

    # Load configuration file
    compss_cfg = load_configuration_file(cmd_args.cfg_file)

    # Compile and Deploy tests
    compile_and_deploy_tests(cmd_args, compss_cfg)

    # Execute tests
    return execute_tests(cmd_args, compss_cfg)


############################################
# MAIN FUNCTION
############################################

def main():
    """
    Main method to execute the tests workflow
    :return:
    """
    # Log start
    print("----------------------------------------")
    print("[INFO] Launching tests...")
    print("----------------------------------------")
    start_time = time.time()

    # Launch main function
    try:
        ev = launch_tests()
    except ArgumentError as ae:
        print("----------------------------------------")
        print("[ERROR] Parsing arguments")
        print(ae)
        print("----------------------------------------")
    except ConfigurationError as ce:
        print("----------------------------------------")
        print("[ERROR] Cannot load configuration file")
        print(ce)
        print("----------------------------------------")
    except TestCompilationError as tce:
        print("----------------------------------------")
        print("[ERROR] Cannot compile tests")
        print(tce)
        print("----------------------------------------")
    except TestDeploymentError as tde:
        print("----------------------------------------")
        print("[ERROR] Cannot deploy tests")
        print(tde)
        print("----------------------------------------")
    except TestExecutionError as tee:
        # WARN: This is received when there is an infrastructure issue executing the tests, not when the
        # tests fail themselves
        print("----------------------------------------")
        print("[ERROR] Cannot execute tests")
        print(tee)
        print("----------------------------------------")
        pass
    else:
        # All tests executed, they may have failed though
        end_time = time.time()
        elapsed_time = end_time - start_time
        print()
        print("----------------------------------------")
        print("[INFO] Tests finished")
        print("[INFO]    - Success = " + str(ev.name))
        print("[INFO]    - Elapsed time = " + str(elapsed_time))
        print("----------------------------------------")


############################################
# ENTRY POINT
############################################

if __name__ == "__main__":
    main()
