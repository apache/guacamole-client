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
-- Add new system-level permission
--

EXEC sp_unbindrule 'guacamole_system_permission';
DROP RULE [guacamole_system_permission_list];
GO

CREATE RULE [guacamole_system_permission_list] AS @list IN (
    'CREATE_CONNECTION',
    'CREATE_CONNECTION_GROUP',
    'CREATE_SHARING_PROFILE',
    'CREATE_USER',
    'CREATE_USER_GROUP',
    'ADMINISTER'
);
GO

EXEC sp_bindrule
    'guacamole_system_permission_list',
    'guacamole_system_permission';
GO

--
-- Entity types
--

CREATE RULE [guacamole_entity_type_list] AS @list IN (
    'USER',
    'USER_GROUP'
);
GO

CREATE TYPE [guacamole_entity_type] FROM [nvarchar](16);
EXEC sp_bindrule
    'guacamole_entity_type_list',
    'guacamole_entity_type';
GO

--
-- Table of base entities which may each be either a user or user group. Other
-- tables which represent qualities shared by both users and groups will point
-- to guacamole_entity, while tables which represent qualities specific to
-- users or groups will point to guacamole_user or guacamole_user_group.
--

CREATE TABLE [guacamole_entity] (

    [entity_id]     [int] IDENTITY(1,1)     NOT NULL,
    [name]          [nvarchar](128)         NOT NULL,
    [type]          [guacamole_entity_type] NOT NULL,

    CONSTRAINT [PK_guacamole_entity]
        PRIMARY KEY CLUSTERED ([entity_id]),

    CONSTRAINT [AK_guacamole_entity_name_scope]
        UNIQUE ([type], [name])

);
GO

--
-- Table of user groups. Each user group may have an arbitrary set of member
-- users and member groups, with those members inheriting the permissions
-- granted to that group.
--

CREATE TABLE [guacamole_user_group] (

    [user_group_id] [int] IDENTITY(1,1) NOT NULL,
    [entity_id]     [int]               NOT NULL,

    -- Group disabled status
    [disabled] [bit] NOT NULL DEFAULT 0,

    CONSTRAINT [PK_guacamole_user_group]
        PRIMARY KEY CLUSTERED ([user_group_id]),

    CONSTRAINT [guacamole_user_group_single_entity]
        UNIQUE ([entity_id]),

    CONSTRAINT [guacamole_user_group_entity]
        FOREIGN KEY ([entity_id])
        REFERENCES [guacamole_entity] ([entity_id])
        ON DELETE CASCADE

);
GO

--
-- Table of users which are members of given user groups.
--

CREATE TABLE [guacamole_user_group_member] (

    [user_group_id]    [int] NOT NULL,
    [member_entity_id] [int] NOT NULL,

    CONSTRAINT [PK_guacamole_user_group_member]
        PRIMARY KEY CLUSTERED ([user_group_id], [member_entity_id]),

    -- Parent must be a user group
    CONSTRAINT [guacamole_user_group_member_parent_id]
        FOREIGN KEY ([user_group_id])
        REFERENCES [guacamole_user_group] ([user_group_id])
        ON DELETE CASCADE,

    -- Member may be either a user or a user group (any entity)
    CONSTRAINT [guacamole_user_group_member_entity_id]
        FOREIGN KEY ([member_entity_id])
        REFERENCES [guacamole_entity] ([entity_id])
        -- ON DELETE CASCADE handled by guacamole_delete_entity trigger

);
GO

--
-- Table of user group permissions. Each user group permission grants a user
-- or user group access to a another user group (the "affected" user group) for
-- a specific type of operation.
--

