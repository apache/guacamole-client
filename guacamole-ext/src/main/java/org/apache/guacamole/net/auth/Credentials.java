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

package org.apache.guacamole.net.auth;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


/**
 * Simple arbitrary set of credentials, including a username/password pair,
 * the HttpServletRequest associated with the request for authorization
 * (if any) and the HttpSession associated with that request.
 *
 * This class is used along with AuthenticationProvider to provide arbitrary
 * HTTP-based authentication for Guacamole.
 */
public class Credentials implements Serializable {

    /**
     * Unique identifier associated with this specific version of Credentials.
     */
    private static final long serialVersionUID = 1L;

    /**
     * An arbitrary username.
     */
    private String username;

    /**
     * An arbitrary password.
     */
    private String password;

    /**
     * The address of the client end of the connection which provided these
     * credentials, if known.
     */
    private String remoteAddress;

    /**
     * The hostname or, if the hostname cannot be determined, the address of
     * the client end of the connection which provided these credentials, if
     * known.
     */
    private String remoteHostname;

    /**
     * The HttpServletRequest carrying additional credentials, if any.
     */
    private transient HttpServletRequest request;

    /**
     * The HttpSession carrying additional credentials, if any.
     */
    private transient HttpSession session;

    /**
     * Returns the password associated with this set of credentials.
     * @return The password associated with this username/password pair, or
     *         null if no password has been set.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password associated with this set of credentials.
     * @param password The password to associate with this username/password
     *                 pair.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the username associated with this set of credentials.
     * @return The username associated with this username/password pair, or
     *         null if no username has been set.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username associated with this set of credentials.
     * @param username The username to associate with this username/password
     *                 pair.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the HttpServletRequest associated with this set of credentials.
     * @return The HttpServletRequest associated with this set of credentials,
     *         or null if no such request exists.
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Sets the HttpServletRequest associated with this set of credentials.
     * @param request  The HttpServletRequest to associated with this set of
     *                 credentials.
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the HttpSession associated with this set of credentials.
     * @return The HttpSession associated with this set of credentials, or null
     *         if no such request exists.
     */
    public HttpSession getSession() {
        return session;
    }

    /**
     * Sets the HttpSession associated with this set of credentials.
     * @param session The HttpSession to associated with this set of
     *                credentials.
     */
    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * Returns the address of the client end of the connection which provided
     * these credentials, if known.
     *
     * @return
     *     The address of the client end of the connection which provided these
     *     credentials, or null if the address is not known.
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Sets the address of the client end of the connection which provided
     * these credentials.
     *
     * @param remoteAddress
     *     The address of the client end of the connection which provided these
     *     credentials, or null if the address is not known.
     */
    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * Returns the hostname of the client end of the connection which provided
     * these credentials, if known. If the hostname of the client cannot be
     * determined, but the address is known, the address may be returned
     * instead.
     *
     * @return
     *     The hostname or address of the client end of the connection which
     *     provided these credentials, or null if the hostname is not known.
     */
    public String getRemoteHostname() {
        return remoteHostname;
    }

    /**
     * Sets the hostname of the client end of the connection which provided
     * these credentials, if known. If the hostname of the client cannot be
     * determined, but the address is known, the address may be specified
     * instead.
     *
     * @param remoteHostname
     *     The hostname or address of the client end of the connection which
     *     provided these credentials, or null if the hostname is not known.
     */
    public void setRemoteHostname(String remoteHostname) {
        this.remoteHostname = remoteHostname;
    }

}
