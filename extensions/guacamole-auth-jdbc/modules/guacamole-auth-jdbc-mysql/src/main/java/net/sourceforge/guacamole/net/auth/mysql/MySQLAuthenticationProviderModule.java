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

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.Properties;
import org.glyptodon.guacamole.GuacamoleException;
import org.mybatis.guice.datasource.helper.JdbcHelper;

/**
 * Guice module which configures MySQL-specific injections.
 *
 * @author James Muehlner
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
        driverProperties.setProperty("characterEncoding","UTF-8");


    }

    @Override
    public void configure(Binder binder) {

        // Bind MySQL-specific properties
        JdbcHelper.MySQL.configure(binder);
        
        // Bind MyBatis properties
        Names.bindProperties(binder, myBatisProperties);

        // Bing JDBC driver properties
        binder.bind(Properties.class)
            .annotatedWith(Names.named("JDBC.driverProperties"))
            .toInstance(driverProperties);

    }

}
