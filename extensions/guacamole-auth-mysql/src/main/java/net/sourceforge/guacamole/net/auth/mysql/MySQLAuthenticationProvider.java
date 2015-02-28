/*
 * Copyright (C) 2013 Glyptodon LLC
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


import org.glyptodon.guacamole.auth.mysql.user.MySQLUserContext;
import org.glyptodon.guacamole.auth.mysql.connectiongroup.MySQLRootConnectionGroup;
import org.glyptodon.guacamole.auth.mysql.connectiongroup.MySQLConnectionGroup;
import org.glyptodon.guacamole.auth.mysql.connectiongroup.ConnectionGroupDirectory;
import org.glyptodon.guacamole.auth.mysql.connection.ConnectionDirectory;
import org.glyptodon.guacamole.auth.mysql.connection.MySQLGuacamoleConfiguration;
import org.glyptodon.guacamole.auth.mysql.connection.MySQLConnection;
import org.glyptodon.guacamole.auth.mysql.permission.MySQLSystemPermissionSet;
import org.glyptodon.guacamole.auth.mysql.user.MySQLUser;
import org.glyptodon.guacamole.auth.mysql.user.UserDirectory;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.Properties;
import org.glyptodon.guacamole.auth.mysql.connectiongroup.ConnectionGroupMapper;
import org.glyptodon.guacamole.auth.mysql.connection.ConnectionMapper;
import org.glyptodon.guacamole.auth.mysql.connection.ConnectionRecordMapper;
import org.glyptodon.guacamole.auth.mysql.connection.ParameterMapper;
import org.glyptodon.guacamole.auth.mysql.permission.SystemPermissionMapper;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.auth.mysql.user.UserMapper;
import org.glyptodon.guacamole.auth.mysql.conf.MySQLGuacamoleProperties;
import org.glyptodon.guacamole.auth.mysql.connectiongroup.ConnectionGroupService;
import org.glyptodon.guacamole.auth.mysql.connection.ConnectionService;
import org.glyptodon.guacamole.auth.mysql.socket.GuacamoleSocketService;
import org.glyptodon.guacamole.auth.mysql.security.PasswordEncryptionService;
import org.glyptodon.guacamole.auth.mysql.security.SHA256PasswordEncryptionService;
import org.glyptodon.guacamole.auth.mysql.security.SaltService;
import org.glyptodon.guacamole.auth.mysql.security.SecureRandomSaltService;
import org.glyptodon.guacamole.auth.mysql.permission.SystemPermissionService;
import org.glyptodon.guacamole.auth.mysql.socket.UnrestrictedGuacamoleSocketService;
import org.glyptodon.guacamole.auth.mysql.user.UserService;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.environment.LocalEnvironment;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.builtin.PooledDataSourceProvider;
import org.mybatis.guice.datasource.helper.JdbcHelper;

/**
 * Provides a MySQL based implementation of the AuthenticationProvider
 * functionality.
 *
 * @author James Muehlner
 */
public class MySQLAuthenticationProvider implements AuthenticationProvider {

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    @Override
    public UserContext getUserContext(Credentials credentials) throws GuacamoleException {

        // Get user service
        UserService userService = injector.getInstance(UserService.class);

        // Authenticate user
        MySQLUser user = userService.retrieveUser(credentials);
        if (user != null) {

            // Upon successful authentication, return new user context
            MySQLUserContext context = injector.getInstance(MySQLUserContext.class);
            context.init(user.getCurrentUser());
            return context;

        }

        // Otherwise, unauthorized
        return null;

    }

    /**
     * Creates a new MySQLAuthenticationProvider that reads and writes
     * authentication data to a MySQL database defined by properties in
     * guacamole.properties.
     *
     * @throws GuacamoleException If a required property is missing, or
     *                            an error occurs while parsing a property.
     */
    public MySQLAuthenticationProvider() throws GuacamoleException {

        // Get local environment
        final Environment environment = new LocalEnvironment();
        
        final Properties myBatisProperties = new Properties();
        final Properties driverProperties = new Properties();

        // Set the mysql properties for MyBatis.
        myBatisProperties.setProperty("mybatis.environment.id", "guacamole");
        myBatisProperties.setProperty("JDBC.host", environment.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_HOSTNAME));
        myBatisProperties.setProperty("JDBC.port", String.valueOf(environment.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_PORT)));
        myBatisProperties.setProperty("JDBC.schema", environment.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_DATABASE));
        myBatisProperties.setProperty("JDBC.username", environment.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_USERNAME));
        myBatisProperties.setProperty("JDBC.password", environment.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_PASSWORD));
        myBatisProperties.setProperty("JDBC.autoCommit", "false");
        myBatisProperties.setProperty("mybatis.pooled.pingEnabled", "true");
        myBatisProperties.setProperty("mybatis.pooled.pingQuery", "SELECT 1");
        driverProperties.setProperty("characterEncoding","UTF-8");

        // Set up Guice injector.
        injector = Guice.createInjector(
            JdbcHelper.MySQL,

            new Module() {
                @Override
                public void configure(Binder binder) {
                    Names.bindProperties(binder, myBatisProperties);
                    binder.bind(Properties.class)
                        .annotatedWith(Names.named("JDBC.driverProperties"))
                        .toInstance(driverProperties);
                }
            },

            new MyBatisModule() {
                @Override
                protected void initialize() {

                    // Datasource
                    bindDataSourceProviderType(PooledDataSourceProvider.class);

                    // Transaction factory
                    bindTransactionFactoryType(JdbcTransactionFactory.class);

                    // Add MyBatis mappers
                    addMapperClass(ConnectionMapper.class);
                    addMapperClass(ConnectionGroupMapper.class);
                    addMapperClass(ConnectionRecordMapper.class);
                    addMapperClass(ParameterMapper.class);
                    addMapperClass(SystemPermissionMapper.class);
                    addMapperClass(UserMapper.class);

                    // Bind core implementations of guacamole-ext classes
                    bind(Environment.class).toInstance(environment);
                    bind(ConnectionDirectory.class);
                    bind(ConnectionGroupDirectory.class);
                    bind(MySQLConnection.class);
                    bind(MySQLConnectionGroup.class);
                    bind(MySQLGuacamoleConfiguration.class);
                    bind(MySQLUser.class);
                    bind(MySQLUserContext.class);
                    bind(MySQLRootConnectionGroup.class);
                    bind(MySQLSystemPermissionSet.class);
                    bind(UserDirectory.class);

                    // Bind services
                    bind(ConnectionService.class);
                    bind(ConnectionGroupService.class);
                    bind(PasswordEncryptionService.class).to(SHA256PasswordEncryptionService.class);
                    bind(SaltService.class).to(SecureRandomSaltService.class);
                    bind(SystemPermissionService.class);
                    bind(UserService.class);

                    // Bind appropriate socket service based on policy
                    bind(GuacamoleSocketService.class).to(UnrestrictedGuacamoleSocketService.class);
                    
                }
            } // end of mybatis module

        );
    } // end of constructor

    @Override
    public UserContext updateUserContext(UserContext context,
        Credentials credentials) throws GuacamoleException {

        // No need to update the context
        return context;

    }

}
