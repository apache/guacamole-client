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

package org.apache.guacamole.cluster;

/**
 * Utilities for handling Redis URIs safely.
 */
public class RedisUris {

    /**
     * Value logged in place of a URI which cannot be parsed. Returning this
     * rather than the original is deliberate: a URI this class does not
     * understand is exactly the one most likely to carry a credential in a
     * shape not anticipated here.
     */
    private static final String UNPARSEABLE = "(redacted)";

    /**
     * Returns the given Redis URI with any credentials removed, leaving the
     * scheme, host and port.
     *
     * A Redis URI carries its credentials inline, as
     * "rediss://user:password@host:6379", so logging one verbatim writes the
     * password to the log. The scheme is preserved because it states whether
     * the connection is encrypted, and the host and port because they are what
     * an operator needs in order to diagnose anything.
     *
     * @param uri
     *     The Redis URI to redact, which may be null.
     *
     * @return
     *     The given URI with any credentials replaced, never null.
     */
    public static String redact(String uri) {

        if (uri == null)
            return UNPARSEABLE;

        int schemeEnd = uri.indexOf("://");
        if (schemeEnd < 0)
            return UNPARSEABLE;

        String scheme = uri.substring(0, schemeEnd + 3);
        String remainder = uri.substring(schemeEnd + 3);

        // Credentials, when present, precede the last "@" of the authority.
        // A password may itself contain "@", so the last one is the delimiter.
        int credentialsEnd = remainder.lastIndexOf('@');
        if (credentialsEnd < 0)
            return uri;

        return scheme + "***@" + remainder.substring(credentialsEnd + 1);

    }

}
