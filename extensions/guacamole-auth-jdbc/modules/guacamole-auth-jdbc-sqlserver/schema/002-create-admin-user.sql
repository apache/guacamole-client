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
 * Create the default admin user account and set up full privileges.
 */
INSERT INTO [guacamole].[user] (username, password_hash, password_date)
VALUES ('guacadmin', HASHBYTES('SHA2_256', 'guacadmin'), getdate());

INSERT INTO [guacamole].[user_permission]
SELECT [guacamole].[user].[user_id], [affected].[user_id], permission
FROM (
    SELECT 'guacadmin' AS username, 'guacadmin' AS affected_username, 'READ' AS permission
        UNION SELECT 'guacadmin' AS username, 'guacadmin' AS affected_username, 'UPDATE' AS permission
        UNION SELECT 'guacadmin' AS username, 'guacadmin' AS affected_username, 'ADMINISTER' AS permission)
    permissions
    JOIN [guacamole].[user] ON permissions.username = [guacamole].[user].[username]
    JOIN [guacamole].[user] affected ON permissions.affected_username = affected.username;

INSERT INTO [guacamole].[system_permission]
SELECT user_id, permission
FROM (
    SELECT 'guacadmin' AS username, 'CREATE_CONNECTION' AS permission
        UNION SELECT 'guacadmin' AS username, 'CREATE_CONNECTION_GROUP' AS permission
        UNION SELECT 'guacadmin' AS username, 'CREATE_SHARING_PROFILE' AS permission
        UNION SELECT 'guacadmin' AS username, 'CREATE_USER' AS permission
        UNION SELECT 'guacadmin' AS username, 'ADMINISTER' AS permission)
    permissions
    JOIN [guacamole].[user] ON permissions.username = [guacamole].[user].[username];
GO
