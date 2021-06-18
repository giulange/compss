#!/bin/bash

def_cache=''
NO_CACHE=${1:-$def_cache}

# save the previous image in the meanwhile:
docker tag compss_ettfm compss_ettfm-old:old

# remove base tags:
docker rmi compss_ettfm

# re-build base images:
#  > armosa
docker build $NO_CACHE -t compss:ettfm -f Dockerfile_ettfm .
docker tag compss:ettfm compss_ettfm
docker rmi compss:ettfm

