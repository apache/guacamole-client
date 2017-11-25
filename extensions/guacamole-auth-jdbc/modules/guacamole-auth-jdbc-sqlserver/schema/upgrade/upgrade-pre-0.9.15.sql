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
-- Table of arbitrary user attributes. Each attribute is simply a name/value
-- pair associated with a user. Arbitrary attributes are defined by other
-- extensions. Attributes defined by this extension will be mapped to
-- properly-typed columns of a specific table.
--

CREATE TABLE [guacamole_user_attribute] (

    [user_id]         [int]            NOT NULL,
    [attribute_name]  [nvarchar](128)  NOT NULL,
    [attribute_value] [nvarchar](4000) NOT NULL,

    CONSTRAINT [PK_guacamole_user_attribute]
        PRIMARY KEY CLUSTERED ([user_id], [attribute_name]),

    CONSTRAINT [FK_guacamole_user_attribute_user_id]
        FOREIGN KEY ([user_id])
        REFERENCES [guacamole_user] ([user_id])
        ON DELETE CASCADE

);

CREATE NONCLUSTERED INDEX [IX_guacamole_user_attribute_user_id]
    ON [guacamole_user_attribute] ([user_id])
    INCLUDE ([attribute_name], [attribute_value]);
GO

--
-- Table of arbitrary connection attributes. Each attribute is simply a
-- name/value pair associated with a connection. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE [guacamole_connection_attribute] (

    [connection_id]   [int]            NOT NULL,
    [attribute_name]  [nvarchar](128)  NOT NULL,
    [attribute_value] [nvarchar](4000) NOT NULL,

    PRIMARY KEY (connection_id, attribute_name),

    CONSTRAINT [FK_guacamole_connection_attribute_connection_id]
        FOREIGN KEY ([connection_id])
        REFERENCES [guacamole_connection] ([connection_id])
        ON DELETE CASCADE

);

CREATE NONCLUSTERED INDEX [IX_guacamole_connection_attribute_connection_id]
    ON [guacamole_connection_attribute] ([connection_id])
    INCLUDE ([attribute_name], [attribute_value]);
GO

--
-- Table of arbitrary connection group attributes. Each attribute is simply a
-- name/value pair associated with a connection group. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE [guacamole_connection_group_attribute] (

    [connection_group_id] [int]            NOT NULL,
    [attribute_name]      [nvarchar](128)  NOT NULL,
    [attribute_value]     [nvarchar](4000) NOT NULL,

    PRIMARY KEY (connection_group_id, attribute_name),

    CONSTRAINT [FK_guacamole_connection_group_attribute_connection_group_id]
        FOREIGN KEY ([connection_group_id])
        REFERENCES [guacamole_connection_group] ([connection_group_id])
        ON DELETE CASCADE

);

CREATE NONCLUSTERED INDEX [IX_guacamole_connection_group_attribute_connection_group_id]
    ON [guacamole_connection_group_attribute] ([connection_group_id])
    INCLUDE ([attribute_name], [attribute_value]);
GO

--
-- Table of arbitrary sharing profile attributes. Each attribute is simply a
-- name/value pair associated with a sharing profile. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE [guacamole_sharing_profile_attribute] (

    [sharing_profile_id] [int]            NOT NULL,
    [attribute_name]     [nvarchar](128)  NOT NULL,
    [attribute_value]    [nvarchar](4000) NOT NULL,

    PRIMARY KEY (sharing_profile_id, attribute_name),

    CONSTRAINT [FK_guacamole_sharing_profile_attribute_sharing_profile_id]
        FOREIGN KEY ([sharing_profile_id])
        REFERENCES [guacamole_sharing_profile] ([sharing_profile_id])
        ON DELETE CASCADE

);

CREATE NONCLUSTERED INDEX [IX_guacamole_sharing_profile_attribute_sharing_profile_id]
    ON [guacamole_sharing_profile_attribute] ([sharing_profile_id])
    INCLUDE ([attribute_name], [attribute_value]);
GO