CREATE TABLE [guacamole_user_group_permission] (

    [entity_id]              [int]                         NOT NULL,
    [affected_user_group_id] [int]                         NOT NULL,
    [permission]             [guacamole_object_permission] NOT NULL,

    CONSTRAINT [PK_guacamole_user_group_permission]
        PRIMARY KEY CLUSTERED ([entity_id], [affected_user_group_id], [permission]),

    CONSTRAINT [FK_guacamole_user_group_permission_affected_user_group_id]
        FOREIGN KEY ([affected_user_group_id])
        REFERENCES [guacamole_user_group] ([user_group_id])
        ON DELETE CASCADE,

    CONSTRAINT [FK_guacamole_user_group_permission_entity_id]
        FOREIGN KEY ([entity_id])
        REFERENCES [guacamole_entity] ([entity_id])
        -- ON DELETE CASCADE handled by guacamole_delete_entity trigger

);

CREATE NONCLUSTERED INDEX [IX_guacamole_user_group_permission_entity_id]
    ON [guacamole_user_group_permission] ([entity_id]);

CREATE NONCLUSTERED INDEX [IX_guacamole_user_group_permission_affected_user_group_id]
    ON [guacamole_user_group_permission] ([affected_user_group_id]);
GO

--
-- The guacamole_delete_entity trigger effectively replaces the
-- guacamole_delete_user trigger, which is no longer necessary and will cease
-- being correct after the columns of existing tables are updated.
--

DROP TRIGGER [guacamole_delete_user];
GO

--
-- Modify guacamole_user table to use guacamole_entity as a base
--

-- Add new entity_id column
ALTER TABLE [guacamole_user] ADD [entity_id] [int];
GO

-- Create user entities for each guacamole_user entry
INSERT INTO [guacamole_entity] ([name], [type])
SELECT [username], 'USER' FROM [guacamole_user];
GO

-- Update guacamole_user to point to corresponding guacamole_entity
UPDATE [guacamole_user] SET [entity_id] = (
    SELECT [entity_id] FROM [guacamole_entity]
    WHERE
            [username] = [guacamole_entity].[name]
        AND type = 'USER'
);
GO

-- The entity_id column should now be safely non-NULL
ALTER TABLE [guacamole_user]
    ALTER COLUMN [entity_id] [int] NOT NULL;

-- The entity_id column should now be unique for each user
ALTER TABLE [guacamole_user]
    ADD CONSTRAINT [AK_guacamole_user_single_entity]
    UNIQUE ([entity_id]);

-- The entity_id column should now safely point to guacamole_entity entries
ALTER TABLE [guacamole_user]
    ADD CONSTRAINT [FK_guacamole_user_entity]
    FOREIGN KEY ([entity_id])
    REFERENCES [guacamole_entity] ([entity_id])
    ON DELETE CASCADE;

-- The username column can now safely be removed
ALTER TABLE [guacamole_user] DROP [AK_guacamole_user_username];
ALTER TABLE [guacamole_user] DROP COLUMN [username];
GO

--
-- Modify guacamole_connection_permission to use guacamole_entity instead of
-- guacamole_user
--

-- Add new entity_id column
ALTER TABLE [guacamole_connection_permission] ADD [entity_id] [int];
GO

-- Update guacamole_connection_permission to point to the guacamole_entity
-- that has been granted the permission
UPDATE [guacamole_connection_permission] SET [entity_id] = (
    SELECT [entity_id] FROM [guacamole_user]
    WHERE [guacamole_user].[user_id] = [guacamole_connection_permission].[user_id]
);
GO

-- The entity_id column should now be safely non-NULL
ALTER TABLE [guacamole_connection_permission]
    ALTER COLUMN [entity_id] [int] NOT NULL;

-- Remove user_id column
DROP INDEX [IX_guacamole_connection_permission_user_id] ON [guacamole_connection_permission];
ALTER TABLE [guacamole_connection_permission] DROP [PK_guacamole_connection_permission];
ALTER TABLE [guacamole_connection_permission] DROP [FK_guacamole_connection_permission_user_id];
ALTER TABLE [guacamole_connection_permission] DROP COLUMN [user_id];

-- The entity_id column should now safely point to guacamole_entity entries
ALTER TABLE [guacamole_connection_permission]
    ADD CONSTRAINT [FK_guacamole_connection_permission_entity_id]
    FOREIGN KEY ([entity_id])
    REFERENCES [guacamole_entity] ([entity_id])
    ON DELETE CASCADE;

