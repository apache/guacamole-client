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


-- Records one authentication failure for a client address, returning the
-- number of failures recorded for that address within the ban window.
--
-- KEYS[1]  The failure counter for the client address.
-- ARGV[1]  The ban duration, in seconds.
--
-- Returns the value of the counter after it has been incremented.
--
-- Increments the counter and re-arms its expiry, mirroring the in-memory
-- tracker, where each failure both increments failureCount and resets
-- lastFailure. The counter therefore expires only once a full ban duration
-- has passed without a further failure.
--
-- Why a script: the increment and the expiry must happen together. If they
-- were separate commands and the EXPIRE were lost (for example, because the
-- connection failed between the two), the counter would never expire and the
-- address would remain banned permanently.

local count = redis.call('INCR', KEYS[1])
redis.call('EXPIRE', KEYS[1], ARGV[1])
return count
