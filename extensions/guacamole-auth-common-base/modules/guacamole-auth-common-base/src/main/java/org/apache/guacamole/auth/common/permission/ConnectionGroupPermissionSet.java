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

package org.apache.guacamole.auth.common.permission;

import java.util.Map;

import com.google.inject.Inject;

/**
 * A database implementation of ObjectPermissionSet which uses an injected
 * service to query and manipulate the connection group permissions associated
 * with a particular user.
 */
public class ConnectionGroupPermissionSet extends ObjectPermissionSet {

    /**
     * Service for querying and manipulating connection group permissions.
     */
    private ObjectPermissionService connectionGroupPermissionService;
    
    @Inject
   	public ConnectionGroupPermissionSet(Map<String, ObjectPermissionService> mappers) {
    	connectionGroupPermissionService = mappers.get("ConnectionGroupPermissionService");
   	}
    
    @Override
    protected ObjectPermissionService getObjectPermissionService() {
        return connectionGroupPermissionService;
    }
 
}
