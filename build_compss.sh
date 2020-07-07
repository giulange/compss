#!/bin/bash

# save the previous image in the meanwhile
docker tag compss:latest compss-old:old
docker rmi compss:latest

# 
docker build --no-cache -t compss:latest -f Dockerfile .



