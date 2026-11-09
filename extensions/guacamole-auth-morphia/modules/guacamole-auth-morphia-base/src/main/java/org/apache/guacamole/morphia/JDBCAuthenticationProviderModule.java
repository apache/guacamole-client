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

package org.apache.guacamole.morphia;

import org.apache.guacamole.morphia.activeconnection.ActiveConnectionDirectory;
import org.apache.guacamole.morphia.activeconnection.ActiveConnectionPermissionService;
import org.apache.guacamole.morphia.activeconnection.ActiveConnectionPermissionSet;
import org.apache.guacamole.morphia.activeconnection.ActiveConnectionService;
import org.apache.guacamole.morphia.activeconnection.TrackedActiveConnection;
import org.apache.guacamole.morphia.connection.ConnectionDirectory;
import org.apache.guacamole.morphia.connection.ConnectionMapper;
import org.apache.guacamole.morphia.connection.ConnectionMapperImp;
import org.apache.guacamole.morphia.connection.ConnectionParameterMapper;
import org.apache.guacamole.morphia.connection.ConnectionParameterMapperImp;
import org.apache.guacamole.morphia.connection.ConnectionPermissionMapperImp;
import org.apache.guacamole.morphia.connection.ConnectionRecordMapper;
import org.apache.guacamole.morphia.connection.ConnectionRecordMapperImp;
import org.apache.guacamole.morphia.connection.ConnectionService;
import org.apache.guacamole.morphia.connection.ModeledConnection;
import org.apache.guacamole.morphia.connection.ModeledGuacamoleConfiguration;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupDirectory;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupMapper;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupMapperImp;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupPermissionMapperImp;
import org.apache.guacamole.morphia.connectiongroup.ConnectionGroupService;
import org.apache.guacamole.morphia.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.morphia.connectiongroup.RootConnectionGroup;
import org.apache.guacamole.morphia.permission.ConnectionGroupPermissionMapper;
import org.apache.guacamole.morphia.permission.ConnectionGroupPermissionService;
import org.apache.guacamole.morphia.permission.ConnectionGroupPermissionSet;
import org.apache.guacamole.morphia.permission.ConnectionPermissionMapper;
import org.apache.guacamole.morphia.permission.ConnectionPermissionService;
import org.apache.guacamole.morphia.permission.ConnectionPermissionSet;
import org.apache.guacamole.morphia.permission.SharingProfilePermissionMapper;
import org.apache.guacamole.morphia.permission.SharingProfilePermissionService;
import org.apache.guacamole.morphia.permission.SharingProfilePermissionSet;
import org.apache.guacamole.morphia.permission.SystemPermissionMapper;
import org.apache.guacamole.morphia.permission.SystemPermissionMapperImp;
import org.apache.guacamole.morphia.permission.SystemPermissionService;
import org.apache.guacamole.morphia.permission.SystemPermissionSet;
import org.apache.guacamole.morphia.permission.UserPermissionMapper;
import org.apache.guacamole.morphia.permission.UserPermissionService;
import org.apache.guacamole.morphia.permission.UserPermissionSet;
import org.apache.guacamole.morphia.security.PasswordEncryptionService;
import org.apache.guacamole.morphia.security.PasswordPolicyService;
import org.apache.guacamole.morphia.security.PasswordRecordMapperImp;
import org.apache.guacamole.morphia.security.SHA256PasswordEncryptionService;
import org.apache.guacamole.morphia.security.SaltService;
import org.apache.guacamole.morphia.security.SecureRandomSaltService;
import org.apache.guacamole.morphia.sharing.ConnectionSharingService;
import org.apache.guacamole.morphia.sharing.HashSharedConnectionMap;
import org.apache.guacamole.morphia.sharing.SecureRandomShareKeyGenerator;
import org.apache.guacamole.morphia.sharing.ShareKeyGenerator;
import org.apache.guacamole.morphia.sharing.SharedConnectionMap;
import org.apache.guacamole.morphia.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileDirectory;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileMapper;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileMapperImp;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileParameterMapper;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileParameterMapperImp;
import org.apache.guacamole.morphia.sharingprofile.SharingProfilePermissionMapperImp;
import org.apache.guacamole.morphia.sharingprofile.SharingProfileService;
import org.apache.guacamole.morphia.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.morphia.tunnel.RestrictedGuacamoleTunnelService;
import org.apache.guacamole.morphia.user.ModeledUser;
import org.apache.guacamole.morphia.user.ModeledUserContext;
import org.apache.guacamole.morphia.user.PasswordRecordMapper;
import org.apache.guacamole.morphia.user.UserDirectory;
import org.apache.guacamole.morphia.user.UserMapper;
import org.apache.guacamole.morphia.user.UserMapperImp;
import org.apache.guacamole.morphia.user.UserPermissionMapperImp;
import org.apache.guacamole.morphia.user.UserRecordMapper;
import org.apache.guacamole.morphia.user.UserRecordMapperImp;
import org.apache.guacamole.morphia.user.UserService;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module which configures the injections used by the JDBC authentication
 * provider base. This module MUST be included in the Guice injector, or
 * authentication providers based on JDBC will not function.
 */
