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
-- Single-use password reset tokens. Only the SHA-256 hash of each token is
-- stored; the token itself exists only in the e-mail sent to the user.
--

CREATE TABLE guacamole_user_password_reset (

  token_hash bytea       NOT NULL,
  user_id    integer     NOT NULL,
  created    timestamptz NOT NULL,
  expires    timestamptz NOT NULL,
  used       timestamptz,

  PRIMARY KEY (token_hash),

  CONSTRAINT guacamole_user_password_reset_ibfk_1
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_user_password_reset_user_id
    ON guacamole_user_password_reset(user_id);

CREATE INDEX guacamole_user_password_reset_created
    ON guacamole_user_password_reset(created);
