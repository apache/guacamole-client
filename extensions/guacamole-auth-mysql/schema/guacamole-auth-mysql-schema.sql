--
-- Table structure for table `guacamole_connection`
--

CREATE TABLE `guacamole_connection` (
  `connection_id` int(11) NOT NULL AUTO_INCREMENT,
  `connection_name` varchar(128) NOT NULL,
  `protocol` varchar(32) NOT NULL,
  PRIMARY KEY (`connection_id`),
  UNIQUE KEY `connection_name` (`connection_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `guacamole_user`
--

CREATE TABLE `guacamole_user` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(128) NOT NULL,
  `password_hash` binary(32) NOT NULL,
  `password_salt` binary(32) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `guacamole_connection_parameter`
--

CREATE TABLE `guacamole_connection_parameter` (
  `connection_id` int(11) NOT NULL,
  `parameter_name` varchar(128) NOT NULL,
  `parameter_value` varchar(4096) NOT NULL,
  PRIMARY KEY (`connection_id`,`parameter_name`),
  CONSTRAINT `guacamole_connection_parameter_ibfk_1` FOREIGN KEY (`connection_id`) REFERENCES `guacamole_connection` (`connection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `guacamole_connection_permission`
--

CREATE TABLE `guacamole_connection_permission` (
  `user_id` int(11) NOT NULL,
  `connection_id` int(11) NOT NULL,
  `permission` enum('READ','WRITE','DELETE','ADMINISTER') NOT NULL,
  PRIMARY KEY (`user_id`,`connection_id`,`permission`),
  CONSTRAINT `guacamole_connection_permission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `guacamole_connection` (`connection_id`),
  CONSTRAINT `guacamole_connection_permission_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `guacamole_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `guacamole_system_permission`
--

CREATE TABLE `guacamole_system_permission` (
  `user_id` int(11) NOT NULL,
  `permission` enum('CREATE_CONFIGURATION','CREATE_USER') NOT NULL,
  PRIMARY KEY (`user_id`,`permission`),
  CONSTRAINT `guacamole_system_permission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `guacamole_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `guacamole_user_permission`
--

CREATE TABLE `guacamole_user_permission` (
  `user_id` int(11) NOT NULL,
  `affected_user_id` int(11) NOT NULL,
  `permission` enum('READ','WRITE','DELETE','ADMINISTER') NOT NULL,
  PRIMARY KEY (`user_id`,`affected_user_id`,`permission`),
  CONSTRAINT `guacamole_user_permission_ibfk_1` FOREIGN KEY (`affected_user_id`) REFERENCES `guacamole_user` (`user_id`),
  CONSTRAINT `guacamole_user_permission_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `guacamole_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

