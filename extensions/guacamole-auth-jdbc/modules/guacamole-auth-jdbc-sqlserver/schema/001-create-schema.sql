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

/**
 * List for permission data type.
 */
CREATE RULE [guacamole_permission_list] 
    AS
    @list IN ('READ','UPDATE','DELETE','ADMINISTER');
GO;

/**
 * List for system permission data type.
 */
CREATE RULE [guacamole_system_permission_list] 
    AS
    @list IN ('CREATE_CONNECTION',
        'CREATE_CONNECTION_GROUP',
        'CREATE_SHARING_PROFILE',
        'CREATE_USER',
        'ADMINISTER');
GO;

/**
 * The permission data type.
 */
CREATE TYPE [guacamole_permission] FROM [nvarchar](10) NOT NULL;
EXEC sp_bindrule 'guacamole_permission_list','guacamole_permission';

/**
 * The system permission data type.
 */
CREATE TYPE [guacamole_system_permission] FROM [nvarchar](32) NOT NULL;
EXEC sp_bindrule 'guacamole_system_permission_list','guacamole_system_permission';
GO;

/**
 * The connection_group table stores organizational and balancing groups.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_connection_group](
    [connection_group_id] [int] IDENTITY(1,1) NOT NULL,
    [parent_id] [int] NULL,
    [connection_group_name] [nvarchar](128) NOT NULL,
    [type] [nvarchar](32) NOT NULL,
    [max_connections] [int] NULL,
    [max_connections_per_user] [int] NULL,
    [enable_session_affinity] [bit] NOT NULL,

    CONSTRAINT [PK_guacmaole_connection_group] PRIMARY KEY CLUSTERED
        ([connection_group_id] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
       ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for connection_group table.
 */
ALTER TABLE [guacamole_connection_group]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_group_connection_group_id] FOREIGN KEY([parent_id])
    REFERENCES [guacamole_connection_group] ([connection_group_id]);
ALTER TABLE [guacamole_connection_group]
    CHECK CONSTRAINT [FK_guacamole_connection_group_connection_group_id];
ALTER TABLE [guacamole_connection_group]
    WITH CHECK ADD CONSTRAINT [CK_guacamole_connection_group_type] 
    CHECK (([type]='BALANCING' OR [type]='ORGANIZATIONAL'));
ALTER TABLE [guacamole_connection_group]
    CHECK CONSTRAINT [CK_guacamole_connection_group_type];

/**
 * Default values for connection_group table.
 */
ALTER TABLE [guacamole_connection_group]
    ADD CONSTRAINT [DF_guacamole_connection_group_type] DEFAULT (N'ORGANIZATIONAL') FOR [type];
ALTER TABLE [guacamole_connection_group]
    ADD CONSTRAINT [DF_guacamole_connection_group_enable_session_affinity] DEFAULT ((0)) FOR [enable_session_affinity];
GO;

