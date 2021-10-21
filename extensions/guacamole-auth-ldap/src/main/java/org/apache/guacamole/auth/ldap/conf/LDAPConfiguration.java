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

import java.util.List;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.guacamole.GuacamoleException;

/**
 * Configuration information defining how a particular LDAP server should be
 * queried.
 */
public interface LDAPConfiguration {

    /**
     * Tests whether this LDAPConfiguration applies to the user having the
     * given username. If the configuration applies, the username that should
     * be used to derive the user's DN is returned.
     *
     * @param username
     *     The username to test.
     *
     * @return
     *     The username that should be used to derive this user's DN, or null
     *     if the configuration does not apply.
     *
     * @throws GuacamoleException
     *     If an error prevents testing against this configuration.
     */
    String appliesTo(String username) throws GuacamoleException;
    
    /**
     * Returns the hostname or IP address of the LDAP server. By default, this
     * will be "localhost".
     *
     * @return
     *     The hostname or IP address of the LDAP server.
     *
     * @throws GuacamoleException
     *     If the hostname or IP address of the LDAP server cannot be
     *     retrieved.
     */
    String getServerHostname() throws GuacamoleException;

    /**
     * Returns the port of the LDAP server. The default value depends on which
     * encryption method is being used. For unencrypted LDAP and STARTTLS, this
     * will be 389. For LDAPS (LDAP over SSL) this will be 636.
     *
     * @return
     *     The port of the LDAP server.
     *
     * @throws GuacamoleException
     *     If the port of the LDAP server cannot be retrieved.
     */
    int getServerPort() throws GuacamoleException;

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
     *     If the username attributes cannot be retrieved.
     */
    List<String> getUsernameAttributes() throws GuacamoleException;

    /**
     * Returns the base DN under which all Guacamole users will be stored
     * within the LDAP directory.
     *
     * @return
     *     The base DN under which all Guacamole users will be stored within
     *     the LDAP directory.
     *
     * @throws GuacamoleException
     *     If the user base DN cannot be retrieved.
     */
    Dn getUserBaseDN() throws GuacamoleException;

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
     *     If the configuration base DN cannot be retrieved.
     */
    Dn getConfigurationBaseDN() throws GuacamoleException;

    /**
     * Returns all attributes which should be used to determine the unique
     * identifier of each user group. By default, this will be "cn".
     *
     * @return
     *     The attributes which should be used to determine the unique
     *     identifier of each group.
     *
     * @throws GuacamoleException
     *     If the group name attributes cannot be retrieved.
     */
    List<String> getGroupNameAttributes() throws GuacamoleException;

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
     *     If the group base DN cannot be retrieved.
     */
    Dn getGroupBaseDN() throws GuacamoleException;

    /**
     * Returns the login that should be used when searching for the DNs of users
     * attempting to authenticate. If no such search should be performed, null
     * is returned.
     *
     * @return
     *     The DN that should be used when searching for the DNs of users
     *     attempting to authenticate, or null if no such search should be
     *     performed.
     *
     * @throws GuacamoleException
     *     If the search bind DN cannot be retrieved.
     */
    String getSearchBindDN() throws GuacamoleException;

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
     *     If the search bind password cannot be retrieved.
     */
    String getSearchBindPassword() throws GuacamoleException;

    /**
     * Returns the encryption method that should be used when connecting to the
     * LDAP server. By default, no encryption is used.
     *
     * @return
     *     The encryption method that should be used when connecting to the
     *     LDAP server.
     *
     * @throws GuacamoleException
     *     If the encryption method cannot be retrieved.
     */
    EncryptionMethod getEncryptionMethod() throws GuacamoleException;

    /**
     * Returns maximum number of results a LDAP query can return. By default,
     * this will be 1000.
     *
     * @return
     *     The maximum number of results a LDAP query can return.
     *
     * @throws GuacamoleException
     *     If the maximum number of results cannot be retrieved.
     */
    int getMaxResults() throws GuacamoleException;

    /**
     * Returns whether or not LDAP aliases will be dereferenced. By default,
     * aliases are never dereferenced.
     *
     * @return
     *     The LDAP alias dereferencing mode.
     *
     * @throws GuacamoleException
     *     If the LDAP alias dereferencing mode cannot be retrieved.
     */
    AliasDerefMode getDereferenceAliases() throws GuacamoleException;

    /**
     * Returns whether referrals should be automatically followed. By default,
     * referrals are not followed.
     *
     * @return
     *     Whether referrals should be followed.
     *
     * @throws GuacamoleException
     *     If the configuration information determining whether LDAP referrals
     *     should be followed cannot be retrieved.
     */
    boolean getFollowReferrals() throws GuacamoleException;

    /**
     * Returns the maximum number of referral hops to follow. By default
     * a maximum of 5 hops is allowed.
     *
     * @return
     *     The maximum number of referral hops to follow.
     *
     * @throws GuacamoleException
     *     If the maximum number of referral hops cannot be retrieved.
     */
    int getMaxReferralHops() throws GuacamoleException;

    /**
     * Returns the search filter that should be used when querying the
     * LDAP server for Guacamole users.  If no filter is specified,
     * a default of "(objectClass=user)" is returned.
     *
     * @return
     *     The search filter that should be used when querying the
     *     LDAP server for users that are valid in Guacamole, or
     *     "(objectClass=user)" if not specified.
     *
     * @throws GuacamoleException
     *     If the user search filter cannot be retrieved.
     */
    ExprNode getUserSearchFilter() throws GuacamoleException;

    /**
     * Returns the search filter that should be used when querying the
     * LDAP server for Guacamole groups.  If no filter is specified,
     * a default of "(objectClass=*)" is used.
     *
     * @return
     *     The search filter that should be used when querying the
     *     LDAP server for groups that are valid in Guacamole, or
     *     "(objectClass=*)" if not specified.
     *
     * @throws GuacamoleException
     *     If the group search filter cannot be retrieved.
     */
    ExprNode getGroupSearchFilter() throws GuacamoleException;

    /**
     * Returns the maximum number of seconds to wait for LDAP operations.
     *
     * @return
     *     The maximum number of seconds to wait for LDAP operations.
     *
     * @throws GuacamoleException
     *     If the LDAP operation timeout cannot be retrieved.
     */
    int getOperationTimeout() throws GuacamoleException;

    /**
     * Returns names for custom LDAP user attributes that should be made
     * available as parameter tokens. By default, no additional LDAP attributes
     * will be exposed as parameter tokens.
     *
     * @return
     *     A list of all LDAP user attributes that should be made available as
     *     parameter tokens.
     *
     * @throws GuacamoleException
     *     If the names of custom LDAP user attributes cannot be retrieved.
     */
    List<String> getAttributes() throws GuacamoleException;
    
    /**
     * Returns the name of the LDAP attribute used to enumerate members in a
     * group. By default, this will be "member".
     * 
     * @return
     *     The name of the LDAP attribute to use to enumerate
     *     members in a group.
     * 
     * @throws GuacamoleException
     *     If the group member attribute cannot be retrieved.
     */
    String getMemberAttribute() throws GuacamoleException;

    /**
     * Returns whether the LDAP attribute used to enumerate members in a group
     * specifies a UID or DN.
     *
     * @return
     *     The type of data contained in the LDAP attribute used to enumerate
     *     members in a group.
     *
     * @throws GuacamoleException
     *     If the type of attribute used to enumerate group members cannot be
     *     retrieved.
     */
    MemberAttributeType getMemberAttributeType() throws GuacamoleException;

}
