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

package org.apache.guacamole.auth.ldap;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for retrieving configuration information regarding the LDAP server.
 */
public class ConfigurationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the hostname of the LDAP server as configured with
     * guacamole.properties. By default, this will be "localhost".
     *
     * @return
     *     The hostname of the LDAP server, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getServerHostname() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_HOSTNAME,
            "localhost"
        );
    }

    /**
     * Returns the port of the LDAP server configured with
     * guacamole.properties. The default value depends on which encryption
     * method is being used. For unencrypted LDAP and STARTTLS, this will be
     * 389. For LDAPS (LDAP over SSL) this will be 636.
     *
     * @return
     *     The port of the LDAP server, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getServerPort() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_PORT,
            getEncryptionMethod().DEFAULT_PORT
        );
    }

    /**
     * Returns all username attributes which should be used to query and bind
     * users using the LDAP directory. By default, this will be "uid" - a
     * common attribute used for this purpose.
     *
     * @return
     *     The username attributes which should be used to query and bind users
     *     using the LDAP directory.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public List<String> getUsernameAttributes() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_USERNAME_ATTRIBUTE,
            Collections.singletonList("uid")
        );
    }

    /**
     * Returns the base DN under which all Guacamole users will be stored
     * within the LDAP directory.
     *
     * @return
     *     The base DN under which all Guacamole users will be stored within
     *     the LDAP directory.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the user base DN
     *     property is not specified.
     */
    public Dn getUserBaseDN() throws GuacamoleException {
        return environment.getRequiredProperty(
            LDAPGuacamoleProperties.LDAP_USER_BASE_DN
        );
    }

    /**
     * Returns the base DN under which all Guacamole configurations
     * (connections) will be stored within the LDAP directory. If Guacamole
     * configurations will not be stored within LDAP, null is returned.
     *
     * @return
     *     The base DN under which all Guacamole configurations will be stored
     *     within the LDAP directory, or null if no Guacamole configurations
     *     will be stored within the LDAP directory.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public Dn getConfigurationBaseDN() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_CONFIG_BASE_DN
        );
    }

    /**
     * Returns the base DN under which all Guacamole role based access control
     * (RBAC) groups will be stored within the LDAP directory. If RBAC will not
     * be used, null is returned.
     *
     * @return
     *     The base DN under which all Guacamole RBAC groups will be stored
     *     within the LDAP directory, or null if RBAC will not be used.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public Dn getGroupBaseDN() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_GROUP_BASE_DN
        );
    }

    /**
     * Returns the DN that should be used when searching for the DNs of users
     * attempting to authenticate. If no such search should be performed, null
     * is returned.
     *
     * @return
     *     The DN that should be used when searching for the DNs of users
     *     attempting to authenticate, or null if no such search should be
     *     performed.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public Dn getSearchBindDN() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_SEARCH_BIND_DN
        );
    }

    /**
     * Returns the password that should be used when binding to the LDAP server
     * using the DN returned by getSearchBindDN(). If no password should be
     * used, null is returned.
     *
     * @return
     *     The password that should be used when binding to the LDAP server
     *     using the DN returned by getSearchBindDN(), or null if no password
     *     should be used.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getSearchBindPassword() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_SEARCH_BIND_PASSWORD
        );
    }

    /**
     * Returns the encryption method that should be used when connecting to the
     * LDAP server. By default, no encryption is used.
     *
     * @return
     *     The encryption method that should be used when connecting to the
     *     LDAP server.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public EncryptionMethod getEncryptionMethod() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_ENCRYPTION_METHOD,
            EncryptionMethod.NONE
        );
    }

    /**
     * Returns maximum number of results a LDAP query can return,
     * as configured with guacamole.properties.
     * By default, this will be 1000.
     *
     * @return
     *     The maximum number of results a LDAP query can return,
     *     as configured with guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getMaxResults() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_MAX_SEARCH_RESULTS,
            1000 
        );
    }

    /**
     * Returns whether or not LDAP aliases will be dereferenced,
     * as configured with guacamole.properties. The default
     * behavior if not explicitly defined is to never
     * dereference them.
     *
     * @return
     *     The behavior for handling dereferencing of aliases
     *     as configured in guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public AliasDerefMode getDereferenceAliases() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_DEREFERENCE_ALIASES,
            AliasDerefMode.NEVER_DEREF_ALIASES
        );
    }

    /**
     * Returns the search filter that should be used when querying the
     * LDAP server for Guacamole users.  If no filter is specified,
     * a default of "(objectClass=*)" is returned.
     *
     * @return
     *     The search filter that should be used when querying the
     *     LDAP server for users that are valid in Guacamole, or
     *     "(objectClass=*)" if not specified.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getUserSearchFilter() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_USER_SEARCH_FILTER,
            "(objectClass=*)"
        );
    }

    /**
     * Returns the time limit, in seconds, that should be used when
     * performing LDAP operations.  If no limit is specified a default
     * of 30 seconds is returned.
     *
     * @return
     *     The number of seconds to allow LDAP operations to run
     *     before terminating the operation.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public Integer getOperationTimeout() throws GuacamoleException {
        return environment.getProperty(
            LDAPGuacamoleProperties.LDAP_OPERATION_TIMEOUT,
            30
        );
    }

}
