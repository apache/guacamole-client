
-- Create default user "guacadmin" with password "guacadmin"
insert into guacamole_user values(1, 'guacadmin',
    x'CA458A7D494E3BE824F5E1E175A1556C0F8EEF2C2D7DF3633BEC4A29C4411960',  -- 'guacadmin'
    x'FE24ADC5E11E2B25288D1704ABE67A79E342ECC26064CE69C5B3177795A82264');

-- Grant this user create permissions
insert into guacamole_system_permission values(1, 'CREATE_CONNECTION');
insert into guacamole_system_permission values(1, 'CREATE_CONNECTION_GROUP');
insert into guacamole_system_permission values(1, 'CREATE_USER');
insert into guacamole_system_permission values(1, 'ADMINISTER');

-- Grant admin permission to read/update/administer self
insert into guacamole_user_permission values(1, 1, 'READ');
insert into guacamole_user_permission values(1, 1, 'UPDATE');
insert into guacamole_user_permission values(1, 1, 'ADMINISTER');

