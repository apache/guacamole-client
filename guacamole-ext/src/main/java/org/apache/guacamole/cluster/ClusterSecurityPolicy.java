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

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.metrics.ClusterMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether cluster state may be held in the Redis named by a given URI.
 *
 * This is the one place in the cluster implementation that fails closed. A
 * Redis outage degrades everywhere else, because the alternative is denying
 * service over a dependency users do not control; here the failure is a
 * configuration error, visible only at startup, and proceeding would produce a
 * deployment nobody would approve if asked.
 */
public class ClusterSecurityPolicy {

    /**
     * Logger for this class.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(ClusterSecurityPolicy.class);

    /**
     * Checks the given Redis URI, returning a one-line description of the
     * resulting security posture.
     *
     * @param uri
     *     The Redis URI cluster state will be held in.
     *
     * @param allowInsecure
     *     Whether an unauthenticated or unencrypted Redis has been explicitly
     *     opted into.
     *
     * @return
     *     A redacted, one-line description of the security posture, suitable
     *     for logging.
     *
     * @throws GuacamoleException
     *     If the URI is unacceptable and insecure operation was not opted into.
     */
    public static String check(String uri, boolean allowInsecure)
            throws GuacamoleException {

        // Classify the URI by the two properties that matter: TLS and
        // credentials
        boolean parseable = uri != null && uri.contains("://");
        boolean encrypted = parseable && uri.startsWith("rediss://");
        boolean authenticated = hasCredentials(uri);

        // Published from here because this is the only place that knows whether
        // the URI was accepted or merely tolerated. Before the first return, so
        // that a refused start publishes 1 for the seconds the process lives
        ClusterMetrics.gauge("guacamole_cluster_redis_insecure",
                parseable && encrypted && authenticated ? 0 : 1);

        // Fully secured: nothing further to decide
        if (parseable && encrypted && authenticated)
            return "Cluster state is held in an authenticated, encrypted Redis ("
                    + RedisUris.redact(uri) + ").";

        // Fail closed unless the operator has explicitly opted in
        if (!allowInsecure)
            throw new GuacamoleServerException("Refusing to hold cluster state "
                    + "in \"" + RedisUris.redact(uri) + "\": it is "
                    + describeShortcoming(parseable, encrypted, authenticated)
                    + ". Since session identity is held in this keyspace, an "
                    + "open Redis exposes who is logged in and from where. Use "
                    + "a \"rediss://user:password@host\" URI, or set "
                    + "\"cluster-allow-insecure-redis\" to true to accept this "
                    + "deliberately.");

        // Insecure but permitted: proceed, loudly
        logger.warn("Cluster state is held in an INSECURE Redis ({}): it is {}. "
                + "Session identity in this keyspace is readable by anything "
                + "that can reach the port. This was permitted by "
                + "\"cluster-allow-insecure-redis\".",
                RedisUris.redact(uri),
                describeShortcoming(parseable, encrypted, authenticated));

        // Name the actual shortcoming rather than a catch-all: an operator
        // reading "UNAUTHENTICATED" about a Redis that is authenticated but
        // unencrypted is being told something false
        return "Cluster state is held in an INSECURE Redis ("
                + RedisUris.redact(uri) + "): it is "
                + describeShortcoming(parseable, encrypted, authenticated) + ".";

    }

    /**
     * Returns whether the given URI carries credentials.
     *
     * @param uri
     *     The URI to examine, which may be null.
     *
     * @return
     *     true if the URI carries credentials, false otherwise.
     */
    private static boolean hasCredentials(String uri) {

        if (uri == null)
            return false;

        int schemeEnd = uri.indexOf("://");
        if (schemeEnd < 0)
            return false;

        return uri.indexOf('@', schemeEnd + 3) >= 0;

    }

    /**
     * Returns a human description of what is wrong with a URI.
     *
     * @param parseable
     *     Whether the URI could be understood at all.
     *
     * @param encrypted
     *     Whether the URI selects TLS.
     *
     * @param authenticated
     *     Whether the URI carries credentials.
     *
     * @return
     *     A description suitable for inclusion in a log line or an exception.
     */
    private static String describeShortcoming(boolean parseable,
            boolean encrypted, boolean authenticated) {

        if (!parseable)
            return "not a URI this policy can evaluate";

        if (!authenticated && !encrypted)
            return "neither authenticated nor encrypted";

        if (!authenticated)
            return "not authenticated";

        return "not encrypted";

    }

}
