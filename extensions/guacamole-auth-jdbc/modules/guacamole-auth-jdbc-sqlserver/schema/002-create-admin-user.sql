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