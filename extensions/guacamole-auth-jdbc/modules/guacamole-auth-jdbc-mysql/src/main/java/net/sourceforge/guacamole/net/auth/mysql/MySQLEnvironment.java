/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.sourceforge.guacamole.net.auth.mysql;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.JDBCEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MySQL-specific implementation of JDBCEnvironment provides database
 * properties specifically for MySQL.
 *
 * @author James Muehlner
 */
public class MySQLEnvironment extends JDBCEnvironment {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(MySQLEnvironment.class);
    
    /**
     * Constructs a new MysqlEnvironment.
     * 
     * @throws GuacamoleException 
     *     If an error occurs while setting up the underlying JDBCEnvironment.
     */
    public MySQLEnvironment() throws GuacamoleException {
        super();
    }
    
    /**
     * Log a warning about the usage of the deprecated 
     * MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS property, and the appropriate
     * replacements for it.
     * 
     * @param disallowSimultaneous 
     *     Whether simultaneous connections have been disabled.
     */
    private void warnOfSimultaneousPropertyDeprecation(boolean disallowSimultaneous) {
        
        // Warn of deprecation
        logger.warn("The \"{}\" property is deprecated. Use \"{}\" and \"{}\" instead.",
                MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(),
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS.getName(),
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

        // Inform of new equivalent
        logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS.getName(), disallowSimultaneous,
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS.getName(),           disallowSimultaneous ? 1 : 0,
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName(),     0);

    }
    
    /**
     * Log a warning about the usage of the deprecated 
     * MYSQL_DISALLOW_DUPLICATE_CONNECTIONS property, and the appropriate
     * replacements for it.
     * 
     * @param disallowDuplicate 
     *     Whether duplicate connections have been disabled.
     */
    private void warnOfDuplicatePropertyDeprecation(boolean disallowDuplicate) {
        
        // Warn of deprecation
        logger.warn("The \"{}\" property is deprecated. Use \"{}\" and \"{}\" instead.",
                MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS.getName());

        // Inform of new equivalent
        logger.info("To achieve the same result of setting \"{}\" to \"{}\", set \"{}\" to \"{}\" and \"{}\" to \"{}\".",
                MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS.getName(),         disallowDuplicate,
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER.getName(),       disallowDuplicate ? 1 :0,
                MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER.getName(), disallowDuplicate ? 1 :0);

    }
    
    @Override
    public int getDefaultMaxConnections() throws GuacamoleException {

        // Tunnel service default configuration
        int connectionDefaultMaxConnections;
        
        // Read legacy concurrency-related property
        Boolean disallowSimultaneous = getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS);

        // Legacy "simultaneous" property dictates only the maximum number of
        // connections per connection
        if (disallowSimultaneous != null) {

            // Translate legacy property
            if (disallowSimultaneous) {
                connectionDefaultMaxConnections      = 1;
            }
            else {
                connectionDefaultMaxConnections      = 0;
            }

            // Warn that a different property should be used going forward
            warnOfSimultaneousPropertyDeprecation(disallowSimultaneous);

        }

