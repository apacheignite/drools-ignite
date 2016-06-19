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
package org.drools.persistence.ignite.processinstance;

import org.apache.ignite.IgniteCache;
import org.jbpm.persistence.ProcessPersistenceContext;
import org.jbpm.persistence.correlation.CorrelationKeyInfo;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.kie.internal.process.CorrelationKey;

import java.util.List;

public class IgniteProcessPersistenceContext implements ProcessPersistenceContext {


    public IgniteProcessPersistenceContext(IgniteCache<String, Object> cache){
    }

    public ProcessInstanceInfo persist(ProcessInstanceInfo processInstanceInfo) {
        return null;
    }

    public CorrelationKeyInfo persist(CorrelationKeyInfo correlationKeyInfo) {
        return null;
    }

    public ProcessInstanceInfo findProcessInstanceInfo(Long aLong) {
        return null;
    }

    public void remove(ProcessInstanceInfo processInstanceInfo) {

    }

    public List<Long> getProcessInstancesWaitingForEvent(String s) {
        return null;
    }

    public Long getProcessInstanceByCorrelationKey(CorrelationKey correlationKey) {
        return null;
    }
}
