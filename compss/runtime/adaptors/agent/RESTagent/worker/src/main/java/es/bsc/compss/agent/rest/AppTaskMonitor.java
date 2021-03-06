/*
 *  Copyright 2002-2019 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package es.bsc.compss.agent.rest;

import es.bsc.compss.agent.AppMonitor;
import es.bsc.compss.agent.rest.types.Orchestrator;
import es.bsc.compss.agent.rest.types.messages.EndApplicationNotification;
import es.bsc.compss.agent.types.ApplicationParameter;
import es.bsc.compss.types.annotations.parameter.DataType;
import es.bsc.compss.types.job.JobEndStatus;
import es.bsc.compss.util.ErrorManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;


/**
 * Class handling the status changes for a task and the corresponding notifications to its orchestrator.
 */
public class AppTaskMonitor extends AppMonitor {

    private static final Client CLIENT = ClientBuilder.newClient(new ClientConfig());

    private final Orchestrator orchestrator;

    private boolean successful;


    /**
     * Constructs a new AppTaskMonitor.
     *
     * @param args Monitored execution's arguments
     * @param target Monitored execution's target
     * @param results Monitored execution's results
     * @param orchestrator orchestrator to notify any task status updates.
     */
    public AppTaskMonitor(ApplicationParameter[] args, ApplicationParameter target, ApplicationParameter[] results,
        Orchestrator orchestrator) {
        super(args, target, results);
        this.orchestrator = orchestrator;
        this.successful = false;
    }

    @Override
    public void valueGenerated(int paramId, String paramName, DataType paramType, String dataId, Object dataLocation) {
        super.valueGenerated(paramId, paramName, paramType, dataId, dataLocation);
        /*
         * this.paramTypes[paramId] = paramType; if (paramType == DataType.OBJECT_T) { LogicalData ld =
         * Comm.getData(dataId); StubItf psco = (StubItf) ld.getValue(); psco.makePersistent(ld.getName());
         * this.paramTypes[paramId] = DataType.PSCO_T; ld.setPscoId(psco.getID()); DataLocation outLoc = null; try {
         * SimpleURI targetURI = new SimpleURI(ProtocolType.PERSISTENT_URI.getSchema() + psco.getID()); outLoc =
         * DataLocation.createLocation(Comm.getAppHost(), targetURI); this.paramLocations[paramId] = outLoc.toString();
         * } catch (Exception e) { ErrorManager.error(DataLocation.ERROR_INVALID_LOCATION + " " + dataId, e); } } else {
         * this.paramLocations[paramId] = dataLocation.toString(); }
         */
    }

    @Override
    public void onFailedExecution() {
        super.onFailedExecution();
        this.successful = false;
    }

    @Override
    public void onSuccesfulExecution() {
        super.onSuccesfulExecution();
        this.successful = true;
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        if (this.orchestrator != null) {
            String masterId = this.orchestrator.getHost();
            String operation = this.orchestrator.getOperation();
            WebTarget target = CLIENT.target(masterId);
            WebTarget wt = target.path(operation);
            EndApplicationNotification ean = new EndApplicationNotification("" + getAppId(),
                this.successful ? JobEndStatus.OK : JobEndStatus.EXECUTION_FAILED, this.getParamTypes(),
                this.getParamLocations());

            Response response = wt.request(MediaType.APPLICATION_JSON).put(Entity.xml(ean), Response.class);
            if (response.getStatusInfo().getStatusCode() != 200) {
                ErrorManager.warn("AGENT Could not notify Application " + getAppId() + " end to " + wt);
            }
        }
    }

}
