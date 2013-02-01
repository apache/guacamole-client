-- MySQL dump 10.13  Distrib 5.5.29, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: guacamole
-- ------------------------------------------------------
-- Server version	5.5.29-0ubuntu0.12.10.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `guacamole_connection`
--

DROP TABLE IF EXISTS `guacamole_connection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guacamole_connection` (
  `connection_id` int(11) NOT NULL,
  `connection_name` varchar(128) NOT NULL,
  PRIMARY KEY (`connection_id`),
  UNIQUE KEY `connection_name` (`connection_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guacamole_connection`
--

LOCK TABLES `guacamole_connection` WRITE;
/*!40000 ALTER TABLE `guacamole_connection` DISABLE KEYS */;
/*!40000 ALTER TABLE `guacamole_connection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guacamole_connection_parameter`
--

DROP TABLE IF EXISTS `guacamole_connection_parameter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guacamole_connection_parameter` (
  `connection_id` int(11) NOT NULL DEFAULT '0',
  `parameter_name` varchar(128) NOT NULL DEFAULT '',
  `parameter_value` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`connection_id`,`parameter_name`),
  CONSTRAINT `guacamole_connection_parameter_ibfk_1` FOREIGN KEY (`connection_id`) REFERENCES `guacamole_connection` (`connection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guacamole_connection_parameter`
--

LOCK TABLES `guacamole_connection_parameter` WRITE;
/*!40000 ALTER TABLE `guacamole_connection_parameter` DISABLE KEYS */;
/*!40000 ALTER TABLE `guacamole_connection_parameter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guacamole_connection_permission`
--

DROP TABLE IF EXISTS `guacamole_connection_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guacamole_connection_permission` (
  `user_id` int(11) NOT NULL DEFAULT '0',
  `connection_id` int(11) NOT NULL DEFAULT '0',
  `permission` enum('READ','WRITE','DELETE','ADMINISTER') NOT NULL,
  PRIMARY KEY (`user_id`,`connection_id`,`permission`),
  CONSTRAINT `guacamole_connection_permission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `guacamole_connection` (`connection_id`),
  CONSTRAINT `guacamole_connection_permission_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `guacamole_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guacamole_connection_permission`
--

LOCK TABLES `guacamole_connection_permission` WRITE;
/*!40000 ALTER TABLE `guacamole_connection_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `guacamole_connection_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guacamole_system_permission`
--

DROP TABLE IF EXISTS `guacamole_system_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guacamole_system_permission` (
  `user_id` int(11) NOT NULL DEFAULT '0',
  `permission` enum('CREATE_CONFIGURATION','CREATE_USER') NOT NULL,
  PRIMARY KEY (`user_id`,`permission`),
  CONSTRAINT `guacamole_system_permission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `guacamole_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guacamole_system_permission`
--

LOCK TABLES `guacamole_system_permission` WRITE;
/*!40000 ALTER TABLE `guacamole_system_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `guacamole_system_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guacamole_user`
--

DROP TABLE IF EXISTS `guacamole_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guacamole_user` (
  `user_id` int(11) NOT NULL,
  `username` varchar(128) NOT NULL,
  `password_hash` binary(32) NOT NULL,
  `password_salt` varchar(100) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guacamole_user`
--

LOCK TABLES `guacamole_user` WRITE;
/*!40000 ALTER TABLE `guacamole_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `guacamole_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guacamole_user_permission`
--

DROP TABLE IF EXISTS `guacamole_user_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guacamole_user_permission` (
  `user_id` int(11) NOT NULL DEFAULT '0',
  `other_user_id` int(11) NOT NULL DEFAULT '0',
  `permission` enum('READ','WRITE','DELETE','ADMINISTER') NOT NULL,
  PRIMARY KEY (`user_id`,`other_user_id`,`permission`),
  CONSTRAINT `guacamole_user_permission_ibfk_1` FOREIGN KEY (`other_user_id`) REFERENCES `guacamole_user` (`user_id`),
  CONSTRAINT `guacamole_user_permission_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `guacamole_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guacamole_user_permission`
--

LOCK TABLES `guacamole_user_permission` WRITE;
/*!40000 ALTER TABLE `guacamole_user_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `guacamole_user_permission` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-02-01  0:02:13
