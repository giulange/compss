"""
@author: etejedor
@author: fconejer

PyCOMPSs API - Parameter
========================
    This file contains the clases needed for the parameter definition.
    1. Direction.
        - IN
        - OUT
        - INOUT
    2. Type.
        - FILE
        - BOOLEAN
        - STRING
        - INT
        - LONG
        - FLOAT
        - OBJECT
        - PSCO
        - EXTERNAL_PSCO
    3. Stream.
        - STDIN
        - STDOUT
        - STDERR
        - UNSPECIFIED

    4. Parameter.
"""


# Numbers match both C and Java enums
class Direction:
    IN = 0
    OUT = 1
    INOUT = 2


# Numbers match both C and Java enums
class Type:
    BOOLEAN = 0
    CHAR = 1
    BYTE = 2
    SHORT = 3
    INT = 4
    LONG = 5
    FLOAT = 6		# C double
    DOUBLE = 7
    STRING = 8
    FILE = 9
    OBJECT = 10         # Unavailable
    PSCO = 11           # Unavailable
    EXTERNAL_PSCO = 12	# PSCO

# Numbers match both C and Java enums
class Stream:
    STDIN = 0
    STDOUT = 1
    STDERR = 2
    UNSPECIFIED = 3

class Parameter:
    """
    Parameter class
    Used to group the type, direction and value of a parameter
    """
    def __init__(self, p_type=None, p_direction=Direction.IN):
        self.type = p_type
        self.direction = p_direction
        self.value = None    # placeholder for parameter value


# Aliases for parameters
IN = Parameter()
OUT = Parameter(p_direction=Direction.OUT)
INOUT = Parameter(p_direction=Direction.INOUT)

FILE = Parameter(p_type=Type.FILE)
FILE_IN = Parameter(p_type=Type.FILE)
FILE_OUT = Parameter(p_type=Type.FILE, p_direction=Direction.OUT)
FILE_INOUT = Parameter(p_type=Type.FILE, p_direction=Direction.INOUT)

# Java max and min integer and long values
JAVA_MAX_INT = 2147483647
JAVA_MIN_INT = -2147483648
JAVA_MAX_LONG = PYTHON_MAX_INT = 9223372036854775807
JAVA_MIN_LONG = PYTHON_MIN_INT = -9223372036854775808
