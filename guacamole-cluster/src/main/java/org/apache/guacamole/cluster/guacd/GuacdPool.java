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

package org.apache.guacamole.cluster.guacd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;

/**
 * The set of guacd instances this replica may connect to.
 *
 * Membership is owned by the platform, not by Guacamole: a DNS-backed pool
 * resolves a Kubernetes headless Service, which contains only ready pods.
 *
 * Note on DNS caching: the JVM caches successful lookups for 30 seconds by
 * default. The cache duration here therefore sets a floor, not a ceiling. In
 * Kubernetes, set -Dnetworkaddress.cache.ttl=5 (or the equivalent entry in
 * java.security) so that pod churn is observed promptly.
 */
public abstract class GuacdPool {

    /**
     * @return
     *     Every guacd instance currently believed to be available.
     *
     * @throws GuacamoleException
     *     If the set of candidates cannot be determined.
     */
    public abstract List<GuacdEndpoint> getCandidates() throws GuacamoleException;

    /**
     * Creates a pool from a comma-separated list of "host" or "host:port"
     * entries.
     *
     * @param hostList
     *     The configured list.
     *
     * @param defaultPort
     *     Port to use for entries which omit one.
     *
     * @param encryptionMethod
     *     Encryption method required by every instance in the pool.
     *
     * @return
     *     A pool over the configured hosts.
     */
    public static GuacdPool fromHostList(String hostList, final int defaultPort,
            final EncryptionMethod encryptionMethod) {

        final List<GuacdEndpoint> endpoints = new ArrayList<GuacdEndpoint>();

        for (String entry : hostList.split(",")) {

            String trimmed = entry.trim();
            if (trimmed.isEmpty())
                continue;

            int separator = trimmed.lastIndexOf(':');

            // Treat a trailing ":digits" as a port; anything else (including a
            // bare IPv6 literal) is a hostname
            if (separator > 0 && trimmed.substring(separator + 1).matches("\\d+"))
                endpoints.add(new GuacdEndpoint(
                        trimmed.substring(0, separator),
                        Integer.parseInt(trimmed.substring(separator + 1)),
                        encryptionMethod));
            else
                endpoints.add(new GuacdEndpoint(trimmed, defaultPort, encryptionMethod));

        }

        return new GuacdPool() {

            @Override
            public List<GuacdEndpoint> getCandidates() {
                return Collections.unmodifiableList(endpoints);
            }

        };

    }

    /**
     * Creates a pool which resolves every address of a hostname, intended for
     * use against a Kubernetes headless Service.
     *
     * @param hostname
     *     The hostname to resolve.
     *
     * @param port
     *     Port on which every resolved instance listens.
     *
     * @param encryptionMethod
     *     Encryption method required by every instance in the pool.
     *
     * @param cacheMs
     *     Milliseconds to reuse a resolution before resolving again.
     *
     * @return
     *     A pool over every address of the given hostname.
     */
    public static GuacdPool fromDns(final String hostname, final int port,
            final EncryptionMethod encryptionMethod, final long cacheMs) {

        return new GuacdPool() {

            private volatile List<GuacdEndpoint> cached = Collections.emptyList();
            private volatile long resolvedAt = 0L;

            @Override
            public synchronized List<GuacdEndpoint> getCandidates()
                    throws GuacamoleException {

                long now = System.currentTimeMillis();
                if (!cached.isEmpty() && now - resolvedAt < cacheMs)
                    return cached;

                try {

                    InetAddress[] addresses = InetAddress.getAllByName(hostname);
                    List<GuacdEndpoint> endpoints =
                            new ArrayList<GuacdEndpoint>(addresses.length);

                    for (InetAddress address : addresses)
                        endpoints.add(new GuacdEndpoint(address.getHostAddress(), port,
                                encryptionMethod));

                    cached = Collections.unmodifiableList(endpoints);
                    resolvedAt = now;
                    return cached;

                }
                catch (UnknownHostException e) {

                    // Prefer a stale answer to no answer: a transient DNS
                    // failure must not take down connection establishment
                    if (!cached.isEmpty())
                        return cached;

                    throw new GuacamoleServerException(
                            "Unable to resolve guacd hostname \"" + hostname + "\".", e);

                }

            }

        };

    }

}
