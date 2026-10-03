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

import java.util.Objects;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;

/**
 * A single guacd instance: hostname, port, and required encryption method.
 *
 * The string form produced by toKey() is stored in Redis as the value of a
 * route entry and as part of a load index key. "|" is used as the separator
 * because IPv6 literal addresses contain ":".
 */
public class GuacdEndpoint {

    private static final String SEPARATOR = "|";

    private final String hostname;
    private final int port;
    private final EncryptionMethod encryptionMethod;

    public GuacdEndpoint(String hostname, int port, EncryptionMethod encryptionMethod) {
        this.hostname = hostname;
        this.port = port;
        this.encryptionMethod = encryptionMethod;
    }

    public static GuacdEndpoint from(GuacamoleProxyConfiguration config) {
        return new GuacdEndpoint(config.getHostname(), config.getPort(),
                config.getEncryptionMethod());
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public EncryptionMethod getEncryptionMethod() {
        return encryptionMethod;
    }

    public GuacamoleProxyConfiguration toProxyConfiguration() {
        return new GuacamoleProxyConfiguration(hostname, port, encryptionMethod);
    }

    /**
     * Returns the stable string form of this endpoint, suitable for storage in
     * Redis.
     *
     * @return
     *     This endpoint as "hostname|port|encryptionMethod".
     */
    public String toKey() {
        return hostname + SEPARATOR + port + SEPARATOR + encryptionMethod.name();
    }

    /**
     * Parses the string form produced by toKey().
     *
     * @param key
     *     The string form of an endpoint.
     *
     * @return
     *     The endpoint described by the given string.
     *
     * @throws IllegalArgumentException
     *     If the given string is not a valid endpoint key.
     */
    public static GuacdEndpoint fromKey(String key) {

        int portStart = key.indexOf(SEPARATOR);
        int methodStart = key.indexOf(SEPARATOR, portStart + 1);

        if (portStart < 0 || methodStart < 0)
            throw new IllegalArgumentException("Malformed guacd endpoint key: " + key);

        return new GuacdEndpoint(
                key.substring(0, portStart),
                Integer.parseInt(key.substring(portStart + 1, methodStart)),
                EncryptionMethod.valueOf(key.substring(methodStart + 1)));

    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof GuacdEndpoint))
            return false;

        GuacdEndpoint endpoint = (GuacdEndpoint) other;
        return port == endpoint.port
                && Objects.equals(hostname, endpoint.hostname)
                && encryptionMethod == endpoint.encryptionMethod;

    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port, encryptionMethod);
    }

    @Override
    public String toString() {
        return toKey();
    }

}
