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

CREATE TABLE guacamole_user_attribute (

  `user_id`         int(11)       NOT NULL,
  `attribute_name`  varchar(128)  NOT NULL,
  `attribute_value` varchar(4096) NOT NULL,

  PRIMARY KEY (user_id, attribute_name),
  KEY `user_id` (`user_id`),

  CONSTRAINT guacamole_user_attribute_ibfk_1
    FOREIGN KEY (user_id)
    REFERENCES guacamole_user (user_id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table of arbitrary connection attributes. Each attribute is simply a
-- name/value pair associated with a connection. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE guacamole_connection_attribute (

  `connection_id`   int(11)       NOT NULL,
  `attribute_name`  varchar(128)  NOT NULL,
  `attribute_value` varchar(4096) NOT NULL,

  PRIMARY KEY (connection_id, attribute_name),
  KEY `connection_id` (`connection_id`),

  CONSTRAINT guacamole_connection_attribute_ibfk_1
    FOREIGN KEY (connection_id)
    REFERENCES guacamole_connection (connection_id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table of arbitrary connection group attributes. Each attribute is simply a
-- name/value pair associated with a connection group. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE guacamole_connection_group_attribute (

  `connection_group_id` int(11)       NOT NULL,
  `attribute_name`      varchar(128)  NOT NULL,
  `attribute_value`     varchar(4096) NOT NULL,

  PRIMARY KEY (connection_group_id, attribute_name),
  KEY `connection_group_id` (`connection_group_id`),

  CONSTRAINT guacamole_connection_group_attribute_ibfk_1
    FOREIGN KEY (connection_group_id)
    REFERENCES guacamole_connection_group (connection_group_id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table of arbitrary sharing profile attributes. Each attribute is simply a
-- name/value pair associated with a sharing profile. Arbitrary attributes are
-- defined by other extensions. Attributes defined by this extension will be
-- mapped to properly-typed columns of a specific table.
--

CREATE TABLE guacamole_sharing_profile_attribute (

  `sharing_profile_id` int(11)       NOT NULL,
  `attribute_name`     varchar(128)  NOT NULL,
  `attribute_value`    varchar(4096) NOT NULL,

  PRIMARY KEY (sharing_profile_id, attribute_name),
  KEY `sharing_profile_id` (`sharing_profile_id`),

  CONSTRAINT guacamole_sharing_profile_attribute_ibfk_1
    FOREIGN KEY (sharing_profile_id)
    REFERENCES guacamole_sharing_profile (sharing_profile_id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8;