CREATE NONCLUSTERED INDEX [IX_guacamole_connection_permission_entity_id]
    ON [guacamole_connection_permission] ([entity_id]);

-- Add new primary key which uses entity_id
ALTER TABLE [guacamole_connection_permission]
    ADD CONSTRAINT [PK_guacamole_connection_permission]
    PRIMARY KEY CLUSTERED ([entity_id], [connection_id], [permission]);
GO

--
-- Modify guacamole_connection_group_permission to use guacamole_entity instead
-- of guacamole_user
--

-- Add new entity_id column
ALTER TABLE [guacamole_connection_group_permission] ADD [entity_id] [int];
GO

-- Update guacamole_connection_group_permission to point to the guacamole_entity
-- that has been granted the permission
UPDATE guacamole_connection_group_permission SET entity_id = (
    SELECT entity_id FROM guacamole_user
    WHERE guacamole_user.user_id = guacamole_connection_group_permission.user_id
);
GO

-- The entity_id column should now be safely non-NULL
ALTER TABLE [guacamole_connection_group_permission]
    ALTER COLUMN [entity_id] [int] NOT NULL;

-- Remove user_id column
DROP INDEX [IX_guacamole_connection_group_permission_user_id] ON [guacamole_connection_group_permission];
ALTER TABLE [guacamole_connection_group_permission] DROP [PK_guacamole_connection_group_permission];
ALTER TABLE [guacamole_connection_group_permission] DROP [FK_guacamole_connection_group_permission_user_id];
ALTER TABLE [guacamole_connection_group_permission] DROP COLUMN user_id;

-- The entity_id column should now safely point to guacamole_entity entries
ALTER TABLE [guacamole_connection_group_permission]
    ADD CONSTRAINT [FK_guacamole_connection_group_permission_entity_id]
    FOREIGN KEY ([entity_id])
    REFERENCES [guacamole_entity] ([entity_id])
    ON DELETE CASCADE;

CREATE NONCLUSTERED INDEX [IX_guacamole_connection_group_permission_entity_id]
    ON [guacamole_connection_group_permission] ([entity_id]);

-- Add new primary key which uses entity_id
ALTER TABLE [guacamole_connection_group_permission]
    ADD CONSTRAINT [PK_guacamole_connection_group_permission]
    PRIMARY KEY CLUSTERED ([entity_id], [connection_group_id], [permission]);
GO

--
-- Modify guacamole_sharing_profile_permission to use guacamole_entity instead
-- of guacamole_user
--

-- Add new entity_id column
ALTER TABLE [guacamole_sharing_profile_permission] ADD [entity_id] [int];
GO

-- Update guacamole_sharing_profile_permission to point to the guacamole_entity
-- that has been granted the permission
UPDATE guacamole_sharing_profile_permission SET entity_id = (
    SELECT entity_id FROM guacamole_user
    WHERE guacamole_user.user_id = guacamole_sharing_profile_permission.user_id
);
GO

-- The entity_id column should now be safely non-NULL
ALTER TABLE [guacamole_sharing_profile_permission]
    ALTER COLUMN [entity_id] [int] NOT NULL;

-- Remove user_id column
DROP INDEX [IX_guacamole_sharing_profile_permission_user_id] ON [guacamole_sharing_profile_permission];
ALTER TABLE [guacamole_sharing_profile_permission] DROP [PK_guacamole_sharing_profile_permission];
ALTER TABLE [guacamole_sharing_profile_permission] DROP [FK_guacamole_sharing_profile_permission_user_id];
ALTER TABLE [guacamole_sharing_profile_permission] DROP COLUMN user_id;

-- The entity_id column should now safely point to guacamole_entity entries
ALTER TABLE [guacamole_sharing_profile_permission]
    ADD CONSTRAINT [FK_guacamole_sharing_profile_permission_entity_id]
    FOREIGN KEY ([entity_id])
    REFERENCES [guacamole_entity] ([entity_id])
    ON DELETE CASCADE;

CREATE NONCLUSTERED INDEX [IX_guacamole_sharing_profile_permission_entity_id]
    ON [guacamole_sharing_profile_permission] ([entity_id]);

