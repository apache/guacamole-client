/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.rest.work;

import java.util.Map;

import org.apache.guacamole.net.auth.Work;

public class APIWorkWrapper implements Work {
    
    /**
     * The wrapped APIWork.
     */
    private final APIWork apiWork;

    /**
     * Creates a new APIWorkWrapper which wraps the given APIWork,
     * exposing its properties through the Work interface.
     *
     * @param apiWork
     *     The APIWork to wrap.
     */
    public APIWorkWrapper(APIWork apiWork) {
        this.apiWork = apiWork;
    }

    @Override
    public String getIdentifier() {
        return apiWork.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        apiWork.setIdentifier(identifier);
    }

    @Override
    public Map<String, String> getAttributes() {
        return apiWork.getAttributes();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        apiWork.setAttributes(attributes);
    }

    @Override
    public String getName() {
        return apiWork.getName();
    }

    @Override
    public void setName(String name) {
        apiWork.setName(name);
    }
}
