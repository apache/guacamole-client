
package net.sourceforge.guacamole.net.auth.mysql;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-auth-mysql.
 *
 * The Initial Developer of the Original Code is
 * James Muehlner.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.Properties;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.AuthenticationProvider;
import net.sourceforge.guacamole.net.auth.Credentials;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionHistoryMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionParameterMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.SystemPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserMapper;
import net.sourceforge.guacamole.net.auth.mysql.dao.UserPermissionMapper;
import net.sourceforge.guacamole.net.auth.mysql.properties.MySQLGuacamoleProperties;
import net.sourceforge.guacamole.net.auth.mysql.service.ConnectionService;
import net.sourceforge.guacamole.net.auth.mysql.service.PasswordEncryptionService;
import net.sourceforge.guacamole.net.auth.mysql.service.PermissionCheckService;
import net.sourceforge.guacamole.net.auth.mysql.service.SaltService;
import net.sourceforge.guacamole.net.auth.mysql.service.SecureRandomSaltService;
import net.sourceforge.guacamole.net.auth.mysql.service.SHA256PasswordEncryptionService;
import net.sourceforge.guacamole.net.auth.mysql.service.UserService;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
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
     * Set of all active connections.
     */
    private ActiveConnectionSet activeConnectionSet = new ActiveConnectionSet();

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private Injector injector;

    @Override
    public UserContext getUserContext(Credentials credentials) throws GuacamoleException {

        // Get user service
        UserService userService = injector.getInstance(UserService.class);

        // Get user
        MySQLUser authenticatedUser = userService.retrieveUser(credentials);
        if (authenticatedUser != null) {
            MySQLUserContext context = injector.getInstance(MySQLUserContext.class);
            context.init(authenticatedUser.getUserID());
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

        final Properties myBatisProperties = new Properties();

        // Set the mysql properties for MyBatis.
        myBatisProperties.setProperty("mybatis.environment.id", "guacamole");
        myBatisProperties.setProperty("JDBC.host", GuacamoleProperties.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_HOSTNAME));
        myBatisProperties.setProperty("JDBC.port", String.valueOf(GuacamoleProperties.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_PORT)));
        myBatisProperties.setProperty("JDBC.schema", GuacamoleProperties.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_DATABASE));
        myBatisProperties.setProperty("JDBC.username", GuacamoleProperties.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_USERNAME));
        myBatisProperties.setProperty("JDBC.password", GuacamoleProperties.getRequiredProperty(MySQLGuacamoleProperties.MYSQL_PASSWORD));
        myBatisProperties.setProperty("JDBC.autoCommit", "false");

        // Set up Guice injector.
        injector = Guice.createInjector(
            JdbcHelper.MySQL,

            new Module() {
                @Override
                public void configure(Binder binder) {
                    Names.bindProperties(binder, myBatisProperties);
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
                    addMapperClass(ConnectionHistoryMapper.class);
                    addMapperClass(ConnectionMapper.class);
                    addMapperClass(ConnectionParameterMapper.class);
                    addMapperClass(ConnectionPermissionMapper.class);
                    addMapperClass(SystemPermissionMapper.class);
                    addMapperClass(UserMapper.class);
                    addMapperClass(UserPermissionMapper.class);

                    // Bind interfaces
                    bind(MySQLUserContext.class);
                    bind(UserDirectory.class);
                    bind(MySQLUser.class);
                    bind(SaltService.class).to(SecureRandomSaltService.class);
                    bind(PasswordEncryptionService.class).to(SHA256PasswordEncryptionService.class);
                    bind(PermissionCheckService.class);
                    bind(ConnectionService.class);
                    bind(UserService.class);
                    bind(ActiveConnectionSet.class).toInstance(activeConnectionSet);

                }
            } // end of mybatis module

        );
    } // end of constructor

}
