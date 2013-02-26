insert into guacamole_user values(1, 'guacadmin', x'AE97B20D5B24B2F18BE7921E3C0CF6109696391D7D5A6BE24BD267E49F0D7E42', x'FE24ADC5E11E2B25288D1704ABE67A79E342ECC26064CE69C5B3177795A82264');

insert into guacamole_system_permission values(1, 'CREATE_CONNECTION');
insert into guacamole_system_permission values(1, 'CREATE_USER');

insert into guacamole_user_permission values(1, 1, 'READ');
insert into guacamole_user_permission values(1, 1, 'UPDATE');
insert into guacamole_user_permission values(1, 1, 'DELETE');
insert into guacamole_user_permission values(1, 1, 'ADMINISTER');
