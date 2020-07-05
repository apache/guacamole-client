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

package org.apache.guacamole.auth.jdbc;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicy;
import org.apache.ibatis.session.SqlSession;

/**
 * A JDBC-specific implementation of Environment that defines generic properties
 * intended for use within JDBC based authentication providers.
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
     * Returns whether a database user account is required for authentication to
     * succeed, even if another authentication provider has already
     * authenticated the user.
     *
     * @return
     *     true if database user accounts are required for absolutely all
     *     authentication attempts, false otherwise.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the property.
     */
    public abstract boolean isUserRequired() throws GuacamoleException;

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

    /**
     * Returns the policy which applies to newly-set passwords. Passwords which
     * apply to Guacamole user accounts will be required to conform to this
     * policy.
     *
     * @return
     *     The password policy which applies to Guacamole user accounts.
     */
    public abstract PasswordPolicy getPasswordPolicy();

    /**
     * Returns whether the database supports recursive queries. Many database
     * engines support recursive queries through CTEs. If recursive queries are
     * not supported, queries that are intended to be recursive may need to be
     * invoked multiple times to retrieve the same data.
     *
     * @param session
     *     The SqlSession provided by MyBatis for the current transaction.
     *
     * @return
     *     true if the database supports recursive queries, false otherwise.
     */
    public abstract boolean isRecursiveQuerySupported(SqlSession session);
    
    /**
     * Returns a boolean value representing whether or not the JDBC module
     * should automatically create accounts within the database for users that
     * are successfully authenticated via other extensions. Returns true if
     * accounts should be auto-created, otherwise returns false.
     * 
     * @return
     *     true if user accounts should be automatically created within the
     *     database when authentication succeeds from another extension;
     *     otherwise false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    public abstract boolean autoCreateAbsentAccounts() throws GuacamoleException;

}
