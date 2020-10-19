#!/bin/bash

defval="none"
container=${1:-$defval}

def_cache=""
#def_cache=" --no-cache"
NO_CACHE=${2:-$def_cache}

if [[ "${container}" != @(tfm|armosa|compss) ]];

then
  echo "Error: please provid a valid container name { tfm|armosa|compss }"  

else
  echo "Building $container container..."
  # save the previous image in the meanwhile:
  docker tag $container $container_old:old

  # remove base tags:
  docker rmi $container

  # re-build base images:
  docker build $NO_CACHE -t $container -f Dockerfile_$container .
  echo "...done!"
  echo ""
  echo ""

fi

