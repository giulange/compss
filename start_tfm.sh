#!/bin/bash

/usr/sbin/sshd

export CLASSPATH=$CLASSPATH:/models/ET_TFM/build/libs/*.jar:/models/TFM/libs/*.jar:/models/ET/build/libs/ET.jar:/models/CROP_ARMOSA/ArmosaLauncher/target/*.jar:/models/TFM/build/libs/landsupport-TFM.jar:/usr/share/java/postgresql-jdbc4.jar
uid=$(uuidgen)

compss_agent_start --hostname=localhost --log_level=debug --log_dir=/logs/${uid} --rest_port=46300 --classpath=${CLASSPATH} >> /logs/compss.log

compss_agent_add_resources --agent_port=46300 --agent_node=localhost  --cpu=8 localhost

