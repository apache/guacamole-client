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

CREATE TABLE [guacamole_user_password_reset] (

    [token_hash] [binary](32) NOT NULL,
    [user_id]    [int]        NOT NULL,
    [created]    [datetime]   NOT NULL,
    [expires]    [datetime]   NOT NULL,
    [used]       [datetime],

    CONSTRAINT [PK_guacamole_user_password_reset]
        PRIMARY KEY CLUSTERED ([token_hash]),

    CONSTRAINT [FK_guacamole_user_password_reset_user_id]
        FOREIGN KEY ([user_id])
        REFERENCES [guacamole_user] ([user_id])
        ON DELETE CASCADE

);

CREATE NONCLUSTERED INDEX [IX_guacamole_user_password_reset_user_id]
    ON [guacamole_user_password_reset] ([user_id]);

CREATE NONCLUSTERED INDEX [IX_guacamole_user_password_reset_created]
    ON [guacamole_user_password_reset] ([created]);
GO
