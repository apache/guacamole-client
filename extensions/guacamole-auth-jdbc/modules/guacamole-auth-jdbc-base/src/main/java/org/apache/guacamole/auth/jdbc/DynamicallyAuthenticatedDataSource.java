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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.guacamole.GuacamoleException;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;

/**
 * Pooled DataSource implementation which dynamically retrieves the database
 * username and password from the Guacamole server environment each time a
 * new database connection is created.
 */
@Singleton
public class DynamicallyAuthenticatedDataSource extends PooledDataSource {

    /**
     * Creates a new DynamicallyAuthenticatedDataSource which dynamically
     * retrieves database credentials from the given JDBCEnvironment each time
     * a new database connection is needed.
     *
     * @param environment
     *     The JDBCEnvironment that should be used to retrieve database
     *     credentials.
     *
     * @param driverClassLoader
     * @param driver
     * @param url
     */
    @Inject
    public DynamicallyAuthenticatedDataSource(JDBCEnvironment environment,
            @Named(value="JDBC.driverClassLoader") ClassLoader driverClassLoader,
            @Named(value="JDBC.driver") String driver,
            @Named(value="JDBC.url") String url) {

        // Wrap unpooled DataSource, overriding the connection process such
        // that credentials are dynamically retrieved from the JDBCEnvironment
        super(new UnpooledDataSource(driverClassLoader, driver, url, null, null) {

            @Override
            public Connection getConnection() throws SQLException {
                try {
                    return super.getConnection(environment.getUsername(), environment.getPassword());
                }
                catch (GuacamoleException e) {
                    throw new SQLException("Retrieval of database credentials failed.", e);
                }
            }

        });

    }

}
