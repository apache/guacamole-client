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
-- Connection group types
--

CREATE TYPE guacamole_connection_group_type AS ENUM(
    'ORGANIZATIONAL',
    'BALANCING'
);

--
-- Object permission types
--

CREATE TYPE guacamole_object_permission_type AS ENUM(
    'READ',
    'UPDATE',
    'DELETE',
    'ADMINISTER'
);

--
-- System permission types
--

CREATE TYPE guacamole_system_permission_type AS ENUM(
    'CREATE_CONNECTION',
    'CREATE_CONNECTION_GROUP',
    'CREATE_SHARING_PROFILE',
    'CREATE_USER',
    'ADMINISTER'
);

--
-- Guacamole proxy (guacd) encryption methods
--

CREATE TYPE guacamole_proxy_encryption_method AS ENUM(
    'NONE',
    'SSL'
);

--
-- Table of connection groups. Each connection group has a name.
--

CREATE TABLE guacamole_connection_group (

  connection_group_id   serial       NOT NULL,
  parent_id             integer,
  connection_group_name varchar(128) NOT NULL,
  type                  guacamole_connection_group_type
                        NOT NULL DEFAULT 'ORGANIZATIONAL',

  -- Concurrency limits
  max_connections          integer,
  max_connections_per_user integer,
  enable_session_affinity  boolean NOT NULL DEFAULT FALSE,

  PRIMARY KEY (connection_group_id),

  CONSTRAINT connection_group_name_parent
    UNIQUE (connection_group_name, parent_id),

  CONSTRAINT guacamole_connection_group_ibfk_1
    FOREIGN KEY (parent_id)
    REFERENCES guacamole_connection_group (connection_group_id)
    ON DELETE CASCADE

);

CREATE INDEX guacamole_connection_group_parent_id
    ON guacamole_connection_group(parent_id);

--
-- Table of connections. Each connection has a name, protocol, and
-- associated set of parameters.
-- A connection may belong to a connection group.
--

CREATE TABLE guacamole_connection (

  connection_id       serial       NOT NULL,
  connection_name     varchar(128) NOT NULL,
  parent_id           integer,
  protocol            varchar(32)  NOT NULL,
  
  -- Concurrency limits
  max_connections          integer,
  max_connections_per_user integer,

  -- Connection Weight
  connection_weight        integer,
  failover_only            boolean NOT NULL DEFAULT FALSE,

  -- Guacamole proxy (guacd) overrides
  proxy_port              integer,
  proxy_hostname          varchar(512),
  proxy_encryption_method guacamole_proxy_encryption_method,

  PRIMARY KEY (connection_id),

  CONSTRAINT connection_name_parent
    UNIQUE (connection_name, parent_id),

  CONSTRAINT guacamole_connection_ibfk_1
    FOREIGN KEY (parent_id)
    REFERENCES guacamole_connection_group (connection_group_id)
    ON DELETE CASCADE

);

CREATE INDEX guacamole_connection_parent_id
    ON guacamole_connection(parent_id);

--
-- Table of users. Each user has a unique username and a hashed password
-- with corresponding salt. Although the authentication system will always set
-- salted passwords, other systems may set unsalted passwords by simply not
-- providing the salt.
--

CREATE TABLE guacamole_user (

  user_id       serial       NOT NULL,

  -- Username and optionally-salted password
  username      varchar(128) NOT NULL,
  password_hash bytea        NOT NULL,
  password_salt bytea,
  password_date timestamptz  NOT NULL,

  -- Account disabled/expired status
  disabled      boolean      NOT NULL DEFAULT FALSE,
  expired       boolean      NOT NULL DEFAULT FALSE,

  -- Time-based access restriction
  access_window_start    time,
  access_window_end      time,

  -- Date-based access restriction
  valid_from  date,
  valid_until date,

  -- Timezone used for all date/time comparisons and interpretation
  timezone varchar(64),

  -- Profile information
  full_name           varchar(256),
  email_address       varchar(256),
  organization        varchar(256),
  organizational_role varchar(256),

  PRIMARY KEY (user_id),

  CONSTRAINT username
    UNIQUE (username)

);

--
-- Table of sharing profiles. Each sharing profile has a name, associated set
-- of parameters, and a primary connection. The primary connection is the
-- connection that the sharing profile shares, and the parameters dictate the
-- restrictions/features which apply to the user joining the connection via the
-- sharing profile.
--

