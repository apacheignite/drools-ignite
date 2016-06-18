package org.drools.persistence.ignite.processinstance;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.drools.core.common.InternalKnowledgeRuntime;
import org.jbpm.persistence.ProcessPersistenceContext;
import org.jbpm.persistence.ProcessPersistenceContextManager;
import org.jbpm.persistence.correlation.CorrelationKeyInfo;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.process.instance.timer.TimerManager;
import org.jbpm.workflow.instance.node.StateBasedNodeInstance;
import org.jbpm.workflow.instance.node.TimerNodeInstance;
import org.kie.api.definition.process.Process;
import org.jbpm.process.instance.ProcessInstanceManager;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an implementation of the {@link ProcessInstanceManager} that uses Ignite.
 * </p>
 * What's important to remember here is that we have a jbpm-console which has 1 static (stateful) knowledge session
 * which is used by multiple threads: each request sent to the jbpm-console is picked up in it's own thread.
 * </p>
 * This means that multiple threads can be using the same instance of this class.
 */

public class IgniteProcessInstanceManager implements ProcessInstanceManager{

    private InternalKnowledgeRuntime kruntime;
    // In a scenario in which 1000's of processes are running daily,
    //   lazy initialization is more costly than eager initialization
    // Added volatile so that if something happens, we can figure out what
    // in this case we can use the Ignite cache
    private volatile transient ConcurrentHashMap<Long, ProcessInstance> processInstances = new ConcurrentHashMap<Long, ProcessInstance>();

    public void setKnowledgeRuntime(InternalKnowledgeRuntime kruntime) {
        this.kruntime = kruntime;
    }
    public ProcessInstance getProcessInstance(long id) {
        return getProcessInstance(id, false);
    }

    public ProcessInstance getProcessInstance(long id, boolean readOnly) {
        InternalRuntimeManager manager = (InternalRuntimeManager) kruntime.getEnvironment().get("RuntimeManager");
        if (manager != null) {
            manager.validate((KieSession) kruntime, ProcessInstanceIdContext.get(id));
        }
        ProcessPersistenceContextManager ppcm
                = (ProcessPersistenceContextManager) this.kruntime.getEnvironment().get( EnvironmentName.PERSISTENCE_CONTEXT_MANAGER );
        ppcm.beginCommandScopedEntityManager();

        ProcessPersistenceContext context = ppcm.getProcessPersistenceContext();

        org.jbpm.process.instance.ProcessInstance processInstance = null;
        processInstance = (org.jbpm.process.instance.ProcessInstance) this.processInstances.get(id);
        if (processInstance != null) {
            if (processInstance.getKnowledgeRuntime() == null) {
                processInstance.setKnowledgeRuntime( kruntime );
                ((ProcessInstanceImpl) processInstance).reconnect();
            }
        	/*if (processInstance.getParentProcessInstanceId() > 0) {
        		context.persist(new ProcessInstanceInfo(processInstance, this.kruntime.getEnvironment()));
        	}*/
            return processInstance;
        }

        // Make sure that the cmd scoped entity manager has started
        ProcessInstanceInfo processInstanceInfo = context.findProcessInstanceInfo( id );
        if ( processInstanceInfo == null ) {
            return null;
        }
        if (!readOnly) {
            processInstanceInfo.updateLastReadDate();
        }
        processInstance = (org.jbpm.process.instance.ProcessInstance)
                processInstanceInfo.getProcessInstance(kruntime, this.kruntime.getEnvironment(), readOnly);
        processInstance.setId(processInstanceInfo.getId());
        if (((ProcessInstanceImpl) processInstance).getProcessXml() == null) {
            Process process = kruntime.getKieBase().getProcess( processInstance.getProcessId() );
            if ( process == null ) {
                throw new IllegalArgumentException( "Could not find process " + processInstance.getProcessId() );
            }
            processInstance.setProcess( process );
        }
        if ( processInstance.getKnowledgeRuntime() == null ) {
            Long parentProcessInstanceId = (Long) ((ProcessInstanceImpl) processInstance).getMetaData().get("ParentProcessInstanceId");
            if (parentProcessInstanceId != null) {
                kruntime.getProcessInstance(parentProcessInstanceId);
            }
            processInstance.setKnowledgeRuntime( kruntime );
            ((ProcessInstanceImpl) processInstance).reconnect();
        }
        return processInstance;
    }

