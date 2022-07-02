--
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
--

--
-- Ensure history entry start/end dates are indexed.
--

CREATE INDEX guacamole_connection_history_start_date
    ON guacamole_connection_history (start_date);

CREATE INDEX guacamole_connection_history_end_date
    ON guacamole_connection_history (end_date);

CREATE INDEX guacamole_connection_history_search_index
    ON guacamole_connection_history (start_date, connection_id, user_id);