CREATE TABLE guacamole_sharing_profile (

  sharing_profile_id    serial       NOT NULL,
  sharing_profile_name  varchar(128) NOT NULL,
  primary_connection_id integer      NOT NULL,

  PRIMARY KEY (sharing_profile_id),

  CONSTRAINT sharing_profile_name_primary
    UNIQUE (sharing_profile_name, primary_connection_id),

  CONSTRAINT guacamole_sharing_profile_ibfk_1
    FOREIGN KEY (primary_connection_id)
    REFERENCES guacamole_connection (connection_id)
    ON DELETE CASCADE

);

CREATE INDEX guacamole_sharing_profile_primary_connection_id
    ON guacamole_sharing_profile(primary_connection_id);

--
-- Table of connection parameters. Each parameter is simply a name/value pair
-- associated with a connection.
--

CREATE TABLE guacamole_connection_parameter (

  connection_id   integer       NOT NULL,
  parameter_name  varchar(128)  NOT NULL,
  parameter_value varchar(4096) NOT NULL,

  PRIMARY KEY (connection_id,parameter_name),

  CONSTRAINT guacamole_connection_parameter_ibfk_1
    FOREIGN KEY (connection_id)
    REFERENCES guacamole_connection (connection_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_connection_parameter_connection_id
    ON guacamole_connection_parameter(connection_id);

--
-- Table of sharing profile parameters. Each parameter is simply
-- name/value pair associated with a sharing profile. These parameters dictate
-- the restrictions/features which apply to the user joining the associated
-- connection via the sharing profile.
--

CREATE TABLE guacamole_sharing_profile_parameter (

  sharing_profile_id integer       NOT NULL,
  parameter_name     varchar(128)  NOT NULL,
  parameter_value    varchar(4096) NOT NULL,

  PRIMARY KEY (sharing_profile_id, parameter_name),

  CONSTRAINT guacamole_sharing_profile_parameter_ibfk_1
    FOREIGN KEY (sharing_profile_id)
    REFERENCES guacamole_sharing_profile (sharing_profile_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_sharing_profile_parameter_sharing_profile_id
    ON guacamole_sharing_profile_parameter(sharing_profile_id);

--
-- Table of connection permissions. Each connection permission grants a user
-- specific access to a connection.
--

CREATE TABLE guacamole_connection_permission (

  user_id       integer NOT NULL,
  connection_id integer NOT NULL,
  permission    guacamole_object_permission_type NOT NULL,

  PRIMARY KEY (user_id,connection_id,permission),

  CONSTRAINT guacamole_connection_permission_ibfk_1
    FOREIGN KEY (connection_id)
    REFERENCES guacamole_connection (connection_id) ON DELETE CASCADE,

  CONSTRAINT guacamole_connection_permission_ibfk_2
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_connection_permission_connection_id
    ON guacamole_connection_permission(connection_id);

CREATE INDEX guacamole_connection_permission_user_id
    ON guacamole_connection_permission(user_id);

--
-- Table of connection group permissions. Each group permission grants a user
-- specific access to a connection group.
--

CREATE TABLE guacamole_connection_group_permission (

  user_id             integer NOT NULL,
  connection_group_id integer NOT NULL,
  permission          guacamole_object_permission_type NOT NULL,

  PRIMARY KEY (user_id,connection_group_id,permission),

  CONSTRAINT guacamole_connection_group_permission_ibfk_1
    FOREIGN KEY (connection_group_id)
    REFERENCES guacamole_connection_group (connection_group_id) ON DELETE CASCADE,

  CONSTRAINT guacamole_connection_group_permission_ibfk_2
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_connection_group_permission_connection_group_id
    ON guacamole_connection_group_permission(connection_group_id);

CREATE INDEX guacamole_connection_group_permission_user_id
    ON guacamole_connection_group_permission(user_id);

--
-- Table of sharing profile permissions. Each sharing profile permission grants
-- a user specific access to a sharing profile.
--

CREATE TABLE guacamole_sharing_profile_permission (

  user_id            integer NOT NULL,
  sharing_profile_id integer NOT NULL,
  permission         guacamole_object_permission_type NOT NULL,

  PRIMARY KEY (user_id,sharing_profile_id,permission),

  CONSTRAINT guacamole_sharing_profile_permission_ibfk_1
    FOREIGN KEY (sharing_profile_id)
    REFERENCES guacamole_sharing_profile (sharing_profile_id) ON DELETE CASCADE,

  CONSTRAINT guacamole_sharing_profile_permission_ibfk_2
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_sharing_profile_permission_sharing_profile_id
    ON guacamole_sharing_profile_permission(sharing_profile_id);

CREATE INDEX guacamole_sharing_profile_permission_user_id
    ON guacamole_sharing_profile_permission(user_id);

--
-- Table of system permissions. Each system permission grants a user a
-- system-level privilege of some kind.
--

CREATE TABLE guacamole_system_permission (

  user_id    integer NOT NULL,
  permission guacamole_system_permission_type NOT NULL,

  PRIMARY KEY (user_id,permission),

  CONSTRAINT guacamole_system_permission_ibfk_1
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_system_permission_user_id
    ON guacamole_system_permission(user_id);

--
-- Table of user permissions. Each user permission grants a user access to
-- another user (the "affected" user) for a specific type of operation.
--

CREATE TABLE guacamole_user_permission (

  user_id          integer NOT NULL,
  affected_user_id integer NOT NULL,
  permission       guacamole_object_permission_type NOT NULL,

  PRIMARY KEY (user_id,affected_user_id,permission),

  CONSTRAINT guacamole_user_permission_ibfk_1
    FOREIGN KEY (affected_user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE,

  CONSTRAINT guacamole_user_permission_ibfk_2
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_user_permission_affected_user_id
    ON guacamole_user_permission(affected_user_id);

CREATE INDEX guacamole_user_permission_user_id
    ON guacamole_user_permission(user_id);

--
-- Table of connection history records. Each record defines a specific user's
-- session, including the connection used, the start time, and the end time
-- (if any).
--

CREATE TABLE guacamole_connection_history (

  history_id           serial       NOT NULL,
  user_id              integer      DEFAULT NULL,
  username             varchar(128) NOT NULL,
  remote_host          varchar(256) DEFAULT NULL,
  connection_id        integer      DEFAULT NULL,
  connection_name      varchar(128) NOT NULL,
  sharing_profile_id   integer      DEFAULT NULL,
  sharing_profile_name varchar(128) DEFAULT NULL,
  start_date           timestamptz  NOT NULL,
  end_date             timestamptz  DEFAULT NULL,

  PRIMARY KEY (history_id),

  CONSTRAINT guacamole_connection_history_ibfk_1
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE SET NULL,

  CONSTRAINT guacamole_connection_history_ibfk_2
    FOREIGN KEY (connection_id)
    REFERENCES guacamole_connection (connection_id) ON DELETE SET NULL,

  CONSTRAINT guacamole_connection_history_ibfk_3
    FOREIGN KEY (sharing_profile_id)
    REFERENCES guacamole_sharing_profile (sharing_profile_id) ON DELETE SET NULL

);

CREATE INDEX guacamole_connection_history_user_id
    ON guacamole_connection_history(user_id);

CREATE INDEX guacamole_connection_history_connection_id
    ON guacamole_connection_history(connection_id);

CREATE INDEX guacamole_connection_history_sharing_profile_id
    ON guacamole_connection_history(sharing_profile_id);

CREATE INDEX guacamole_connection_history_start_date
    ON guacamole_connection_history(start_date);

CREATE INDEX guacamole_connection_history_end_date
    ON guacamole_connection_history(end_date);

CREATE INDEX guacamole_connection_history_connection_id_start_date
    ON guacamole_connection_history(connection_id, start_date);

--
-- User login/logout history
--

CREATE TABLE guacamole_user_history (

  history_id           serial       NOT NULL,
  user_id              integer      DEFAULT NULL,
  username             varchar(128) NOT NULL,
  remote_host          varchar(256) DEFAULT NULL,
  start_date           timestamptz  NOT NULL,
  end_date             timestamptz  DEFAULT NULL,

  PRIMARY KEY (history_id),

  CONSTRAINT guacamole_user_history_ibfk_1
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE SET NULL

);

CREATE INDEX guacamole_user_history_user_id
    ON guacamole_user_history(user_id);

CREATE INDEX guacamole_user_history_start_date
    ON guacamole_user_history(start_date);

CREATE INDEX guacamole_user_history_end_date
    ON guacamole_user_history(end_date);

CREATE INDEX guacamole_user_history_user_id_start_date
    ON guacamole_user_history(user_id, start_date);

--
-- User password history
--

CREATE TABLE guacamole_user_password_history (

  password_history_id serial  NOT NULL,
  user_id             integer NOT NULL,

  -- Salted password
  password_hash bytea        NOT NULL,
  password_salt bytea,
  password_date timestamptz  NOT NULL,

  PRIMARY KEY (password_history_id),

  CONSTRAINT guacamole_user_password_history_ibfk_1
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

);

CREATE INDEX guacamole_user_password_history_user_id
    ON guacamole_user_password_history(user_id);

