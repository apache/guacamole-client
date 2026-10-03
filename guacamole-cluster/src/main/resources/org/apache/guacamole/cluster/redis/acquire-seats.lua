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

-- Atomically prunes stale members, verifies every limit, and takes seats.
--
-- KEYS    one sorted set per limit-bearing index, in caller-defined order
-- ARGV[1] tunnel UUID
-- ARGV[2] stale window, in milliseconds
-- ARGV[i+2] limit for KEYS[i]; zero or negative means unlimited
--
-- Returns 0 on success, or the 1-based index of the key whose limit was
-- reached. Time is taken from the Redis server so that replica clock drift
-- cannot affect staleness.

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
