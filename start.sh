#!/bin/bash

/usr/sbin/sshd

export CLASSPATH=$CLASSPATH:/models/TFM/libs/*.jar:/models/ET/build/libs/ET.jar:/models/TFM/build/libs/landsupport-TFM.jar:/usr/share/java/postgresql-jdbc4.jar

compss_agent_start --hostname=localhost --log_level=debug --rest_port=46300 --classpath=${CLASSPATH} >> compss.log