/**
 * The connection table, for storing connections and attributes.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_connection](
    [connection_id] [int] IDENTITY(1,1) NOT NULL,
    [connection_name] [nvarchar](128) NOT NULL,
    [parent_id] [int] NULL,
    [protocol] [nvarchar](32) NOT NULL,
    [proxy_port] [int] NULL,
    [proxy_hostname] [nvarchar](512) NULL,
    [proxy_encryption_method] [nvarchar](4) NULL,
    [max_connections] [int] NULL,
    [max_connections_per_user] [int] NULL,
    [connection_weight] [int] NULL,
    [failover_only] [bit] NOT NULL,

    CONSTRAINT [PK_guacamole_connection] PRIMARY KEY CLUSTERED
	([connection_id] ASC)
        WITH (PAD_INDEX = OFF, 
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

ALTER TABLE [guacamole_connection]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_connection_group] FOREIGN KEY([parent_id])
    REFERENCES [guacamole_connection_group] ([connection_group_id]);
ALTER TABLE [guacamole_connection]
    CHECK CONSTRAINT [FK_guacamole_connection_connection_group];
ALTER TABLE [guacamole_connection]
    WITH CHECK ADD CONSTRAINT [CK_proxy_encryption_method]
    CHECK  (([proxy_encryption_method]='SSL' OR [proxy_encryption_method]='NONE'));
ALTER TABLE [guacamole_connection]
    CHECK CONSTRAINT [CK_proxy_encryption_method];
ALTER TABLE [guacamole_connection]
    ADD CONSTRAINT [DF_guacamole_connection_failover_only] DEFAULT ((0)) FOR [failover_only];
GO;

/**
 * The user table stores user accounts, passwords, and properties.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_user](
    [user_id] [int] IDENTITY(1,1) NOT NULL,
    [username] [nvarchar](128) NOT NULL,
    [password_hash] [binary](32) NOT NULL,
    [password_salt] [binary](32) NULL,
    [password_date] [datetime] NOT NULL,
    [disabled] [bit] NOT NULL,
    [expired] [bit] NOT NULL,
    [access_window_start] [time](7) NULL,
    [access_window_end] [time](7) NULL,
    [valid_from] [date] NULL,
    [valid_until] [date] NULL,
    [timezone] [nvarchar](64) NULL,
    [full_name] [nvarchar](256) NULL,
    [email_address] [nvarchar](256) NULL,
    [organization] [nvarchar](256) NULL,
    [organizational_role] [nvarchar](256) NULL,

    CONSTRAINT [PK_guacamole_user] PRIMARY KEY CLUSTERED 
        ([user_id] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Defaults for user table
 */
ALTER TABLE [guacamole_user]
    ADD CONSTRAINT [DF_guacamole_user_disabled] DEFAULT ((0)) FOR [disabled];
ALTER TABLE [guacamole_user]
    ADD CONSTRAINT [DF_guacamole_user_expired] DEFAULT ((0)) FOR [expired];
GO;

/**
 * The sharing_profile table stores profiles that allow
 * connections to be shared amongst multiple users.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_sharing_profile](
    [sharing_profile_id] [int] IDENTITY(1,1) NOT NULL,
    [sharing_profile_name] [nvarchar](128) NOT NULL,
    [primary_connection_id] [int] NOT NULL,

    CONSTRAINT [PK_guacamole_sharing_profile] PRIMARY KEY CLUSTERED 
        ([sharing_profile_id] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for sharing_profile table.
 */
ALTER TABLE [guacamole_sharing_profile]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_sharing_profile_connection] FOREIGN KEY([primary_connection_id])
    REFERENCES [guacamole_connection] ([connection_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_sharing_profile]
    CHECK CONSTRAINT [FK_guacamole_sharing_profile_connection];
GO;

/**
 * The connection_parameter table stores parameters for
 * connection objects.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_connection_parameter](
    [connection_id] [int] NOT NULL,
    [parameter_name] [nvarchar](128) NOT NULL,
    [parameter_value] [nvarchar](max) NOT NULL,

    CONSTRAINT [PK_guacamole_connection_parameter] PRIMARY KEY CLUSTERED 
        ([connection_id] ASC, [parameter_name] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY];

/**
 * Foreign keys for the connection_parameter table.
 */
ALTER TABLE [guacamole_connection_parameter]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_parameter_connection] FOREIGN KEY([connection_id])
    REFERENCES [guacamole_connection] ([connection_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_connection_parameter]
    CHECK CONSTRAINT [FK_guacamole_connection_parameter_connection];
GO;

/**
 * The sharing_profile_parameter table stores parameters
 * for sharing_profile objects.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_sharing_profile_parameter](
    [sharing_profile_id] [int] NOT NULL,
    [parameter_name] [nvarchar](128) NOT NULL,
    [parameter_value] [nvarchar](max) NOT NULL,

    CONSTRAINT [PK_guacamole_sharing_profile_parameter] PRIMARY KEY CLUSTERED 
        ([sharing_profile_id] ASC, [parameter_name] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY];

/**
 * Foreign keys for the sharing_profile_parameter
 * table.
 */
ALTER TABLE [guacamole_sharing_profile_parameter]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_sharing_profile_parameter_sharing_profile] FOREIGN KEY([sharing_profile_id])
    REFERENCES [guacamole_sharing_profile] ([sharing_profile_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_sharing_profile_parameter]
    CHECK CONSTRAINT [FK_guacamole_sharing_profile_parameter_sharing_profile];
GO;

/**
 * The connection_permission table stores permission
 * mappings for connection objects.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_connection_permission](
    [user_id] [int] NOT NULL,
    [connection_id] [int] NOT NULL,
    [permission] [guacamole_permission] NOT NULL,

    CONSTRAINT [PK_guacamole_connection_permission] PRIMARY KEY CLUSTERED 
        ([user_id] ASC, [connection_id] ASC, [permission] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for the connection_permission table.
 */
ALTER TABLE [guacamole_connection_permission]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_permission_connection1] FOREIGN KEY([connection_id])
    REFERENCES [guacamole_connection] ([connection_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_connection_permission]
    CHECK CONSTRAINT [FK_guacamole_connection_permission_connection1];
ALTER TABLE [guacamole_connection_permission]
    WITH CHECK ADD  CONSTRAINT [FK_guacamole_connection_permission_user1] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_connection_permission]
    CHECK CONSTRAINT [FK_guacamole_connection_permission_user1];
