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

package org.apache.guacamole.auth.ldap.conf;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding the LDAP server.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the configuration information for the LDAP server related to the
     * user having the given username. If no such LDAP server is defined, null
     * is returned.
     *
     * @param username
     *     The username of the user whose corresponding LDAP server
     *     configuration should be retrieved.
     *
     * @return
     *     The configuration of the LDAP server related to the user having the
     *     given username, or null if no such LDAP server is defined.
     *
     * @throws GuacamoleException
     *     If the configuration information of the LDAP server related to the
     *     user having the given username cannot be retrieved due to an error.
     */
    public LDAPConfiguration getLDAPConfiguration(String username) throws GuacamoleException {
        return new EnvironmentLDAPConfiguration(environment);
    }

}
