#!/bin/bash

defval=6892
runID=${1:-$defval}

PORT=46300

echo "Running TFM with arg /media/run/$runID"

#docker exec -it pp-processor bash -c 'curl -v -s -XPUT http://compss_armosa:46400/COMPSs/startApplication -H "content-type: application/xml" -d "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><startApplication><ceiClass>unimi.armosa.armosalauncher.LauncherItf</ceiClass><className>unimi.armosa.armosalauncher.Launcher</className><hasResult>false</hasResult><methodName>main</methodName><parameters><params paramId=\"0\"><direction>IN</direction><paramName>args</paramName><prefix></prefix><stdIOStream>UNSPECIFIED</stdIOStream><type>OBJECT_T</type><array paramId=\"0\"><componentClassname>java.lang.String</componentClassname><values><element paramId=\"0\"><className>java.lang.String</className><value xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">/media/ram/'$runID'</value></element></values></array></params></parameters></startApplication>"'

docker exec -it pp-processor bash -c 'curl -s -XPUT http://compss_tfm:$PORT/COMPSs/startApplication -H "content-type: application/xml" -d "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><startApplication><ceiClass>mainCore.MainSimulationItf</ceiClass><className>mainCore.MainSimulation</className><hasResult>false</hasResult><methodName>main</methodName><parameters><params paramId=\"0\"><direction>IN</direction><paramName>args</paramName><prefix></prefix><stdIOStream>UNSPECIFIED</stdIOStream><type>OBJECT_T</type><array paramId=\"0\"><componentClassname>java.lang.String</componentClassname><values><element paramId=\"0\"><className>java.lang.String</className><value xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">/media/ram/$RUNID/</value></element></values></array></params></parameters></startApplication>"'