    public ProcessInstance getProcessInstance(CorrelationKey correlationKey) {
        ProcessPersistenceContext context = ((ProcessPersistenceContextManager) this.kruntime.getEnvironment()
                .get( EnvironmentName.PERSISTENCE_CONTEXT_MANAGER ))
                .getProcessPersistenceContext();
        Long processInstanceId = context.getProcessInstanceByCorrelationKey(correlationKey);
        if (processInstanceId == null) {
            return null;
        }
        return getProcessInstance(processInstanceId);
    }

    public Collection<ProcessInstance> getProcessInstances() {
        return Collections.unmodifiableCollection(processInstances.values());
    }

    /**
     * Add a process instance to persistance context
     * @param processInstance
     * @param correlationKey
     */
    public void addProcessInstance(ProcessInstance processInstance, CorrelationKey correlationKey) {

        ProcessInstanceInfo processInstanceInfo = new ProcessInstanceInfo( processInstance, this.kruntime.getEnvironment() );
        ProcessPersistenceContext context
                = ((ProcessPersistenceContextManager)
                this.kruntime.getEnvironment().get(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER )).getProcessPersistenceContext();
        context.persist( processInstanceInfo );
        ((org.jbpm.process.instance.ProcessInstance) processInstance).setId( processInstanceInfo.getId() );
        processInstanceInfo.updateLastReadDate();
        // persist correlation if exists
        if (correlationKey != null) {
            CorrelationKeyInfo correlationKeyInfo = (CorrelationKeyInfo) correlationKey;
            correlationKeyInfo.setProcessInstanceId(processInstanceInfo.getId());
            context.persist(correlationKeyInfo);
        }
        internalAddProcessInstance(processInstance);

    }

    public void internalAddProcessInstance(ProcessInstance processInstance) {
        if( processInstances.putIfAbsent(processInstance.getId(), processInstance) != null ) {
            throw new ConcurrentModificationException(
                    "Duplicate process instance [" + processInstance.getProcessId() + "/" + processInstance.getId() + "]"
                            + " added to process instance manager." );
        }
    }

    public void removeProcessInstance(ProcessInstance processInstance) {
        ProcessPersistenceContext context = ((ProcessPersistenceContextManager) this.kruntime.getEnvironment().get( EnvironmentName.PERSISTENCE_CONTEXT_MANAGER )).getProcessPersistenceContext();
        ProcessInstanceInfo processInstanceInfo = context.findProcessInstanceInfo( processInstance.getId() );

        if ( processInstanceInfo != null ) {
            context.remove( processInstanceInfo );
        }
        internalRemoveProcessInstance(processInstance);
    }

    public void internalRemoveProcessInstance(ProcessInstance processInstance) {
        processInstances.remove( processInstance.getId() );

    }

    /**
     * Empty for now
     */
    public void clearProcessInstances() {

    }

    public void clearProcessInstancesState() {
        try {
            // at this point only timers are considered as state that needs to be cleared
            TimerManager
                    timerManager = ((InternalProcessRuntime)kruntime.getProcessRuntime()).getTimerManager();
            for (ProcessInstance processInstance: new ArrayList<ProcessInstance>(getProcessInstances())) {
                WorkflowProcessInstance pi = ((WorkflowProcessInstance) processInstance);

                for (org.kie.api.runtime.process.NodeInstance nodeInstance : pi.getNodeInstances()) {
                    if (nodeInstance instanceof TimerNodeInstance){
                        if (((TimerNodeInstance)nodeInstance).getTimerInstance() != null) {
                            timerManager.cancelTimer(((TimerNodeInstance)nodeInstance).getTimerInstance().getId());
                        }
                    } else if (nodeInstance instanceof StateBasedNodeInstance) {
                        List<Long> timerIds = ((StateBasedNodeInstance) nodeInstance).getTimerInstances();
                        if (timerIds != null) {
                            for (Long id: timerIds) {
                                timerManager.cancelTimer(id);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            // catch everything here to make sure it will not break any following
            // logic to allow complete clean up
        }
    }
}