-- Add new primary key which uses entity_id
ALTER TABLE [guacamole_sharing_profile_permission]
    ADD CONSTRAINT [PK_guacamole_sharing_profile_permission]
    PRIMARY KEY CLUSTERED ([entity_id], [sharing_profile_id], [permission]);
GO

--
-- Modify guacamole_user_permission to use guacamole_entity instead of
-- guacamole_user
--

-- Add new entity_id column
ALTER TABLE [guacamole_user_permission] ADD [entity_id] [int];
GO

-- Update guacamole_user_permission to point to the guacamole_entity
-- that has been granted the permission
UPDATE guacamole_user_permission SET entity_id = (
    SELECT entity_id FROM guacamole_user
    WHERE guacamole_user.user_id = guacamole_user_permission.user_id
);
GO

-- The entity_id column should now be safely non-NULL
ALTER TABLE [guacamole_user_permission]
    ALTER COLUMN [entity_id] [int] NOT NULL;

-- The entity_id column should now safely point to guacamole_entity entries
ALTER TABLE [guacamole_user_permission]
    ADD CONSTRAINT [FK_guacamole_user_permission_entity_id]
    FOREIGN KEY ([entity_id])
    REFERENCES [guacamole_entity] ([entity_id]);
    -- ON DELETE CASCADE handled by guacamole_delete_entity trigger

-- The affected_user_id column now has ON DELETE CASCADE
ALTER TABLE [guacamole_user_permission] DROP [FK_guacamole_user_permission_affected_user_id];
ALTER TABLE [guacamole_user_permission]
    ADD CONSTRAINT [FK_guacamole_user_permission_affected_user_id]
        FOREIGN KEY ([affected_user_id])
        REFERENCES [guacamole_user] ([user_id])
        ON DELETE CASCADE;

CREATE NONCLUSTERED INDEX [IX_guacamole_user_permission_entity_id]
    ON [guacamole_user_permission] ([entity_id]);

-- Remove user_id column
DROP INDEX [IX_guacamole_user_permission_user_id] ON [guacamole_user_permission];
ALTER TABLE [guacamole_user_permission] DROP [PK_guacamole_user_permission];
ALTER TABLE [guacamole_user_permission] DROP [FK_guacamole_user_permission_user_id];
ALTER TABLE [guacamole_user_permission] DROP COLUMN user_id;

-- Add new primary key which uses entity_id
ALTER TABLE [guacamole_user_permission]
    ADD CONSTRAINT [PK_guacamole_user_permission]
    PRIMARY KEY CLUSTERED ([entity_id], [affected_user_id], [permission]);
GO

--
-- Modify guacamole_system_permission to use guacamole_entity instead of
-- guacamole_user
--

-- Add new entity_id column
ALTER TABLE [guacamole_system_permission] ADD [entity_id] [int];
GO

-- Update guacamole_system_permission to point to the guacamole_entity
-- that has been granted the permission
UPDATE [guacamole_system_permission] SET [entity_id] = (
    SELECT [entity_id] FROM [guacamole_user]
    WHERE [guacamole_user].[user_id] = [guacamole_system_permission].[user_id]
);
GO

-- The entity_id column should now be safely non-NULL
ALTER TABLE [guacamole_system_permission]
    ALTER COLUMN [entity_id] [int] NOT NULL;

-- Remove user_id column
DROP INDEX [IX_guacamole_system_permission_user_id] ON [guacamole_system_permission];
ALTER TABLE [guacamole_system_permission] DROP [PK_guacamole_system_permission];
ALTER TABLE [guacamole_system_permission] DROP [FK_guacamole_system_permission_user_id];
ALTER TABLE [guacamole_system_permission] DROP COLUMN [user_id];

-- The entity_id column should now safely point to guacamole_entity entries
ALTER TABLE [guacamole_system_permission]
    ADD CONSTRAINT [FK_guacamole_system_permission_entity_id]
    FOREIGN KEY ([entity_id])
    REFERENCES [guacamole_entity] ([entity_id])
    ON DELETE CASCADE;

