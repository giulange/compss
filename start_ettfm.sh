#!/bin/bash

/usr/sbin/sshd

export CLASSPATH=$CLASSPATH:/models/ET_TFM/build/libs/*.jar:/models/ET/build/libs/ET.jar:/models/TFM/build/libs/landsupport-TFM.jar:/usr/share/java/postgresql-jdbc4.jar
uid=$(uuidgen)

compss_agent_start --hostname=localhost --log_level=debug --log_dir=/logs/compss/${uid} --rest_port=46500 --classpath=${CLASSPATH} 

compss_agent_add_resources --agent_port=46500 --agent_node=localhost  --cpu=8 localhost