GO;

/**
 * The connection_group_permission table stores permission mappings for
 * connection_group objects.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_connection_group_permission](
    [user_id] [int] NOT NULL,
    [connection_group_id] [int] NOT NULL,
    [permission] [guacamole_permission] NOT NULL,

    CONSTRAINT [PK_guacamole_connection_group_permission] PRIMARY KEY CLUSTERED 
        ([user_id] ASC,	[connection_group_id] ASC, [permission] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON) 
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for the connection_group_permission table.
 */
ALTER TABLE [guacamole_connection_group_permission] 
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_group_permission_connection_group] FOREIGN KEY([connection_group_id])
    REFERENCES [guacamole_connection_group] ([connection_group_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_connection_group_permission]
    CHECK CONSTRAINT [FK_guacamole_connection_group_permission_connection_group];
ALTER TABLE [guacamole_connection_group_permission]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_group_permission_user] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_connection_group_permission]
    CHECK CONSTRAINT [FK_guacamole_connection_group_permission_user];
GO;

/**
 * The sharing_profile_permission table stores permission
 * mappings for sharing_profile objects.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_sharing_profile_permission](
    [user_id] [int] NOT NULL,
    [sharing_profile_id] [int] NOT NULL,
    [permission] [guacamole_permission] NOT NULL,

    CONSTRAINT [PK_guacamole_sharing_profile_permission] PRIMARY KEY CLUSTERED 
        ([user_id] ASC, [sharing_profile_id] ASC, [permission] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for the sharing_profile_permission table.
 */
ALTER TABLE [guacamole_sharing_profile_permission]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_sharing_profile_permission_sharing_profile] FOREIGN KEY([sharing_profile_id])
    REFERENCES [guacamole_sharing_profile] ([sharing_profile_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_sharing_profile_permission]
    CHECK CONSTRAINT [FK_guacamole_sharing_profile_permission_sharing_profile];
ALTER TABLE [guacamole_sharing_profile_permission]
    WITH CHECK ADD  CONSTRAINT [FK_guacamole_sharing_profile_permission_user] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_sharing_profile_permission]
    CHECK CONSTRAINT [FK_guacamole_sharing_profile_permission_user];
GO;

/**
 * The system_permission table stores permission mappings
 * for system-level operations.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_system_permission](
    [user_id] [int] NOT NULL,
    [permission] [guacamole_system_permission] NOT NULL,

    CONSTRAINT [PK_guacamole_system_permission] PRIMARY KEY CLUSTERED 
        ([user_id] ASC,	[permission] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for system_permission table.
 */
ALTER TABLE [guacamole_system_permission]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_system_permission_user] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_system_permission]
    CHECK CONSTRAINT [FK_guacamole_system_permission_user];
GO;

