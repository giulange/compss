#!/bin/bash

# save the previous image in the meanwhile:
docker tag tfm tfm-old:old
docker tag armosa armosa-old:old

# remove base tags:
docker rmi tfm
docker rmi armosa

# re-build base images:
#  > tfm
docker build --no-cache -t compss:latest -f Dockerfile .
docker tag compss:latest tfm
docker rmi compss:latest
#  > armosa
docker build --no-cache -t compss:armosa -f Dockerfile_armosa .
docker tag compss:armosa armosa
docker rmi compss:armosa

