-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

-- Atomically prunes stale members, verifies every applicable concurrency
-- limit, and claims a seat under each of them for one tunnel.
--
-- Each limit (for example, the maximum number of concurrent connections to a
-- connection, or per user) is represented by one sorted set whose members are
-- tunnel UUIDs and whose scores are the Redis server time, in milliseconds,
-- at which each member was last refreshed by its replica's heartbeat. A member
-- whose score is older than the stale window belongs to a replica that has
-- stopped heartbeating, and no longer occupies a seat.
--
-- KEYS[i]    The sorted set for the i-th limit, in an order defined by the
--            caller. The caller maps a non-zero return value back to the
--            limit that was reached using this order.
--
-- ARGV[1]    The UUID of the tunnel requesting seats.
-- ARGV[2]    The stale window, in milliseconds.
-- ARGV[i+2]  The limit for KEYS[i]. Zero or negative means unlimited; the
--            seat is still recorded so that the tunnel is counted.
--
-- Returns 0 if every limit allowed the tunnel and a seat was claimed under
-- every key, or the 1-based index of the first key whose limit was reached,
-- in which case no seat is claimed under any key. A tunnel which already
-- holds a seat under a key is never refused by that key, so retrying an
-- acquisition is safe and simply refreshes its score.
--
-- Why a script: the check and the claim must be a single atomic step. If they
-- were separate commands, two replicas could both observe a free seat and
-- both claim it, exceeding the limit. Redis runs a script with no other
-- command interleaved, which makes this check-and-set safe across every
-- replica, and guarantees that a refusal claims nothing (only stale members
-- are pruned). Time is taken from the Redis server rather than passed in so
-- that clock drift between replicas cannot affect staleness.

local uuid  = ARGV[1]
local stale = tonumber(ARGV[2])

local time = redis.call('TIME')
local now = (tonumber(time[1]) * 1000) + math.floor(tonumber(time[2]) / 1000)
local cutoff = now - stale

for i = 1, #KEYS do
    redis.call('ZREMRANGEBYSCORE', KEYS[i], '-inf', cutoff)
end

for i = 1, #KEYS do
    local limit = tonumber(ARGV[i + 2])
    if limit > 0 and redis.call('ZSCORE', KEYS[i], uuid) == false then
        if redis.call('ZCARD', KEYS[i]) >= limit then
            return i
        end
    end
end

for i = 1, #KEYS do
    redis.call('ZADD', KEYS[i], now, uuid)
end

return 0
