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
 * One limit-bearing sorted set: the Redis key, the maximum number of members
 * permitted, and the result to report if that maximum is reached.
 */
public class SeatKey {

    /**
     * The key of the Redis sorted set backing this limit, whose members are
     * the tunnels currently holding a seat.
     */
    private final String redisKey;

    /**
     * The maximum number of concurrent members permitted in the sorted set.
     * Zero or negative means unlimited.
     */
    private final int limit;

    /**
     * The result reported if this limit is what blocked acquisition.
     */
    private final SeatResult failureResult;

    /**
     * Creates a new SeatKey describing the given limit-bearing sorted set.
     *
     * @param redisKey
     *     The Redis sorted set backing this limit.
     *
     * @param limit
     *     The maximum number of concurrent members permitted. Zero or negative
     *     means unlimited.
     *
     * @param failureResult
     *     The result reported when this limit is what blocked acquisition.
     */
    public SeatKey(String redisKey, int limit, SeatResult failureResult) {
        this.redisKey = redisKey;
        this.limit = limit;
        this.failureResult = failureResult;
    }

    /**
     * Returns the key of the Redis sorted set backing this limit.
     *
     * @return
     *     The key of the Redis sorted set backing this limit.
     */
    public String getRedisKey() {
        return redisKey;
    }

    /**
     * Returns the maximum number of concurrent members permitted.
     *
     * @return
     *     The maximum number of concurrent members permitted, where zero or
     *     negative means unlimited.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns the result to report if this limit is what blocked acquisition.
     *
     * @return
     *     The result to report if this limit is what blocked acquisition.
     */
    public SeatResult getFailureResult() {
        return failureResult;
    }

}