        // If legacy property is not specified, use new property
        else {
            connectionDefaultMaxConnections = getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS, 0);
        }
        
        return connectionDefaultMaxConnections;
    }

    @Override
    public int getDefaultMaxGroupConnections() throws GuacamoleException {

        int connectionGroupDefaultMaxConnections;

        // Read legacy concurrency-related property
        Boolean disallowSimultaneous = getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_SIMULTANEOUS_CONNECTIONS);

        // Legacy "simultaneous" property dictates only the maximum number of
        // connections per connection
        if (disallowSimultaneous != null) {

            // Translate legacy property
            connectionGroupDefaultMaxConnections = 0;

            // Warn that a different property should be used going forward
            warnOfSimultaneousPropertyDeprecation(disallowSimultaneous);

        }

        // If legacy property is not specified, use new property
        else {
            connectionGroupDefaultMaxConnections = getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS, 0);
        }
        
        return connectionGroupDefaultMaxConnections;
    }

    @Override
    public int getDefaultMaxConnectionsPerUser() throws GuacamoleException {

        int connectionDefaultMaxConnectionsPerUser;

        // Read legacy concurrency-related properties
        Boolean disallowDuplicate    = getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS);

        // Legacy "duplicate" property dictates whether connections and groups
        // may be used concurrently only by different users
        if (disallowDuplicate != null) {

            // Translate legacy property
            if (disallowDuplicate) {
                connectionDefaultMaxConnectionsPerUser      = 1;
            }
            else {
                connectionDefaultMaxConnectionsPerUser      = 0;
            }
            
            // Warn that a different property should be used going forward
            warnOfDuplicatePropertyDeprecation(disallowDuplicate);

        }

        // If legacy property is not specified, use new property
        else {
            connectionDefaultMaxConnectionsPerUser = getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER, 1);
        }
        
        return connectionDefaultMaxConnectionsPerUser;
    }

    @Override
    public int getDefaultMaxGroupConnectionsPerUser() throws GuacamoleException {

        int connectionGroupDefaultMaxConnectionsPerUser;

        // Read legacy concurrency-related property
        Boolean disallowDuplicate = getProperty(MySQLGuacamoleProperties.MYSQL_DISALLOW_DUPLICATE_CONNECTIONS);

        // Legacy "duplicate" property dictates whether connections and groups
        // may be used concurrently only by different users
        if (disallowDuplicate != null) {

            // Translate legacy property
            if (disallowDuplicate) {
                connectionGroupDefaultMaxConnectionsPerUser = 1;
            }
            else {
                connectionGroupDefaultMaxConnectionsPerUser = 0;
            }
            
            // Warn that a different property should be used going forward
            warnOfDuplicatePropertyDeprecation(disallowDuplicate);

        }

        // If legacy property is not specified, use new property
        else {
            connectionGroupDefaultMaxConnectionsPerUser = getProperty(MySQLGuacamoleProperties.MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER, 1);
        }

        return connectionGroupDefaultMaxConnectionsPerUser;

    }

    /**
     * Returns the hostname of the MySQL server hosting the Guacamole
     * authentication tables. If unspecified, this will be "localhost".
     * 
     * @return
     *     The URL of the MySQL server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public String getMySQLHostname() throws GuacamoleException {
        return getProperty(MySQLGuacamoleProperties.MYSQL_HOSTNAME, "localhost");
    }
    
    /**
     * Returns the port number of the MySQL server hosting the Guacamole
     * authentication tables. If unspecified, this will be the default MySQL
     * port of 3306.
     * 
     * @return
     *     The port number of the MySQL server.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value.
     */
    public int getMySQLPort() throws GuacamoleException {
        return getProperty(MySQLGuacamoleProperties.MYSQL_PORT, 3306);
    }
    
    /**
     * Returns the name of the MySQL database containing the Guacamole 
     * authentication tables.
     * 
     * @return
     *     The name of the MySQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getMySQLDatabase() throws GuacamoleException {
        return getRequiredProperty(MySQLGuacamoleProperties.MYSQL_DATABASE);
    }
    
    /**
     * Returns the username that should be used when authenticating with the
     * MySQL database containing the Guacamole authentication tables.
     * 
     * @return
     *     The username for the MySQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getMySQLUsername() throws GuacamoleException {
        return getRequiredProperty(MySQLGuacamoleProperties.MYSQL_USERNAME);
    }
    
    /**
     * Returns the password that should be used when authenticating with the
     * MySQL database containing the Guacamole authentication tables.
     * 
     * @return
     *     The password for the MySQL database.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving the property value, or if the
     *     value was not set, as this property is required.
     */
    public String getMySQLPassword() throws GuacamoleException {
        return getRequiredProperty(MySQLGuacamoleProperties.MYSQL_PASSWORD);
    }
    
}
