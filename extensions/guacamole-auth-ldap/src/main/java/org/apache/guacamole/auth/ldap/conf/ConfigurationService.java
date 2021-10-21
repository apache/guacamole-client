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
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding LDAP servers.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the configuration information for the LDAP servers related to
     * the user having the given username, if any. If multiple servers are
     * returned, each should be tried in order until a successful LDAP
     * connection is established.
     *
     * @param username
     *     The username of the user whose corresponding LDAP server
     *     configuration should be retrieved.
     *
     * @return
     *     The configurations of the LDAP servers related to the user having
     *     the given username.
     *
     * @throws GuacamoleException
     *     If the configuration information of the LDAP servers related to the
     *     user having the given username cannot be retrieved due to an error.
     */
    public Collection<LDAPConfiguration> getLDAPConfigurations(String username) throws GuacamoleException {
        return Collections.singletonList(new EnvironmentLDAPConfiguration(environment));
    }

}
