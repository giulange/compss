package integratedtoolkit.scheduler.resourceEmptyScheduler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import integratedtoolkit.scheduler.exceptions.BlockedActionException;
import integratedtoolkit.scheduler.exceptions.InvalidSchedulingException;
import integratedtoolkit.scheduler.exceptions.UnassignedActionException;
import integratedtoolkit.scheduler.readyScheduler.ReadyScheduler;
import integratedtoolkit.scheduler.types.AllocatableAction;
import integratedtoolkit.scheduler.types.ObjectValue;
import integratedtoolkit.scheduler.types.ResourceEmptyScore;
import integratedtoolkit.scheduler.types.Profile;
import integratedtoolkit.scheduler.types.Score;
import integratedtoolkit.util.CoreManager;
import integratedtoolkit.util.ErrorManager;
import integratedtoolkit.util.ResourceScheduler;
import integratedtoolkit.types.implementations.Implementation;
import integratedtoolkit.types.resources.Worker;
import integratedtoolkit.types.resources.WorkerResourceDescription;


/**
 * Representation of a Scheduler that considers only ready tasks
 *
 * @param <P>
 * @param <T>
 */
public class ResourceEmptyScheduler<P extends Profile, T extends WorkerResourceDescription> extends ReadyScheduler<P, T> {

    /**
     * Constructs a new Ready Scheduler instance
     * 
     */
    public ResourceEmptyScheduler() {
        super();
    }

    @Override
    public ResourceScheduler<P, T> generateSchedulerForResource(Worker<T> w) {
        return new ResourceEmptyResourceScheduler<P, T>(w);
    }

    @Override
    public void actionCompleted(AllocatableAction<P, T> action) {
        ResourceScheduler<P, T> resource = action.getAssignedResource();
        if (action.getImplementations().length > 0) {
            Integer coreId = action.getImplementations()[0].getCoreId();
            if (coreId != null) {
                readyCounts[coreId]--;
            }
        }
        LinkedList<AllocatableAction<P, T>> dataFreeActions = action.completed();
        for (AllocatableAction<P, T> dataFreeAction : dataFreeActions) {
            if (dataFreeAction != null && dataFreeAction.isNotScheduling()) {
                if (dataFreeAction.getImplementations().length > 0) {
                    Integer coreId = dataFreeAction.getImplementations()[0].getCoreId();
                    if (coreId != null) {
                        readyCounts[coreId]++;
                    }
                }

                try {
                    dependencyFreeAction(dataFreeAction);
                } catch (BlockedActionException bae) {
                    if (!dataFreeAction.isLocked() && !dataFreeAction.isRunning()) {
                        logger.info("Blocked Action: " + dataFreeAction);
                        blockedActions.addAction(dataFreeAction);
                    }
                }
            }
        }

        LinkedList<AllocatableAction<P, T>> resourceFree = resource.unscheduleAction(action);
        workerLoadUpdate((ResourceScheduler<P, T>) action.getAssignedResource());
        HashSet<AllocatableAction<P, T>> freeTasks = new HashSet<>();
        freeTasks.addAll(dataFreeActions);
        freeTasks.addAll(resourceFree);
        for (AllocatableAction<P, T> a : freeTasks) {
            if (a != null && !a.isLocked() && !a.isRunning()) {
                try {
                    try {
                        a.tryToLaunch();
                    } catch (InvalidSchedulingException ise) {
                        Score aScore = generateActionScore(a);
                        boolean keepTrying = true;
                        for (int i = 0; i < action.getConstrainingPredecessors().size() && keepTrying; ++i) {
                            AllocatableAction<P, T> pre = action.getConstrainingPredecessors().get(i);
                            action.schedule(pre.getAssignedResource(), aScore);
                            try {
                                action.tryToLaunch();
                                keepTrying = false;
                            } catch (InvalidSchedulingException ise2) {
                                // Try next predecessor
                                keepTrying = true;
                            }
                        }
                    }

                } catch (UnassignedActionException ure) {
                    StringBuilder info = new StringBuilder("Scheduler has lost track of action ");
                    info.append(action.toString());
                    ErrorManager.fatal(info.toString());
                } catch (BlockedActionException bae) {
                    if (a != null && !a.isLocked() && !a.isRunning()) {
                        logger.info("Blocked Action: " + a, bae);
                        blockedActions.addAction(a);
                    }
                }
            }
        }

    }