/**
 * The user_permission table stores permission mappings
 * for users to other users.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_user_permission](
    [user_id] [int] NOT NULL,
    [affected_user_id] [int] NOT NULL,
    [permission] [guacamole_permission] NOT NULL,

    CONSTRAINT [PK_guacamole_user_permission] PRIMARY KEY CLUSTERED 
        ([user_id] ASC,	[affected_user_id] ASC,	[permission] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for user_permission table.
 */
ALTER TABLE [guacamole_user_permission]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_user_permission_user] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_user_permission]
    CHECK CONSTRAINT [FK_guacamole_user_permission_user];
ALTER TABLE [guacamole_user_permission]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_user_permission_user1] FOREIGN KEY([affected_user_id])
    REFERENCES [guacamole_user] ([user_id]);
ALTER TABLE [guacamole_user_permission]
    CHECK CONSTRAINT [FK_guacamole_user_permission_user1];
GO;

/**
 * The connection_history table stores records for historical
 * connections.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_connection_history](
    [history_id] [int] IDENTITY(1,1) NOT NULL,
    [user_id] [int] NULL,
    [username] [nvarchar](128) NOT NULL,
    [remote_host] [nvarchar](256) NULL,
    [connection_id] [int] NULL,
    [connection_name] [nvarchar](128) NOT NULL,
    [sharing_profile_id] [int] NULL,
    [sharing_profile_name] [nvarchar](128) NULL,
    [start_date] [datetime] NOT NULL,
    [end_date] [datetime] NULL,

    CONSTRAINT [PK_guacamole_connection_history] PRIMARY KEY CLUSTERED 
        ([history_id] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for connection_history table
 */
ALTER TABLE [guacamole_connection_history]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_history_connection] FOREIGN KEY([connection_id])
    REFERENCES [guacamole_connection] ([connection_id])
        ON UPDATE CASCADE
        ON DELETE SET NULL;
ALTER TABLE [guacamole_connection_history]
    CHECK CONSTRAINT [FK_guacamole_connection_history_connection];
ALTER TABLE [guacamole_connection_history]
    WITH CHECK ADD  CONSTRAINT [FK_guacamole_connection_history_sharing_profile] FOREIGN KEY([sharing_profile_id])
    REFERENCES [guacamole_sharing_profile] ([sharing_profile_id]);
ALTER TABLE [guacamole_connection_history]
    CHECK CONSTRAINT [FK_guacamole_connection_history_sharing_profile];
ALTER TABLE [guacamole_connection_history]
    WITH CHECK ADD CONSTRAINT [FK_guacamole_connection_history_user] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE SET NULL;
ALTER TABLE [guacamole_connection_history]
    CHECK CONSTRAINT [FK_guacamole_connection_history_user];
GO;

/**
 * The user_password_history table stores password history
 * for users, allowing for enforcing rules associated with
 * reuse of passwords.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
CREATE TABLE [guacamole_user_password_history](
    [password_history_id] [int] IDENTITY(1,1) NOT NULL,
    [user_id] [int] NOT NULL,
    [password_hash] [binary](32) NOT NULL,
    [password_salt] [binary](32) NULL,
    [password_date] [datetime] NOT NULL,

    CONSTRAINT [PK_guacamole_user_password_history] PRIMARY KEY CLUSTERED 
        ([password_history_id] ASC)
        WITH (PAD_INDEX = OFF,
            STATISTICS_NORECOMPUTE = OFF,
            IGNORE_DUP_KEY = OFF,
            ALLOW_ROW_LOCKS = ON,
            ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
) ON [PRIMARY];

/**
 * Foreign keys for user_password_history table
 */
ALTER TABLE [guacamole_user_password_history]
    WITH CHECK ADD  CONSTRAINT [FK_guacamole_user_password_history_user] FOREIGN KEY([user_id])
    REFERENCES [guacamole_user] ([user_id])
        ON UPDATE CASCADE
        ON DELETE CASCADE;
ALTER TABLE [guacamole_user_password_history]
    CHECK CONSTRAINT [FK_guacamole_user_password_history_user];
GO;
