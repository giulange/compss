#!/bin/bash
def_cache=''
NO_CACHE=${1:-$def_cache}

# save the previous image in the meanwhile:
docker tag compss_tfm compss_tfm-old:old

# remove base tags:
docker rmi compss_tfm

# re-build base images:
#  > tfm
docker build $NO_CACHE -t compss:tfm -f Dockerfile_tfm .
docker tag compss:tfm compss_tfm
docker rmi compss:tfm

