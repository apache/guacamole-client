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

package org.apache.guacamole.auth.sso.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.apache.guacamole.net.auth.AuthenticationSession;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Representation of an in-progress OpenID authentication attempt.
 */
public class SSOAuthenticationSession extends AuthenticationSession {
    /**
     * The key value used to store the redirection URI
     */
    private static String REDIRECTION = "redirection";

    /**
     * THe key value of the redirection URI in the credential parameers
     */
    private static String REQUEST_HREF = "href";

    /**
     * A Map of Arbitrary session data
     */
    private final Map<String, Object> session;

    /**
     * Creates a new AuthenticationSession representing an in-progress
     * authentication attempt.
     *
     * @param session
     *     A Map of the session data to be stored
     * 
     * @param expires
     *     The number of milliseconds that may elapse before this session must
     *     be considered invalid.
     */
    public SSOAuthenticationSession(Map<String,Object> session, long expires) {
        super(expires);
        this.session = session;
    }

    /**
     * Creates a new AuthenticationSession representing an in-progress
     * authentication attempt.
     * 
     * @param expires
     *     The number of milliseconds that may elapse before this session must
     *     be considered invalid.
     */
    public SSOAuthenticationSession(long expires) {
        this(new ConcurrentHashMap<>(), expires);
    }
    
    /**
     * Returns the stored session data
     *
     * @return
     *     The session data, can be null
     */
    public Map<String, Object> getSession() {
        return session;
    }

    /**
     * Returns an Object stored in the session data
     *
     * @return
     *     The object in the session, can be null
     */
    public Object get(String key) {
        return session.get(key);
    }

    /**
     * Returns an Object stored in the session data
     *
     * @return
     *     The object in the session, can be null
     */
    public void put(String key, Object value) {
        session.put(key, value);
    }

    /**
     * Special case for redirection from credentials to
     * simplify he authentication providers
     * 
     * @return 
     *      The redirection stored in teh session
     */
    public String getRedirection() {
        Object obj = session.get(REDIRECTION);
        return obj == null ? null : obj.toString();
    }

    /**
     * Special case for redirection from credentials to
     * simplify he authentication providers
     * 
     * @param credentials
     *      The credentials from which to extract the redirection.
     */
    public void setRedirection(Credentials credentials) {
        String redirection = credentials.getParameter(REQUEST_HREF);
        put(REDIRECTION, redirection);
    }
}