public class JDBCAuthenticationProviderModule extends AbstractModule {

    /**
     * The environment of the Guacamole server.
     */
    private final JDBCEnvironment environment;

    /**
     * Creates a new JDBC authentication provider module that configures the
     * various injected base classes using the given environment, and provides
     * connections using the given socket service.
     *
     * @param environment
     *            The environment to use to configure injected classes.
     */
    public JDBCAuthenticationProviderModule(JDBCEnvironment environment) {
        this.environment = environment;
    }

    @Override
    protected void configure() {

        // Bind core implementations of guacamole-ext classes
        bind(ActiveConnectionDirectory.class);
        bind(ActiveConnectionPermissionSet.class);
        bind(JDBCEnvironment.class).toInstance(environment);
        bind(ConnectionDirectory.class);
        bind(ConnectionGroupDirectory.class);
        bind(ConnectionGroupPermissionSet.class);
        bind(ConnectionPermissionSet.class);
        bind(ModeledConnection.class);
        bind(ModeledConnectionGroup.class);
        bind(ModeledGuacamoleConfiguration.class);
        bind(ModeledSharingProfile.class);
        bind(ModeledUser.class);
        bind(ModeledUserContext.class);
        bind(RootConnectionGroup.class);
        bind(SharingProfileDirectory.class);
        bind(SharingProfilePermissionSet.class);
        bind(SystemPermissionSet.class);
        bind(TrackedActiveConnection.class);
        bind(UserDirectory.class);
        bind(UserPermissionSet.class);

        // Bind repository
        bind(ConnectionGroupMapper.class).to(ConnectionGroupMapperImp.class);
        bind(ConnectionGroupPermissionMapper.class)
                .to(ConnectionGroupPermissionMapperImp.class);
        bind(ConnectionMapper.class).to(ConnectionMapperImp.class);
        bind(ConnectionParameterMapper.class)
                .to(ConnectionParameterMapperImp.class);
        bind(ConnectionPermissionMapper.class)
                .to(ConnectionPermissionMapperImp.class);
        bind(ConnectionRecordMapper.class).to(ConnectionRecordMapperImp.class);
        bind(PasswordRecordMapper.class).to(PasswordRecordMapperImp.class);
        bind(SharingProfileMapper.class).to(SharingProfileMapperImp.class);
        bind(SharingProfileParameterMapper.class)
                .to(SharingProfileParameterMapperImp.class);
        bind(SharingProfilePermissionMapper.class)
                .to(SharingProfilePermissionMapperImp.class);
        bind(SystemPermissionMapper.class).to(SystemPermissionMapperImp.class);
        bind(UserMapper.class).to(UserMapperImp.class);
        bind(UserPermissionMapper.class).to(UserPermissionMapperImp.class);
        bind(UserRecordMapper.class).to(UserRecordMapperImp.class);

        // Bind services
        bind(ActiveConnectionService.class);
        bind(ActiveConnectionPermissionService.class);
        bind(ConnectionGroupPermissionService.class);
        bind(ConnectionGroupService.class);
        bind(ConnectionPermissionService.class);
        bind(ConnectionSharingService.class);
        bind(ConnectionService.class);
        bind(GuacamoleTunnelService.class)
                .to(RestrictedGuacamoleTunnelService.class);
        bind(PasswordEncryptionService.class)
                .to(SHA256PasswordEncryptionService.class);
        bind(PasswordPolicyService.class);
        bind(SaltService.class).to(SecureRandomSaltService.class);
        bind(SharedConnectionMap.class).to(HashSharedConnectionMap.class)
                .in(Scopes.SINGLETON);
        bind(ShareKeyGenerator.class).to(SecureRandomShareKeyGenerator.class)
                .in(Scopes.SINGLETON);
        bind(SharingProfilePermissionService.class);
        bind(SharingProfileService.class);
        bind(SystemPermissionService.class);
        bind(UserPermissionService.class);
        bind(UserService.class);

    }

}