    @Override
    public void dependencyFreeAction(AllocatableAction<P, T> action) throws BlockedActionException {
        dependingActions.removeAction(action);
        try {
            Score actionScore = generateActionScore(action);
            action.schedule(actionScore);
            try {
                action.tryToLaunch();
            } catch (InvalidSchedulingException ise) {
                boolean keepTrying = true;
                for (int i = 0; i < action.getConstrainingPredecessors().size() && keepTrying; ++i) {
                    AllocatableAction<P, T> pre = action.getConstrainingPredecessors().get(i);
                    action.schedule(pre.getAssignedResource(), actionScore);
                    try {
                        action.tryToLaunch();
                        keepTrying = false;
                    } catch (InvalidSchedulingException ise2) {
                        // Try next predecessor
                        keepTrying = true;
                    }
                }
            }
        } catch (UnassignedActionException ex) {
            logger.debug("Adding action " + action + " to unassigned list");
            unassignedReadyActions.addAction(action);
        }
    }
/*
    @Override
    protected final void scheduleAction(AllocatableAction<P, T> action, Score actionScore) throws BlockedActionException {
        if (action.hasDataPredecessors()) {
            dependingActions.addAction(action);
        } else {
            try {
                action.schedule(actionScore);
            } catch (UnassignedActionException ex) {
                logger.debug("Adding action " + action + " to unassigned list");
                unassignedReadyActions.addAction(action);
            }
        }
    }*/
/*
    @SuppressWarnings("unchecked")
    @Override
    public void workerLoadUpdate(ResourceScheduler<P, T> resource) {
        Worker<T> worker = resource.getResource();
        // Resource capabilities had already been taken into account when
        // assigning the actions. No need to change the assignation.
        PriorityQueue<ObjectValue<AllocatableAction<P, T>>>[] actions = new PriorityQueue[CoreManager.getCoreCount()];

        // Selecting runnable actions and priorizing them
        LinkedList<Integer> runnableCores = new LinkedList<>();
        LinkedList<Implementation<T>>[] fittingImpls = new LinkedList[CoreManager.getCoreCount()];
        for (int coreId : (LinkedList<Integer>) worker.getExecutableCores()) {
            fittingImpls[coreId] = worker.getRunnableImplementations(coreId);
            if (!fittingImpls[coreId].isEmpty() && unassignedReadyActions.getActionCounts()[coreId] > 0) {
                actions[coreId] = sortActionsForResource(unassignedReadyActions.getActions(coreId), resource);
                // check actions[coreId] is not empty
                if (!actions[coreId].isEmpty()) {
                    runnableCores.add(coreId);
                }
            }
        }

        while (!runnableCores.isEmpty()) {
            // Pick Best Action
            Integer bestCore = null;
            Score bestScore = null;
            for (Integer i : runnableCores) {
                Score coreScore = actions[i].peek().getScore();
                if (Score.isBetter(coreScore, bestScore)) {
                    bestScore = coreScore;
                    bestCore = i;
                }
            }

            ObjectValue<AllocatableAction<P, T>> ov = actions[bestCore].poll();
            AllocatableAction<P, T> selectedAction = ov.getObject();

            if (actions[bestCore].isEmpty()) {
                runnableCores.remove(bestCore);
            }
            unassignedReadyActions.removeAction(selectedAction);

            // Get the best Implementation and tryToLaunch
            try {
                Score actionScore = generateActionScore(selectedAction);
                selectedAction.schedule(resource, actionScore);
                try {
                    selectedAction.tryToLaunch();
                } catch (InvalidSchedulingException ise) {
                    boolean keepTrying = true;
                    for (int i = 0; i < selectedAction.getConstrainingPredecessors().size() && keepTrying; ++i) {
                        AllocatableAction<P, T> pre = selectedAction.getConstrainingPredecessors().get(i);
                        selectedAction.schedule(pre.getAssignedResource(), actionScore);
                        try {
                            selectedAction.tryToLaunch();
                            keepTrying = false;
                        } catch (InvalidSchedulingException ise2) {
                            // Try next predecessor
                            keepTrying = true;
                        }
                    }
                    if (keepTrying) {
                        // Action couldn't be assigned
                        unassignedReadyActions.addAction(selectedAction);
                    }
                }
            } catch (UnassignedActionException uae) {
                // Action stays unassigned and ready
                unassignedReadyActions.addAction(selectedAction);
                continue;
            } catch (BlockedActionException bae) {
                // Never happens!
                unassignedReadyActions.addAction(selectedAction);
                continue;
            }

            // Update Runnable Cores
            Iterator<Integer> coreIter = runnableCores.iterator();
            while (coreIter.hasNext()) {
                int coreId = coreIter.next();
                fittingImpls[coreId] = worker.getRunnableImplementations(coreId);
                Iterator<Implementation<T>> implIter = fittingImpls[coreId].iterator();
                while (implIter.hasNext()) {
                    Implementation<?> impl = implIter.next();
                    T requirements = (T) impl.getRequirements();
                    if (!worker.canRunNow(requirements)) {
                        implIter.remove();
                    }
                }
                if (fittingImpls[coreId].isEmpty() || unassignedReadyActions.getActionCounts()[coreId] == 0) {
                    coreIter.remove();
                }
            }
        }
    }*/

    @Override
    protected PriorityQueue<ObjectValue<AllocatableAction<P, T>>> sortActionsForResource(LinkedList<AllocatableAction<P, T>> actions,
            ResourceScheduler<P, T> resource) {

        PriorityQueue<ObjectValue<AllocatableAction<P, T>>> pq = new PriorityQueue<>();
        int counter = 0;
        for (AllocatableAction<P, T> action : actions) {
            Score actionScore = generateActionScore(action);
            Score score = generateFullScore(action, resource, actionScore);
            if (score == null) {
                continue;
            }
            ObjectValue<AllocatableAction<P, T>> ov = new ObjectValue<AllocatableAction<P, T>>(action, score);
            pq.offer(ov);
            counter++;
            if (counter == THRESHOLD) {
                break;
            }
        }
        return pq;
    }

    @Override
    public Score generateActionScore(AllocatableAction<P, T> action) {
        return new ResourceEmptyScore(action.getPriority(), 0, 0, 0);
    }

    @Override
    protected Score generateFullScore(AllocatableAction<P, T> action, ResourceScheduler<P, T> resource, Score actionScore) {
        return action.schedulingScore(resource, actionScore);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResourceScheduler<P, T>[] getWorkers() {
        synchronized (workers) {
            Collection<ResourceScheduler<P, T>> resScheds = workers.values();
            ResourceEmptyResourceScheduler<P, T>[] scheds = new ResourceEmptyResourceScheduler[resScheds.size()];
            workers.values().toArray(scheds);
            return scheds;
        }
    }

}