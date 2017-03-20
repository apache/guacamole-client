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
    public String getUserBaseDN() throws GuacamoleException {
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
    public String getConfigurationBaseDN() throws GuacamoleException {
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
    public String getGroupBaseDN() throws GuacamoleException {
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
    public String getSearchBindDN() throws GuacamoleException {
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
     * as configured with guacamole.properties.  The default
     * behavior if not explicityly defined is to never 
     * dereference them.
     *
     * @return
     *     An integer value that maps to the JLDAP constants
     *     for dereferencing - 0 is DEREF_NEVER, 1 is DEREF_SEARCHING,
     *     2 is DEREF_FINDING, and 3 is DEREF_ALWAYS - as configured
     *     in guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getDereferenceAliases() throws GuacamoleException {
        String derefAliases = environment.getProperty(
            LDAPGuacamoleProperties.LDAP_DEREFERENCE_ALIASES,
            "never"
        );

        if (derefAliases.equals("always"))
            return 3;

        else if (derefAliases.equals("finding"))
            return 2;

        else if (derefAliases.equals("searching"))
            return 1;

        else if (derefAliases.equals("never"))
            return 0;
        
        else {
            logger.error("Invalid value given for ldap-dereference-aliases.");
            logger.debug("Received {} but expected one of the following: always, finding, searching, never.", derefAliases);
            throw new GuacamoleException("Invalid valid for ldap-dereference-aliases.");
        }

    }

}
