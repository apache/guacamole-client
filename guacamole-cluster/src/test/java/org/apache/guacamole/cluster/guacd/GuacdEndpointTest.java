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

public class GuacdEndpointTest {

    @Test
    public void roundTripsThroughKeyForm() {
        GuacdEndpoint endpoint = new GuacdEndpoint("guacd-0", 4822, EncryptionMethod.NONE);
        assertEquals(endpoint, GuacdEndpoint.fromKey(endpoint.toKey()));
    }

    @Test
    public void roundTripsIpv6Addresses() {
        GuacdEndpoint endpoint = new GuacdEndpoint("fd00::1", 4822, EncryptionMethod.SSL);
        assertEquals(endpoint, GuacdEndpoint.fromKey(endpoint.toKey()));
    }

    @Test
    public void convertsToProxyConfiguration() {
        GuacdEndpoint endpoint = new GuacdEndpoint("guacd-1", 4823, EncryptionMethod.SSL);
        assertEquals("guacd-1", endpoint.toProxyConfiguration().getHostname());
        assertEquals(4823, endpoint.toProxyConfiguration().getPort());
        assertEquals(EncryptionMethod.SSL,
                endpoint.toProxyConfiguration().getEncryptionMethod());
    }

}
