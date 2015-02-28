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

package org.glyptodon.guacamole.auth.jdbc;

import org.glyptodon.guacamole.auth.jdbc.user.MySQLUserContext;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.MySQLRootConnectionGroup;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.MySQLConnectionGroup;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ConnectionGroupDirectory;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionDirectory;
import org.glyptodon.guacamole.auth.jdbc.connection.MySQLGuacamoleConfiguration;
import org.glyptodon.guacamole.auth.jdbc.connection.MySQLConnection;
import org.glyptodon.guacamole.auth.jdbc.permission.MySQLSystemPermissionSet;
import org.glyptodon.guacamole.auth.jdbc.user.MySQLUser;
import org.glyptodon.guacamole.auth.jdbc.user.UserDirectory;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ConnectionGroupMapper;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionRecordMapper;
import org.glyptodon.guacamole.auth.jdbc.connection.ParameterMapper;
import org.glyptodon.guacamole.auth.jdbc.permission.SystemPermissionMapper;
import org.glyptodon.guacamole.auth.jdbc.user.UserMapper;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ConnectionGroupService;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionService;
import org.glyptodon.guacamole.auth.jdbc.socket.GuacamoleSocketService;
import org.glyptodon.guacamole.auth.jdbc.security.PasswordEncryptionService;
import org.glyptodon.guacamole.auth.jdbc.security.SHA256PasswordEncryptionService;
import org.glyptodon.guacamole.auth.jdbc.security.SaltService;
import org.glyptodon.guacamole.auth.jdbc.security.SecureRandomSaltService;
import org.glyptodon.guacamole.auth.jdbc.permission.SystemPermissionService;
import org.glyptodon.guacamole.auth.jdbc.socket.UnrestrictedGuacamoleSocketService;
import org.glyptodon.guacamole.auth.jdbc.user.UserService;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.glyptodon.guacamole.environment.Environment;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.builtin.PooledDataSourceProvider;

/**
 * Guice module which configures the injections used by the JDBC authentication
 * provider base. This module MUST be included in the Guice injector, or
 * authentication providers based on JDBC will not function.
 *
 * @author Michael Jumper
 * @author James Muehlner
 */
public class JDBCAuthenticationProviderModule extends MyBatisModule {

    /**
     * The environment of the Guacamole server.
     */
    private final Environment environment;

    /**
     * Creates a new JDBC authentication provider module that configures the
     * various injected base classes using the given environment.
     *
     * @param environment
     *     The environment to use to configure injected classes.
     */
    public JDBCAuthenticationProviderModule(Environment environment) {
        this.environment = environment;
    }

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

}