CREATE NONCLUSTERED INDEX [IX_guacamole_system_permission_entity_id]
    ON [guacamole_system_permission] ([entity_id]);

-- Add new primary key which uses entity_id
ALTER TABLE [guacamole_system_permission]
    ADD CONSTRAINT [PK_guacamole_system_permission]
    PRIMARY KEY CLUSTERED ([entity_id], [permission]);
GO

--
-- Handle cascading deletion/updates of records in response to deletion of
-- guacamole_entity records, where such deletion is not already covered by
-- ON DELETE CASCADE or ON DELETE SET NULL.
--

CREATE TRIGGER [guacamole_delete_entity]
   ON [guacamole_entity]
   INSTEAD OF DELETE
AS BEGIN

    -- Do not take trigger into account when producing row counts for the DELETE
    SET NOCOUNT ON;

    -- Delete all associated permissions not covered by ON DELETE CASCADE
    DELETE FROM [guacamole_user_permission]
    WHERE [entity_id] IN (SELECT [entity_id] FROM DELETED);

    DELETE FROM [guacamole_user_group_permission]
    WHERE [entity_id] IN (SELECT [entity_id] FROM DELETED);

    -- Delete all associated group memberships not covered by ON DELETE CASCADE
    DELETE FROM [guacamole_user_group_member]
    WHERE [member_entity_id] IN (SELECT [entity_id] FROM DELETED);

    -- Perform original deletion
    DELETE FROM [guacamole_entity]
    WHERE [entity_id] IN (SELECT [entity_id] FROM DELETED);

END
GO

--
-- Update guacamole_delete_connection_group trigger to remove descendant
-- connections first.
--

DROP TRIGGER [guacamole_delete_connection_group];
GO

CREATE TRIGGER [guacamole_delete_connection_group]
   ON [guacamole_connection_group]
   INSTEAD OF DELETE
AS BEGIN

    -- Do not take trigger into account when producing row counts for the DELETE
    SET NOCOUNT ON;

    -- Delete all descendant connections
    WITH [connection_groups] ([connection_group_id]) AS (
        SELECT [connection_group_id] FROM DELETED
    UNION ALL
        SELECT [guacamole_connection_group].[connection_group_id]
        FROM [guacamole_connection_group]
        JOIN [connection_groups] ON [connection_groups].[connection_group_id] = [guacamole_connection_group].[parent_id]
    )
    DELETE FROM [guacamole_connection]
    WHERE [parent_id] IN (
        SELECT [connection_group_id]
        FROM [connection_groups]
    );

    -- Delete all requested connection groups, including descendants
    WITH [connection_groups] ([connection_group_id]) AS (
        SELECT [connection_group_id] FROM DELETED
    UNION ALL
        SELECT [guacamole_connection_group].[connection_group_id]
        FROM [guacamole_connection_group]
        JOIN [connection_groups] ON [connection_groups].[connection_group_id] = [guacamole_connection_group].[parent_id]
    )
    DELETE FROM [guacamole_connection_group]
    WHERE [connection_group_id] IN (
        SELECT [connection_group_id]
        FROM [connection_groups]
    );

END
GO

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
-- Table of arbitrary user group attributes. Each attribute is simply a
-- name/value pair associated with a user group. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE [guacamole_user_group_attribute] (

    [user_group_id]   [int]            NOT NULL,
    [attribute_name]  [nvarchar](128)  NOT NULL,
    [attribute_value] [nvarchar](4000) NOT NULL,

    CONSTRAINT [PK_guacamole_user_group_attribute]
        PRIMARY KEY CLUSTERED ([user_group_id], [attribute_name]),

    CONSTRAINT [FK_guacamole_user_attribute_user_group_id]
        FOREIGN KEY ([user_group_id])
        REFERENCES [guacamole_user_group] ([user_group_id])
        ON DELETE CASCADE

);

CREATE NONCLUSTERED INDEX [IX_guacamole_user_group_attribute_user_id]
    ON [guacamole_user_group_attribute] ([user_group_id])
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
