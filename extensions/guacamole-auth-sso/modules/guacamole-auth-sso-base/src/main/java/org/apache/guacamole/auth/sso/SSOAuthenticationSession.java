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

package org.apache.guacamole.auth.sso;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.Map;
import org.apache.guacamole.net.auth.AuthenticationSession;

/**
 * Representation of an in-progress OpenID authentication attempt.
 */
public class SSOAuthenticationSession extends AuthenticationSession {
    /**
     * A Map of Arbitrary session data
     */
    private final Map<String, Object> session;

    /**
     * A function that can be provider by the AuthenicationProvider to
     * determine is the session is still valid. It receives the session
     * data as a Map<String, Object>
     */
    private Predicate<Map<String, Object>> validCallback;

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
        this.session = new ConcurrentHashMap<>(session);
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
     * @param key
     *     The key of the object to return in the concurrent map
     * 
     * @return
     *     The object in the session, can be null
     */
    public Object get(String key) {
        return session.get(key);
    }

    /**
     * Returns a String stored in the session data
     * 
     * @param key
     *     The key of the object to return in the concurrent map
     *
     * @return
     *     The object in the session, can be null
     */
    public String getString(String key) {
        Object obj = session.get(key);
        return obj == null ? null : obj.toString();
    }

    /**
     * Put or overwrites an object in the session
     * 
     * @param key
     *     The key of the object to return in the concurrent map
     * 
     * @param value
     *     The Object to put into the concurrent map
     *
     * @return
     *     The object in the session, can be null
     */
    public void put(String key, Object value) {
        if (key != null) {
            if (value == null)
                session.remove(key);
            else
                session.put(key, value);
        }
    }

    /**
      * Allows the authenticationProvider to supply a callback to identify 
     * conditions of valid sessions. The callback receives the session map.
     * 
     * @param validCallback
     *     A Predicate that receives the session map. It can be written like
     *     
     *         session.setValidCallback(session -> {
     *             Object identity = session.get("id");
     *             return identity != null && !identity.toString().isEmpty(); 
     *         })
     */
    public void setValidCallback(Predicate<Map<String, Object>> validCallback) {
        this.validCallback = validCallback;
    }

    /**
     * Returns whether this authentication session is still valid (has not yet
     * expired) and an authenicationProvider supplied callback returns valid
     * 
     * @return
     *     Returns true if is still valid, false if it has
     *     expired or authenicationProvider callback is false.
     */
    @Override
    public boolean isValid() {
        return super.isValid() && (validCallback == null || validCallback.test(session));
    }    
}