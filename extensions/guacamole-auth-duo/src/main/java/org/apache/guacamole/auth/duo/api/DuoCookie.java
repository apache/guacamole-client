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

package org.apache.guacamole.auth.duo.api;

import com.google.common.io.BaseEncoding;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;

/**
 * Data which describes the identity of the user being verified by Duo.
 */
public class DuoCookie {

    /**
     * Pattern which matches valid cookies. Each cookie is made up of three
     * sections, separated from each other by pipe symbols ("|").
     */
    private static final Pattern COOKIE_FORMAT = Pattern.compile("([^|]+)\\|([^|]+)\\|([0-9]+)");

    /**
     * The index of the capturing group within COOKIE_FORMAT which contains the
     * username.
     */
    private static final int USERNAME_GROUP = 1;

    /**
     * The index of the capturing group within COOKIE_FORMAT which contains the
     * integration key.
     */
    private static final int INTEGRATION_KEY_GROUP = 2;

    /**
     * The index of the capturing group within COOKIE_FORMAT which contains the
     * expiration timestamp.
     */
    private static final int EXPIRATION_TIMESTAMP_GROUP = 3;

    /**
     * The username of the user being verified.
     */
    private final String username;

    /**
     * The integration key provided by Duo and specific to this deployment of
     * Guacamole.
     */
    private final String integrationKey;

    /**
     * The time that this cookie expires, in seconds since midnight of
     * 1970-01-01 (UTC).
     */
    private final long expires;

    /**
     * Creates a new DuoCookie which describes the identity of a user being
     * verified.
     *
     * @param username
     *     The username of the user being verified.
     *
     * @param integrationKey
     *     The integration key provided by Duo and specific to this deployment
     *     of Guacamole.
     *
     * @param expires
     *     The time that this cookie expires, in seconds since midnight of
     *     1970-01-01 (UTC).
     */
    public DuoCookie(String username, String integrationKey, long expires) {
        this.username = username;
        this.integrationKey = integrationKey;
        this.expires = expires;
    }

    /**
     * Returns the username of the user being verified.
     *
     * @return
     *     The username of the user being verified.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the integration key provided by Duo and specific to this
     * deployment of Guacamole.
     *
     * @return
     *     The integration key provided by Duo and specific to this deployment
     *     of Guacamole.
     */
    public String getIntegrationKey() {
        return integrationKey;
    }

    /**
     * Returns the time that this cookie expires. The expiration time is
     * represented in seconds since midnight of 1970-01-01 (UTC).
     *
     * @return
     *     The time that this cookie expires, in seconds since midnight of
     *     1970-01-01 (UTC).
     */
    public long getExpirationTimestamp(){
        return expires;
    }

    /**
     * Returns the current time as the number of seconds elapsed since
     * midnight of 1970-01-01 (UTC).
     *
     * @return
     *     The current time as the number of seconds elapsed since midnight of
     *     1970-01-01 (UTC).
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Returns whether this cookie has expired (the current time has met or
     * exceeded the expiration timestamp).
     *
     * @return
     *     true if this cookie has expired, false otherwise.
     */
    public boolean isExpired() {
        return currentTimestamp() >= expires;
    }

    /**
     * Parses a base64-encoded Duo cookie, producing a new DuoCookie object
     * containing the data therein. If the given string is not a valid Duo
     * cookie, an exception is thrown. Note that the cookie may be expired, and
     * must be checked for expiration prior to actual use.
     *
     * @param str
     *     The base64-encoded Duo cookie to parse.
     *
     * @return
     *     A new DuoCookie object containing the same data as the given
     *     base64-encoded Duo cookie string.
     *
     * @throws GuacamoleException
     *     If the given string is not a valid base64-encoded Duo cookie.
     */
    public static DuoCookie parseDuoCookie(String str) throws GuacamoleException {

        // Attempt to decode data as base64
        String data;
        try {
            data = new String(BaseEncoding.base64().decode(str), "UTF-8");
        }

        // Bail if invalid base64 is provided
        catch (IllegalArgumentException e) {
            throw new GuacamoleClientException("Username is not correctly "
                    + "encoded as base64.", e);
        }

        // Throw hard errors if standard pieces of Java are missing
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of "
                    + "UTF-8 support.", e);
        }

        // Verify format of provided data
        Matcher matcher = COOKIE_FORMAT.matcher(data);
        if (!matcher.matches())
            throw new GuacamoleClientException("Format of base64-encoded "
                    + "username is invalid.");

        // Get username and key (simple strings)
        String username = matcher.group(USERNAME_GROUP);
        String key = matcher.group(INTEGRATION_KEY_GROUP);

        // Parse expiration time
        long expires;
        try {
            expires = Long.parseLong(matcher.group(EXPIRATION_TIMESTAMP_GROUP));
        }

        // Bail if expiration timestamp is not a valid long
        catch (NumberFormatException e) {
            throw new GuacamoleClientException("Expiration timestamp is "
                    + "not valid.", e);
        }

        // Return parsed cookie
        return new DuoCookie(username, key, expires);

    }

    /**
     * Returns the base64-encoded string representation of this DuoCookie. The
     * format used is identical to that required by the Duo service: the
     * username, integration key, and expiration timestamp separated by pipe
     * symbols ("|") and encoded with base64.
     *
     * @return
     *     The base64-encoded string representation of this DuoCookie.
     */
    @Override
    public String toString() {

        try {

            // Separate each cookie field with pipe symbols
            String data = username + "|" + integrationKey + "|" + expires;

            // Encode resulting cookie string with base64
            return BaseEncoding.base64().encode(data.getBytes("UTF-8"));

        }

        // Throw hard errors if standard pieces of Java are missing
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }

    }

}
