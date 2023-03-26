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

package org.apache.guacamole.auth.restrict;

import inet.ipaddr.HostName;
import inet.ipaddr.HostNameException;
import inet.ipaddr.IPAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.restrict.connection.RestrictConnection;
import org.apache.guacamole.auth.restrict.user.RestrictUser;
import org.apache.guacamole.auth.restrict.usergroup.RestrictUserGroup;
import org.apache.guacamole.calendar.DailyRestriction;
import org.apache.guacamole.calendar.TimeRestrictionParser;
import org.apache.guacamole.host.HostRestrictionParser;
import org.apache.guacamole.language.TranslatableGuacamoleSecurityException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.UserGroup;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for verifying additional user login restrictions against a given
 * login attempt.
 */
public class RestrictionVerificationService {

    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictionVerificationService.class);

    /**
     * Parse out the provided strings of allowed and denied times, verifying
     * whether or not a login or connection should be allowed at the current
     * day and time. A boolean true will be returned if the action should be
     * allowed, otherwise false will be returned.
     * 
     * @param allowedTimeString
     *     The string containing the times that should be parsed to determine if
     *     the login or connection should be allowed at the current time, or
     *     null or an empty string if there are no specific allowed times defined.
     * 
     * @param deniedTimeString
     *     The string containing the times that should be parsed to determine if
     *     the login or connection should be denied at the current time, or null
     *     or an empty string if there are no specific times during which a
     *     action should be denied.
     * 
     * @return
     *     True if the login or connection should be allowed, otherwise false.
     */
    private static boolean allowedByTimeRestrictions(String allowedTimeString,
            String deniedTimeString) {
        
        // Check for denied entries, first, returning false if the login or
        // connection should not be allowed.
        if (deniedTimeString != null && !deniedTimeString.isEmpty()) {
            List<DailyRestriction> deniedTimes = 
                    TimeRestrictionParser.parseString(deniedTimeString);

            for (DailyRestriction restriction : deniedTimes) {
                if (restriction.appliesNow())
                    return false;
            }
        }
        
        // If no allowed entries are present, return true, allowing the login
        // or connection to continue.
        if (allowedTimeString == null || allowedTimeString.isEmpty())
            return true;
        
        List<DailyRestriction> allowedTimes = 
                TimeRestrictionParser.parseString(allowedTimeString);
        
        // Allowed entries are present, loop through them and check for a valid time.
        for (DailyRestriction restriction : allowedTimes) {
            // If this time allows the login or connection return true.
            if (restriction.appliesNow())
                return true;
        }
        
        // We have allowed entries, but login hasn't matched, so deny it.
        return false;
        
    }
    
    /**
     * Given the strings of allowed and denied hosts, verify that the login or
     * connection should be allowed from the given remote address. If the action
     * should not be allowed, return false - otherwise, return true.
     * 
     * @param allowedHostsString
     *     The string containing a semicolon-separated list of hosts from
     *     which the login or connection should be allowed, or null or an empty
     *     string if no specific set of allowed hosts is defined.
     * 
     * @param deniedHostsString
     *     The string containing a semicolon-separated list of hosts from
     *     which the login or connection should be denied, or null or an empty
     *     string if no specific set of denied hosts is defined.
     * 
     * @param remoteAddress
     *     The IP address from which the user is logging in or has logged in
     *     and is attempting to connect from, if it is known. If it is unknown
     *     and restrictions are defined, the login or connection will be denied.
     * 
     * @return
     *     True if the login or connection should be allowed by the host-based
     *     restrictions, otherwise false.
     */
    private static boolean allowedByHostRestrictions(String allowedHostsString,
            String deniedHostsString, String remoteAddress) {
        
        HostName remoteHostName = new HostName(remoteAddress);
        
        // If attributes do not exist or are empty then the action is allowed.
        if ((allowedHostsString == null || allowedHostsString.isEmpty()) 
                && (deniedHostsString == null || deniedHostsString.isEmpty()))
            return true;
        
        // If the remote address cannot be determined, and restrictions are
        // in effect, log an error and deny the action.
        if (remoteAddress == null || remoteAddress.isEmpty()) {
            LOGGER.warn("Host-based restrictions are present, but the remote "
                    + "address is invalid or could not be resolved. "
                    + "The action will not be allowed.");
            return false;
        }
        
        // Split denied hosts attribute and process each entry, checking them
        // against the current remote address, and returning false if a match is
        // found.
        List<HostName> deniedHosts = HostRestrictionParser.parseHostList(deniedHostsString);
        for (HostName hostName : deniedHosts) {
            try {
                if (hostName.isAddress() && hostName.toAddress().contains(remoteHostName.asAddress()))
                    return false;

                else
                    for (IPAddress currAddr : hostName.toAllAddresses())
                        if (currAddr.matches(remoteHostName.asAddressString()))
                            return false;
            }
            catch (UnknownHostException | HostNameException e) {
                LOGGER.warn("Unknown or invalid host in denied hosts list: \"{}\"", hostName);
                LOGGER.debug("Exception while trying to resolve host: \"{}\"", hostName, e);
                return false;
            }
        }
        
        // If denied hosts have been checked and allowed hosts are empty, we're
        // good, and can allow the action.
        if (allowedHostsString == null || allowedHostsString.isEmpty())
            return true;
        
        // Run through allowed hosts, if there are any, and return, allowing the
        // action if there are any matches.
        List<HostName> allowedHosts = HostRestrictionParser.parseHostList(allowedHostsString);
        for (HostName hostName : allowedHosts) {
            try {
                // If the entry is an IP or Subnet, check the remote address against it directly
                if (hostName.isAddress() && hostName.toAddress().contains(remoteHostName.asAddress()))
                    return true;
                
                // Entry is a hostname, so resolve to IPs and check each one
                for (IPAddress currAddr : hostName.toAllAddresses())
                    if (currAddr.matches(remoteHostName.asAddressString()))
                        return true;
                
            }
            // If an entry cannot be resolved we will log a warning.
            catch (UnknownHostException | HostNameException e) {
                LOGGER.warn("Unknown host encountered in allowed host string: {}", hostName);
                LOGGER.debug("Exception received trying to resolve host: {}", hostName, e);
            }
        }
        
        // If we've made it here, the allowed hosts do not contain the remote
        // address, and the action should not be allowed;
        return false;
        
    }
    
    /**
     * Verifies the login restrictions supported by this extension for the user
     * who is attempting to log in, throwing an exception if any of the
     * restrictions result in the user not being allowed to log in.
     * 
     * @param context
     *     The context of the user who is attempting to log in.
     * 
     * @param authenticatedUser
     *     The AuthenticatedUser object associated with the user who is
     *     attempting to log in.
     * 
     * @throws GuacamoleException 
     *     If any of the restrictions should prevent the user from logging in.
     */
    public static void verifyLoginRestrictions(UserContext context,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {
        
        // Get user's attributes
        Map<String, String> userAttributes = context.self().getAttributes();
        String remoteAddress = authenticatedUser.getCredentials().getRemoteAddress();
        
        if (context.self().getEffectivePermissions().getSystemPermissions().hasPermission(SystemPermission.Type.ADMINISTER)) {
            LOGGER.warn("User \"{}\" has System Administration permissions; additional restrictions will be bypassed.", 
                    authenticatedUser.getIdentifier());
            return;
        }
            
        // Verify time-based restrictions specific to the user
        String allowedTimeString = userAttributes.get(RestrictUser.RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME);
        String deniedTimeString = userAttributes.get(RestrictUser.RESTRICT_TIME_DENIED_ATTRIBUTE_NAME);
        if (!allowedByTimeRestrictions(allowedTimeString, deniedTimeString))
            throw new TranslatableInvalidTimeLoginException("User \"" 
                    + authenticatedUser.getIdentifier() 
                    + "\" is not allowed to log in at this time.",
                    "RESTRICT.ERROR_USER_LOGIN_NOT_ALLOWED_NOW");
        
        // Verify host-based restrictions specific to the user
        String allowedHostString = userAttributes.get(RestrictUser.RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME);
        String deniedHostString = userAttributes.get(RestrictUser.RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME);
        if (!allowedByHostRestrictions(allowedHostString, deniedHostString, remoteAddress))
            throw new TranslatableInvalidHostLoginException("User \"" 
                    + authenticatedUser.getIdentifier() 
                    +"\" is not allowed to log in from \"" 
                    + remoteAddress + "\"",
                    "RESTRICT.ERROR_USER_LOGIN_NOT_ALLOWED_FROM_HOST");
        
        // Gather user's effective groups.
        Set<String> userGroups = authenticatedUser.getEffectiveUserGroups();
        Directory<UserGroup> directoryGroups = context.getPrivileged().getUserGroupDirectory();
        
        // Loop user's effective groups and verify restrictions
        for (String userGroup : userGroups) {
            UserGroup thisGroup = directoryGroups.get(userGroup);
            if (thisGroup == null) {
                continue;
            }

            // Get group's attributes
            Map<String, String> grpAttributes = thisGroup.getAttributes();
            
            // Pull time-based restrictions for this group and verify
            String grpAllowedTimeString = grpAttributes.get(RestrictUserGroup.RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME);
            String grpDeniedTimeString = grpAttributes.get(RestrictUserGroup.RESTRICT_TIME_DENIED_ATTRIBUTE_NAME);
            if (!allowedByTimeRestrictions(grpAllowedTimeString, grpDeniedTimeString))
                throw new TranslatableInvalidTimeLoginException("User \"" 
                        + authenticatedUser.getIdentifier() 
                        +"\" is not allowed to log in at this time due to restrictions on group \"" 
                        + userGroup + "\".",
                        "RESTRICT.ERROR_USER_LOGIN_NOT_ALLOWED_NOW");
            
            // Pull host-based restrictions for this group and verify
            String grpAllowedHostString = grpAttributes.get(RestrictUserGroup.RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME);
            String grpDeniedHostString = grpAttributes.get(RestrictUserGroup.RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME);
            if (!allowedByHostRestrictions(grpAllowedHostString, grpDeniedHostString, remoteAddress))
                throw new TranslatableInvalidHostLoginException("User \"" 
                        + authenticatedUser.getIdentifier() 
                        + "\" is not allowed to log in from this host due to restrictions on group \"" 
                        + userGroup + "\".",
                        "RESTRICT.ERROR_USER_LOGIN_NOT_ALLOWED_FROM_HOST");
            
        }
        
    }
    
    /**
     * Verifies the connection restrictions supported by this extension for the
     * connection the user is attempting to access, throwing an exception if
     * any of the restrictions result in the connection being unavailable.
     * 
     * @param connectionAttributes
     *     The attributes of the connection that may contain any additional
     *     restrictions on use of the connection.
     * 
     * @param remoteAddress
     *     The remote IP address of the user trying to access the connection.
     * 
     * @throws GuacamoleException 
     *     If any of the restrictions should prevent the connection from being
     *     used by the user at the current time.
     */
    public static void verifyConnectionRestrictions(
            Map<String, String> connectionAttributes, String remoteAddress)
            throws GuacamoleException {
        
        // Verify time-based restrictions specific to this connection.
        String allowedTimeString = connectionAttributes.get(RestrictConnection.RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME);
        String deniedTimeString = connectionAttributes.get(RestrictConnection.RESTRICT_TIME_DENIED_ATTRIBUTE_NAME);
        if (!allowedByTimeRestrictions(allowedTimeString, deniedTimeString))
            throw new TranslatableGuacamoleSecurityException(
                    "Use of this connection is not allowed at this time.", 
                    "RESTRICT.ERROR_CONNECTION_NOT_ALLOWED_NOW"
            );
        
        // Verify host-based restrictions specific to this connection.
        String allowedHostString = connectionAttributes.get(RestrictConnection.RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME);
        String deniedHostString = connectionAttributes.get(RestrictConnection.RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME);
        if (!allowedByHostRestrictions(allowedHostString, deniedHostString, remoteAddress))
            throw new TranslatableGuacamoleSecurityException(
                    "Use of this connection is not allowed from this remote host: \"" + remoteAddress + "\".", 
                    "RESTRICT.ERROR_CONNECTION_NOT_ALLOWED_NOW"
            );
        
    }

}
