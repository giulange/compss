/*         
 *  Copyright 2002-2018 Barcelona Supercomputing Center (www.bsc.es)
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
package es.bsc.compss.executor.external.piped;

import es.bsc.compss.executor.external.ExternalExecutorException;
import es.bsc.compss.executor.external.piped.commands.PipeCommand;
import es.bsc.compss.executor.external.piped.exceptions.ClosedPipeException;
import es.bsc.compss.executor.external.piped.exceptions.UnknownCommandException;
import es.bsc.compss.log.Loggers;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ControlPipePair {

    private static final Logger LOGGER = LogManager.getLogger(Loggers.WORKER_EXECUTOR);

    private boolean readerAlive = false;
    private boolean closed = false;
    private final PipePair pipe;
    private Thread reader;
    private final List<PipeCommand> unusedCommands;
    private final Map<PipeCommand, PendingCommandStatus> waitingCommands;

    public ControlPipePair(String basePipePath, String id) {
        pipe = new PipePair(basePipePath, id);
        waitingCommands = new TreeMap<>();
        unusedCommands = new LinkedList<>();
    }

    public boolean sendCommand(PipeCommand command) {
        return pipe.sendCommand(command);
    }

    public void waitForCommand(PipeCommand command) throws ClosedPipeException {
        PendingCommandStatus status = new PendingCommandStatus(command);
        PipeCommand unusedCommand;
        synchronized (this) {
            unusedCommand = pollMatchingUnusedCommand(command);
            if (unusedCommand == null) {
                if (closed) {
                    throw new ClosedPipeException();
                }
                waitingCommands.put(command, status);
                if (!readerAlive && !closed) {
                    readerAlive = true;
                    reader = new Thread(new ControlPipeReader());
                    reader.start();
                }
            } else {
                command.join(unusedCommand);
            }
        }
        if (unusedCommand == null) {
            try {
                status.waitUntilCompletion();
            } catch (InterruptedException ie) {
                //Do nothing
            }
        }
    }

    private PipeCommand pollMatchingUnusedCommand(PipeCommand command) {
        Iterator<PipeCommand> commandsItr = unusedCommands.iterator();
        while (commandsItr.hasNext()) {
            PipeCommand unusedCommand = commandsItr.next();
            if (unusedCommand.compareTo(command) == 0) {
                commandsItr.remove();
                return unusedCommand;
            }
        }
        return null;
    }

    public String getOutboundPipe() {
        return pipe.getOutboundPipe();
    }

    public String getInboundPipe() {
        return pipe.getInboundPipe();
    }

    public void noLongerExists() {
        synchronized (this) {
            closed = true;
        }
        pipe.noLongerExists();
        pipe.delete();
    }

    public void delete() {
        readerAlive = false;
        pipe.noLongerExists();
        pipe.delete();
    }


    private class ControlPipeReader implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName("Pipe " + pipe.getPipesLocation() + " Reader");
            try {
                while (readerAlive) {
                    PipeCommand command;
                    try {
                        command = pipe.readCommand();
                    } catch (UnknownCommandException uce) {
                        LOGGER.warn("UNKNOWN COMMAND RECEIVED TRHOUGH PIPE " + pipe.getInboundPipe() + ": " + uce.getMessage());
                        continue;
                    }
                    synchronized (ControlPipePair.this) {
                        PendingCommandStatus commandStatus = waitingCommands.remove(command);
                        if (commandStatus == null) {
                            ControlPipePair.this.unusedCommands.add(command);
                        } else {
                            commandStatus.completed(false, command);
                        }
                    }
                }
            } catch (ExternalExecutorException eee) {
                synchronized (ControlPipePair.this) {
                    for (PendingCommandStatus commandStatus : waitingCommands.values()) {
                        commandStatus.completed(true, null);
                    }

                }
            }
        }
    }


    private static class PendingCommandStatus {

        private final PipeCommand expectedCommand;
        private final Semaphore sem;
        private boolean failed;

        private PendingCommandStatus(PipeCommand command) {
            this.sem = new Semaphore(0);
            this.expectedCommand = command;
            failed = false;
        }

        private void waitUntilCompletion() throws InterruptedException, ClosedPipeException {
            sem.acquire();
            if (failed) {
                throw new ClosedPipeException();
            }
        }

        private void completed(boolean failure, PipeCommand receivedCommand) {
            failed = failure;
            if (!failure) {
                expectedCommand.join(receivedCommand);
            }
            sem.release(Integer.MAX_VALUE);
        }
    }
}