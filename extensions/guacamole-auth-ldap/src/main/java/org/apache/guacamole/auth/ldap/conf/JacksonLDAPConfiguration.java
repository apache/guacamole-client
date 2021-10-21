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

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * LDAPConfiguration implementation that is annotated for deserialization by
 * Jackson.
 */
public class JacksonLDAPConfiguration implements LDAPConfiguration {

    /**
     * The regular expressions that match all users that should be routed to
     * the LDAP server represented by this configuration.
     */
    @JsonProperty("match-usernames")
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Pattern> matchUsernames;
    
    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_HOSTNAME}. If
     * not set within the YAML, this will be null.
     */
    @JsonProperty("hostname")
    private String hostname;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_PORT}. If not
     * set within the YAML, this will be null.
     */
    @JsonProperty("port")
    private Integer port;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_USERNAME_ATTRIBUTES}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("username-attribute")
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> usernameAttributes;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_USER_BASE_DN}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("user-base-dn")
    private String userBaseDn;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_CONFIG_BASE_DN}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("config-base-dn")
    private String configBaseDn;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_GROUP_BASE_DN}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("group-base-dn")
    private String groupBaseDn;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_GROUP_NAME_ATTRIBUTES}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("group-name-attribute")
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> groupNameAttributes;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_SEARCH_BIND_DN}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("search-bind-dn")
    private String searchBindDn;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_SEARCH_BIND_PASSWORD}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("search-bind-password")
    private String searchBindPassword;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_ENCRYPTION_METHOD}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("encryption-method")
    private String encryptionMethod;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_MAX_SEARCH_RESULTS}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("max-search-results")
    private Integer maxSearchResults;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_DEREFERENCE_ALIASES}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("dereference-aliases")
    private String dereferenceAliases;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_FOLLOW_REFERRALS}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("follow-referrals")
    private Boolean followReferrals;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_MAX_REFERRAL_HOPS}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("max-referral-hops")
    private Integer maxReferralHops;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_USER_SEARCH_FILTER}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("user-search-filter")
    private String userSearchFilter;
    
    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_GROUP_SEARCH_FILTER}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("group-search-filter")
    private String groupSearchFilter;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_OPERATION_TIMEOUT}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("operation-timeout")
    private Integer operationTimeout;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_USER_ATTRIBUTES}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("user-attributes")
    @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> userAttributes;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_MEMBER_ATTRIBUTE}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("member-attribute")
    private String memberAttribute;

    /**
     * The raw YAML value of {@link LDAPGuacamoleProperties#LDAP_MEMBER_ATTRIBUTE_TYPE}.
     * If not set within the YAML, this will be null.
     */
    @JsonProperty("member-attribute-type")
    private String memberAttributeType;

    @Override
    public String appliesTo(String username) throws GuacamoleException {

        for (Pattern pattern : matchUsernames) {
            Matcher matcher = pattern.matcher(username);
            if (matcher.matches())
                return matcher.groupCount() >= 1 ? matcher.group(1) : username;
        }

        return null;

    }

    @Override
    public String getServerHostname() {
        return hostname != null ? hostname : "localhost";
    }

    @Override
    public int getServerPort() throws GuacamoleException {
        return port != null ? port : getEncryptionMethod().DEFAULT_PORT;
    }

    @Override
    public List<String> getUsernameAttributes() {
        return usernameAttributes != null ? usernameAttributes : Collections.singletonList("uid");
    }

    @Override
    public Dn getUserBaseDN() throws GuacamoleException {

        Dn parsedDn = LDAPGuacamoleProperties.LDAP_USER_BASE_DN.parseValue(userBaseDn);
        if (parsedDn == null) 
            throw new GuacamoleServerException("The \"user-base-dn\" property is required for all LDAP servers.");

        return parsedDn;

    }

    @Override
    public Dn getConfigurationBaseDN() throws GuacamoleException {
        return LDAPGuacamoleProperties.LDAP_CONFIG_BASE_DN.parseValue(configBaseDn);
    }

    @Override
    public List<String> getGroupNameAttributes() throws GuacamoleException {
        return groupNameAttributes != null ? groupNameAttributes : Collections.singletonList("cn");
    }

    @Override
    public Dn getGroupBaseDN() throws GuacamoleException {
        return LDAPGuacamoleProperties.LDAP_GROUP_BASE_DN.parseValue(groupBaseDn);
    }

    @Override
    public String getSearchBindDN() throws GuacamoleException {
        return searchBindDn;
    }

    @Override
    public String getSearchBindPassword() throws GuacamoleException {
        return searchBindPassword;
    }

    @Override
    public EncryptionMethod getEncryptionMethod() throws GuacamoleException {

        EncryptionMethod parsedMethod = LDAPGuacamoleProperties.LDAP_ENCRYPTION_METHOD.parseValue(encryptionMethod);
        if (parsedMethod == null)
            return EncryptionMethod.NONE;

        return parsedMethod;

    }

    @Override
    public int getMaxResults() throws GuacamoleException {
        return maxSearchResults != null ? maxSearchResults : 1000;
    }

    @Override
    public AliasDerefMode getDereferenceAliases() throws GuacamoleException {

        AliasDerefMode parsedMode = LDAPGuacamoleProperties.LDAP_DEREFERENCE_ALIASES.parseValue(dereferenceAliases);
        if (parsedMode == null)
            return AliasDerefMode.NEVER_DEREF_ALIASES;

        return parsedMode;

    }

    @Override
    public boolean getFollowReferrals() throws GuacamoleException {
        return followReferrals != null ? followReferrals : false;
    }

    @Override
    public int getMaxReferralHops() throws GuacamoleException {
        return maxReferralHops != null ? maxReferralHops : 5;
    }

    @Override
    public ExprNode getUserSearchFilter() throws GuacamoleException {

        ExprNode parsedFilter = LDAPGuacamoleProperties.LDAP_USER_SEARCH_FILTER.parseValue(userSearchFilter);
        if (parsedFilter == null)
            return new PresenceNode("objectClass");

        return parsedFilter;

    }

    @Override
    public ExprNode getGroupSearchFilter() throws GuacamoleException {

        ExprNode parsedFilter = LDAPGuacamoleProperties.LDAP_GROUP_SEARCH_FILTER.parseValue(groupSearchFilter);
        if (parsedFilter == null)
            return new PresenceNode("objectClass");

        return parsedFilter;

    }

    @Override
    public int getOperationTimeout() throws GuacamoleException {
        return operationTimeout != null ? operationTimeout : 30;
    }

    @Override
    public List<String> getAttributes() throws GuacamoleException {
        return userAttributes != null ? userAttributes : Collections.<String>emptyList();
    }
    
    @Override
    public String getMemberAttribute() throws GuacamoleException {
        return memberAttribute != null ? memberAttribute : "member";
    }

    @Override
    public MemberAttributeType getMemberAttributeType()
            throws GuacamoleException {

        MemberAttributeType parsedType = LDAPGuacamoleProperties.LDAP_MEMBER_ATTRIBUTE_TYPE.parseValue(memberAttributeType);
        if (parsedType == null)
            return MemberAttributeType.DN;

        return parsedType;

    }

}
