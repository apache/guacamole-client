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

import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for GuacdEndpoint, the value object identifying one guacd
 * instance. These tests verify the string form stored in Redis route entries
 * and load index keys, and the conversion to the standard
 * GuacamoleProxyConfiguration used to open a connection.
 */
public class GuacdEndpointTest {

    /**
     * Verifies that an endpoint converted to its key form with toKey() and
     * parsed back with fromKey() is equal to the original endpoint.
     */
    @Test
    public void roundTripsThroughKeyForm() {
        GuacdEndpoint endpoint = new GuacdEndpoint("guacd-0", 4822, EncryptionMethod.NONE);
        assertEquals(endpoint, GuacdEndpoint.fromKey(endpoint.toKey()));
    }

    /**
     * Verifies that an IPv6 literal hostname, which itself contains ":",
     * survives the key round trip intact. This is why the key form uses "|" as
     * its separator rather than ":".
     */
    @Test
    public void roundTripsIpv6Addresses() {
        GuacdEndpoint endpoint = new GuacdEndpoint("fd00::1", 4822, EncryptionMethod.SSL);
        assertEquals(endpoint, GuacdEndpoint.fromKey(endpoint.toKey()));
    }

    /**
     * Verifies that the hostname, port and encryption method of an endpoint are
     * carried over unchanged into the GuacamoleProxyConfiguration it produces.
     */
    @Test
    public void convertsToProxyConfiguration() {
        GuacdEndpoint endpoint = new GuacdEndpoint("guacd-1", 4823, EncryptionMethod.SSL);
        assertEquals("guacd-1", endpoint.toProxyConfiguration().getHostname());
        assertEquals(4823, endpoint.toProxyConfiguration().getPort());
        assertEquals(EncryptionMethod.SSL,
                endpoint.toProxyConfiguration().getEncryptionMethod());
    }

}
