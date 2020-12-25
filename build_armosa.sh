#!/bin/bash

def_cache=''
NO_CACHE=${1:-$def_cache}

# save the previous image in the meanwhile:
docker tag compss_armosa compss_armosa-old:old

# remove base tags:
docker rmi compss_armosa

# re-build base images:
#  > armosa
docker build $NO_CACHE -t compss:armosa -f Dockerfile_armosa .
docker tag compss:armosa compss_armosa
docker rmi compss:armosa

