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

    private final String redisKey;
    private final int limit;
    private final SeatResult failureResult;

    /**
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

    public String getRedisKey() {
        return redisKey;
    }

    public int getLimit() {
        return limit;
    }

    public SeatResult getFailureResult() {
        return failureResult;
    }

}
