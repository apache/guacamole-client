/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.guacamole.net.auth.mysql.properties;

import net.sourceforge.guacamole.properties.IntegerGuacamoleProperty;
import net.sourceforge.guacamole.properties.StringGuacamoleProperty;

/**
 * Properties used by the MySQL Authentication plugin.
 * @author dagger10k
 */
public class MySQLGuacamoleProperties {
    
    /**
     * This class should not be instantiated.
     */
    private MySQLGuacamoleProperties() {}
    
    /**
     * The URL of the MySQL server hosting the guacamole authentication tables.
     */
    public static final StringGuacamoleProperty MYSQL_HOSTNAME = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-hostname"; }

    };
    
    /**
     * The port of the MySQL server hosting the guacamole authentication tables.
     */
    public static final IntegerGuacamoleProperty MYSQL_PORT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-port"; }

    };
    
    /**
     * The name of the MySQL database containing the guacamole authentication tables.
     */
    public static final StringGuacamoleProperty MYSQL_DATABASE = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-database"; }

    };
    
    /**
     * The username used to authenticate to the MySQL database containing the guacamole authentication tables.
     */
    public static final StringGuacamoleProperty MYSQL_USERNAME = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-username"; }

    };
    
    /**
     * The password used to authenticate to the MySQL database containing the guacamole authentication tables.
     */
    public static final StringGuacamoleProperty MYSQL_PASSWORD = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "mysql-password"; }

    };
}
