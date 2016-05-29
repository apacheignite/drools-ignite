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
import org.jbpm.process.instance.ProcessInstanceManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.process.CorrelationKey;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Define the class to manage the instance
 */
public class IgniteProcessInstanceManager implements ProcessInstanceManager{

    private InternalKnowledgeRuntime kruntime;
    // In a scenario in which 1000's of processes are running daily,
    //   lazy initialization is more costly than eager initialization
    // Added volatile so that if something happens, we can figure out what
    private volatile transient ConcurrentHashMap<Long, ProcessInstance> processInstances = new ConcurrentHashMap<Long, ProcessInstance>();

    public void setKnowledgeRuntime(InternalKnowledgeRuntime kruntime) {
        this.kruntime = kruntime;
    }

    public ProcessInstance getProcessInstance(long l) {
        return null;
    }

    public ProcessInstance getProcessInstance(long l, boolean b) {
        return null;
    }

    public ProcessInstance getProcessInstance(CorrelationKey correlationKey) {
        return null;
    }

    public Collection<ProcessInstance> getProcessInstances() {
        return null;
    }

    public void addProcessInstance(ProcessInstance processInstance, CorrelationKey correlationKey) {

    }

    public void internalAddProcessInstance(ProcessInstance processInstance) {

    }

    public void removeProcessInstance(ProcessInstance processInstance) {

    }

    public void internalRemoveProcessInstance(ProcessInstance processInstance) {

    }

    public void clearProcessInstances() {

    }

    public void clearProcessInstancesState() {

    }
}
