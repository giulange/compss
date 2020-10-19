#!/bin/bash

# save the previous image in the meanwhile:
docker tag tfm tfm-old:old

# remove base tags:
docker rmi tfm

# re-build base images:
#  > tfm
docker build --no-cache -t compss:latest -f Dockerfile .
docker tag compss:latest tfm
docker rmi compss:latest

