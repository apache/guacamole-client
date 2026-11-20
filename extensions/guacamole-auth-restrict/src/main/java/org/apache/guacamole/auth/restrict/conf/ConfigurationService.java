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
package org.apache.guacamole.auth.restrict.conf;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * A service for accessing the Guacamaole configuration in order to read
 * properties from the guacamole.properties file.
 */
public class ConfigurationService {
    
    /**
     * The environment of the Guacamole Server.
     */ 
    @Inject
    private Environment environment;
    
    /**
     * Return true if login restrictions implemented by this extension should
     * apply to system admin accounts in addition to non-administrative users,
     * or false if administrative accounts should be exempt from those
     * restrictions. This will return false by default, exempting system
     * administrators from login restrictions imposed by this extension.
     * 
     * @return
     *     true if login restrictions should apply to Guacamole system
     *     administrative accounts, otherwise false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    public boolean getRestrictAdminAccounts() throws GuacamoleException {
        return environment.getProperty(
                RestrictionProperties.RESTRICT_ADMIN_ACCOUNTS, 
                false
        );
    }
    
}
