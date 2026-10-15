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

import java.util.List;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuacdPoolTest {

    @Test
    public void staticPoolParsesAConfiguredHostList() throws Exception {

        GuacdPool pool = GuacdPool.fromHostList("guacd-a:4822, guacd-b:4823",
                4822, EncryptionMethod.NONE);

        List<GuacdEndpoint> candidates = pool.getCandidates();

        assertEquals(2, candidates.size());
        assertEquals(new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE),
                candidates.get(0));
        assertEquals(new GuacdEndpoint("guacd-b", 4823, EncryptionMethod.NONE),
                candidates.get(1));

    }

    @Test
    public void staticPoolAppliesTheDefaultPortWhenOmitted() throws Exception {

        GuacdPool pool = GuacdPool.fromHostList("guacd-a", 4822, EncryptionMethod.NONE);

        assertEquals(new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE),
                pool.getCandidates().get(0));

    }

    @Test
    public void dnsPoolResolvesLocalhostToAtLeastOneEndpoint() throws Exception {

        GuacdPool pool = GuacdPool.fromDns("localhost", 4822, EncryptionMethod.NONE, 1000L);

        List<GuacdEndpoint> candidates = pool.getCandidates();

        assertTrue(!candidates.isEmpty(), "expected at least one resolved address");
        assertEquals(4822, candidates.get(0).getPort());

    }

}
