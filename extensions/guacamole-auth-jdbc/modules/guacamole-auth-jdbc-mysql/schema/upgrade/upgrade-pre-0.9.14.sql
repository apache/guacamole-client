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
-- Add per-connection weight
--

ALTER TABLE guacamole_connection
    ADD COLUMN connection_weight int(11);

--
-- Add failover-only flag
--

ALTER TABLE guacamole_connection
    ADD COLUMN failover_only BOOLEAN NOT NULL DEFAULT 0;

--
-- Add remote_host to connection history
--

ALTER TABLE guacamole_connection_history
    ADD COLUMN remote_host VARCHAR(256) DEFAULT NULL;

--
-- Add template_connection_id to guacamole_connection
--

ALTER TABLE guacamole_connection
    ADD COLUMN template_connection_id int(11)
    REFERENCES guacamole_connection(connection_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

--
-- Check template_connection_id to make sure it is
-- not set to itself.
--

DELIMITER $
CREATE TRIGGER `template_connection_id_self_check` BEFORE UPDATE ON `guacamole_connection`
FOR EACH ROW
BEGIN
    IF new.`connection_id` = new.`template_connection_id` THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot set connection template to itself.';
    END IF;
END$
DELIMITER ;
