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

package org.apache.guacamole.auth.jdbc;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.LocalEnvironment;

/**
 * A JDBC-specific implementation of Environment that defines generic properties
 * intended for use within JDBC based authentication providers.
 *
 * @author James Muehlner
 */
public abstract class JDBCEnvironment extends LocalEnvironment {
    
    /**
     * Constructs a new JDBCEnvironment using an underlying LocalEnviroment to
     * read properties from the file system.
     * 
     * @throws GuacamoleException
     *     If an error occurs while setting up the underlying LocalEnvironment.
     */
    public JDBCEnvironment() throws GuacamoleException {
        super();
    }

    /**
     * Returns the maximum number of concurrent connections to allow overall.
     * As this limit applies globally (independent of which connection is in
     * use or which user is using it), this setting cannot be overridden at the
     * connection level. Zero denotes unlimited.
     *
     * @return
     *     The maximum allowable number of concurrent connections.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property.
     */
    public abstract int getAbsoluteMaxConnections() throws GuacamoleException;

    /**
     * Returns the default maximum number of concurrent connections to allow to 
     * any one connection, unless specified differently on an individual 
     * connection. Zero denotes unlimited.
     * 
     * @return
     *     The default maximum allowable number of concurrent connections 
     *     to any connection.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property.
     */
    public abstract int getDefaultMaxConnections() throws GuacamoleException;
    
    /**
     * Returns the default maximum number of concurrent connections to allow to 
     * any one connection group, unless specified differently on an individual 
     * connection group. Zero denotes unlimited.
     * 
     * @return
     *     The default maximum allowable number of concurrent connections
     *     to any connection group.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property.
     */
    public abstract int getDefaultMaxGroupConnections()
            throws GuacamoleException;
    
    /**
     * Returns the default maximum number of concurrent connections to allow to 
     * any one connection by an individual user, unless specified differently on
     * an individual connection. Zero denotes unlimited.
     * 
     * @return
     *     The default maximum allowable number of concurrent connections to
     *     any connection by an individual user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property.
     */
    public abstract int getDefaultMaxConnectionsPerUser()
            throws GuacamoleException;
    
    /**
     * Returns the default maximum number of concurrent connections to allow to 
     * any one connection group by an individual user, unless specified 
     * differently on an individual connection group. Zero denotes unlimited.
     * 
     * @return
     *     The default maximum allowable number of concurrent connections to
     *     any connection group by an individual user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property.
     */
    public abstract int getDefaultMaxGroupConnectionsPerUser()
            throws GuacamoleException;

}
