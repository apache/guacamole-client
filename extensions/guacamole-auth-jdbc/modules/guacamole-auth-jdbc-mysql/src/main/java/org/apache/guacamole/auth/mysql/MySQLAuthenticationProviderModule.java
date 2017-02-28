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

package org.apache.guacamole.auth.mysql;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.Properties;
import org.apache.guacamole.GuacamoleException;
import org.mybatis.guice.datasource.helper.JdbcHelper;

/**
 * Guice module which configures MySQL-specific injections.
 */
public class MySQLAuthenticationProviderModule implements Module {

    /**
     * MyBatis-specific configuration properties.
     */
    private final Properties myBatisProperties = new Properties();

    /**
     * MySQL-specific driver configuration properties.
     */
    private final Properties driverProperties = new Properties();
    
    /**
     * Creates a new MySQL authentication provider module that configures
     * driver and MyBatis properties using the given environment.
     *
     * @param environment
     *     The environment to use when configuring MyBatis and the underlying
     *     JDBC driver.
     *
     * @throws GuacamoleException
     *     If a required property is missing, or an error occurs while parsing
     *     a property.
     */
    public MySQLAuthenticationProviderModule(MySQLEnvironment environment)
            throws GuacamoleException {

        // Set the MySQL-specific properties for MyBatis.
        myBatisProperties.setProperty("mybatis.environment.id", "guacamole");
        myBatisProperties.setProperty("JDBC.host", environment.getMySQLHostname());
        myBatisProperties.setProperty("JDBC.port", String.valueOf(environment.getMySQLPort()));
        myBatisProperties.setProperty("JDBC.schema", environment.getMySQLDatabase());
        myBatisProperties.setProperty("JDBC.username", environment.getMySQLUsername());
        myBatisProperties.setProperty("JDBC.password", environment.getMySQLPassword());
        myBatisProperties.setProperty("JDBC.autoCommit", "false");
        myBatisProperties.setProperty("mybatis.pooled.pingEnabled", "true");
        myBatisProperties.setProperty("mybatis.pooled.pingQuery", "SELECT 1");

        // Use UTF-8 in database
        driverProperties.setProperty("characterEncoding", "UTF-8");

        // Allow use of multiple statements within a single query
        driverProperties.setProperty("allowMultiQueries", "true");

    }

    @Override
    public void configure(Binder binder) {

        // Bind MySQL-specific properties
        JdbcHelper.MySQL.configure(binder);
        
        // Bind MyBatis properties
        Names.bindProperties(binder, myBatisProperties);

        // Bind JDBC driver properties
        binder.bind(Properties.class)
            .annotatedWith(Names.named("JDBC.driverProperties"))
            .toInstance(driverProperties);

    }

}
