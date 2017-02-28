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

package org.apache.guacamole.auth.postgresql;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.Properties;
import org.apache.guacamole.GuacamoleException;
import org.mybatis.guice.datasource.helper.JdbcHelper;

/**
 * Guice module which configures PostgreSQL-specific injections.
 */
public class PostgreSQLAuthenticationProviderModule implements Module {

    /**
     * MyBatis-specific configuration properties.
     */
    private final Properties myBatisProperties = new Properties();

    /**
     * PostgreSQL-specific driver configuration properties.
     */
    private final Properties driverProperties = new Properties();
    
    /**
     * Creates a new PostgreSQL authentication provider module that configures
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
    public PostgreSQLAuthenticationProviderModule(PostgreSQLEnvironment environment)
            throws GuacamoleException {

        // Set the PostgreSQL-specific properties for MyBatis.
        myBatisProperties.setProperty("mybatis.environment.id", "guacamole");
        myBatisProperties.setProperty("JDBC.host", environment.getPostgreSQLHostname());
        myBatisProperties.setProperty("JDBC.port", String.valueOf(environment.getPostgreSQLPort()));
        myBatisProperties.setProperty("JDBC.schema", environment.getPostgreSQLDatabase());
        myBatisProperties.setProperty("JDBC.username", environment.getPostgreSQLUsername());
        myBatisProperties.setProperty("JDBC.password", environment.getPostgreSQLPassword());
        myBatisProperties.setProperty("JDBC.autoCommit", "false");
        myBatisProperties.setProperty("mybatis.pooled.pingEnabled", "true");
        myBatisProperties.setProperty("mybatis.pooled.pingQuery", "SELECT 1");

        // Use UTF-8 in database
        driverProperties.setProperty("characterEncoding", "UTF-8");

    }

    @Override
    public void configure(Binder binder) {

        // Bind PostgreSQL-specific properties
        JdbcHelper.PostgreSQL.configure(binder);
        
        // Bind MyBatis properties
        Names.bindProperties(binder, myBatisProperties);

        // Bind JDBC driver properties
        binder.bind(Properties.class)
            .annotatedWith(Names.named("JDBC.driverProperties"))
            .toInstance(driverProperties);

    }

}
