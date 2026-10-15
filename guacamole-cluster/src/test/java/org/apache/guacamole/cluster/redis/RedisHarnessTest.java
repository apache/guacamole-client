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

package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.testcontainers.DockerClientFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisHarnessTest {

    /**
     * Skips this class rather than failing it when no Docker daemon is
     * reachable. The image build runs the full test suite inside a container
     * that has no daemon of its own, and a Redis-backed test cannot be
     * meaningfully run there.
     */
    @BeforeAll
    public static void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Redis-backed tests.");
    }


    @Test
    public void redisContainerRespondsToPing() {
        RedisClient client = RedisClient.create(RedisTestSupport.redisUri());
        StatefulRedisConnection<String, String> connection = client.connect();
        try {
            assertEquals("PONG", connection.sync().ping());
        }
        finally {
            connection.close();
            client.shutdown();
        }
    }

}
