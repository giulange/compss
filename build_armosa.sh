#!/bin/bash

# save the previous image in the meanwhile:
docker tag armosa armosa-old:old

# remove base tags:
docker rmi armosa

# re-build base images:
#  > armosa
docker build --no-cache -t compss:armosa -f Dockerfile_armosa .
docker tag compss:armosa armosa
docker rmi compss:armosa

