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

package org.apache.guacamole.auth.jdbc.user;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Associates a user with the credentials they used to authenticate.
 *
 * @author Michael Jumper 
 */
public class AuthenticatedUser implements org.apache.guacamole.net.auth.AuthenticatedUser {

    /**
     * The user that authenticated.
     */
    private final ModeledUser user;

    /**
     * The credentials given when this user authenticated.
     */
    private final Credentials credentials;

    /**
     * The AuthenticationProvider that authenticated this user.
     */
    private final AuthenticationProvider authenticationProvider;

    /**
     * The host from which this user authenticated.
     */
    private final String remoteHost;

    /**
     * Regular expression which matches any IPv4 address.
     */
    private static final String IPV4_ADDRESS_REGEX = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";

    /**
     * Regular expression which matches any IPv6 address.
     */
    private static final String IPV6_ADDRESS_REGEX = "([0-9a-fA-F]*(:[0-9a-fA-F]*){0,7})";

    /**
     * Regular expression which matches any IP address, regardless of version.
     */
    private static final String IP_ADDRESS_REGEX = "(" + IPV4_ADDRESS_REGEX + "|" + IPV6_ADDRESS_REGEX + ")";

    /**
     * Pattern which matches valid values of the de-facto standard
     * "X-Forwarded-For" header.
     */
    private static final Pattern X_FORWARDED_FOR = Pattern.compile("^" + IP_ADDRESS_REGEX + "(, " + IP_ADDRESS_REGEX + ")*$");

    /**
     * The connections which have been committed for use by this user in the
     * context of a balancing connection group. Balancing connection groups
     * will preferentially choose connections within this set, unless those
     * connections are not children of the group in question. If a group DOES
     * have at least one child connection within this set, no connections that
     * are not in this set will be used.
     */
    private final Set<String> preferredConnections =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Derives the remote host of the authenticating user from the given
     * credentials object. The remote host is derived from X-Forwarded-For
     * in addition to the actual source IP of the request, and thus is not
     * trusted. The derived remote host is really only useful for logging,
     * unless the server is configured such that X-Forwarded-For is guaranteed
     * to be trustworthy.
     *
     * @param credentials
     *     The credentials to derive the remote host from.
     *
     * @return
     *     The remote host from which the user with the given credentials is
     *     authenticating.
     */
    private static String getRemoteHost(Credentials credentials) {

        HttpServletRequest request = credentials.getRequest();

        // Use X-Forwarded-For, if present and valid
        String header = request.getHeader("X-Forwarded-For");
        if (header != null) {
            Matcher matcher = X_FORWARDED_FOR.matcher(header);
            if (matcher.matches())
                return matcher.group(1);
        }

        // If header absent or invalid, just use source IP
        return request.getRemoteAddr();

    }
    
    /**
     * Creates a new AuthenticatedUser associating the given user with their
     * corresponding credentials.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider that has authenticated the given user.
     *
     * @param user
     *     The user this object should represent.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public AuthenticatedUser(AuthenticationProvider authenticationProvider,
            ModeledUser user, Credentials credentials) {
        this.authenticationProvider = authenticationProvider;
        this.user = user;
        this.credentials = credentials;
        this.remoteHost = getRemoteHost(credentials);
    }

    /**
     * Returns the user that authenticated.
     *
     * @return 
     *     The user that authenticated.
     */
    public ModeledUser getUser() {
        return user;
    }

    /**
     * Returns the credentials given during authentication by this user.
     *
     * @return 
     *     The credentials given during authentication by this user.
     */
    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the host from which this user authenticated.
     *
     * @return
     *     The host from which this user authenticated.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Returns whether the connection having the given identifier has been
     * marked as preferred for this user's current Guacamole session. A
     * preferred connection is always chosen in favor of other connections when
     * it is a child of a balancing connection group.
     *
     * @param identifier
     *     The identifier of the connection to test.
     *
     * @return
     *     true if the connection having the given identifier has been marked
     *     as preferred, false otherwise.
     */
    public boolean isPreferredConnection(String identifier) {
        return preferredConnections.contains(identifier);
    }

    /**
     * Marks the connection having the given identifier as preferred for this
     * user's current Guacamole session. A preferred connection is always chosen
     * in favor of other connections when it is a child of a balancing
     * connection group.
     *
     * @param identifier
     *     The identifier of the connection to prefer.
     */
    public void preferConnection(String identifier) {
        preferredConnections.add(identifier);
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public String getIdentifier() {
        return user.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        user.setIdentifier(identifier);
    }

}
