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

import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionDirectory;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionPermissionService;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionPermissionSet;
import org.apache.guacamole.auth.jdbc.activeconnection.ActiveConnectionService;
import org.apache.guacamole.auth.jdbc.activeconnection.TrackedActiveConnection;
import org.apache.guacamole.auth.jdbc.base.EntityMapper;
import org.apache.guacamole.auth.jdbc.base.EntityMapperInterface;
import org.apache.guacamole.auth.jdbc.base.EntityMapperImp;
import org.apache.guacamole.auth.jdbc.base.EntityService;
import org.apache.guacamole.auth.jdbc.base.EntityServiceInterface;
import org.apache.guacamole.auth.jdbc.connection.ConnectionDirectory;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapperInterface;
import org.apache.guacamole.auth.jdbc.connection.ConnectionMapperImp;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterMapperInterface;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterMapperImp;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordMapper;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordMapperInterface;
import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordMapperImp;
import org.apache.guacamole.auth.jdbc.connection.ConnectionService;
import org.apache.guacamole.auth.jdbc.connection.ConnectionServiceInterface;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
import org.apache.guacamole.auth.jdbc.connection.ModeledGuacamoleConfiguration;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupDirectory;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupDirectoryInterface;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupMapper;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupMapperInterface;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupMapperImp;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupService;
import org.apache.guacamole.auth.jdbc.connectiongroup.ConnectionGroupServiceInterface;
import org.apache.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;
import org.apache.guacamole.auth.jdbc.connectiongroup.RootConnectionGroup;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionMapperImp;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionService;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionServiceInterface;
import org.apache.guacamole.auth.jdbc.permission.ConnectionGroupPermissionSet;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionMapperImp;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionService;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionServiceInterface;
import org.apache.guacamole.auth.jdbc.permission.ConnectionPermissionSet;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionMapperImp;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionService;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionServiceInterface;
import org.apache.guacamole.auth.jdbc.permission.SharingProfilePermissionSet;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionMapperImp;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionService;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionServiceInterface;
import org.apache.guacamole.auth.jdbc.permission.SystemPermissionSet;
import org.apache.guacamole.auth.jdbc.permission.UserGroupPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.UserGroupPermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.permission.UserGroupPermissionMapperImp;
import org.apache.guacamole.auth.jdbc.permission.UserGroupPermissionService;
import org.apache.guacamole.auth.jdbc.permission.UserGroupPermissionServiceInterface;
import org.apache.guacamole.auth.jdbc.permission.UserGroupPermissionSet;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionMapper;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionMapperInterface;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionMapperImp;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionService;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionServiceInterface;
import org.apache.guacamole.auth.jdbc.permission.UserPermissionSet;
import org.apache.guacamole.auth.jdbc.security.PasswordEncryptionService;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicyService;
import org.apache.guacamole.auth.jdbc.security.PasswordRecordMapper;
import org.apache.guacamole.auth.jdbc.security.PasswordRecordMapperInterface;
import org.apache.guacamole.auth.jdbc.security.PasswordRecordMapperImp;
import org.apache.guacamole.auth.jdbc.security.SHA256PasswordEncryptionService;
import org.apache.guacamole.auth.jdbc.security.SaltService;
import org.apache.guacamole.auth.jdbc.security.SecureRandomSaltService;
import org.apache.guacamole.auth.jdbc.sharing.ConnectionSharingService;
import org.apache.guacamole.auth.jdbc.sharing.HashSharedConnectionMap;
import org.apache.guacamole.auth.jdbc.sharing.SecureRandomShareKeyGenerator;
import org.apache.guacamole.auth.jdbc.sharing.ShareKeyGenerator;
import org.apache.guacamole.auth.jdbc.sharing.SharedConnectionMap;
import org.apache.guacamole.auth.jdbc.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileDirectory;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileDirectoryInterface;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileMapper;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileMapperInterface;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileMapperImp;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterMapper;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterMapperInterface;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterMapperImp;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileService;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileServiceInterface;
import org.apache.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.auth.jdbc.tunnel.RestrictedGuacamoleTunnelService;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUserContext;
import org.apache.guacamole.auth.jdbc.user.ModeledUserContextInterface;
import org.apache.guacamole.auth.jdbc.user.ModeledUserInterface;
import org.apache.guacamole.auth.jdbc.user.UserDirectory;
import org.apache.guacamole.auth.jdbc.user.UserDirectoryInterface;
import org.apache.guacamole.auth.jdbc.user.UserMapper;
import org.apache.guacamole.auth.jdbc.user.UserMapperInterface;
import org.apache.guacamole.auth.jdbc.user.UserMapperImp;
import org.apache.guacamole.auth.jdbc.user.UserModelInterface;
import org.apache.guacamole.auth.jdbc.user.UserParentUserGroupMapper;
import org.apache.guacamole.auth.jdbc.user.UserParentUserGroupMapperInterface;
import org.apache.guacamole.auth.jdbc.user.UserParentUserGroupMapperImp;
import org.apache.guacamole.auth.jdbc.user.UserRecordMapper;
import org.apache.guacamole.auth.jdbc.user.UserRecordMapperInterface;
import org.apache.guacamole.auth.jdbc.user.UserRecordMapperImp;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.auth.jdbc.user.UserServiceInterface;
import org.apache.guacamole.auth.jdbc.usergroup.ModeledUserGroup;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupDirectory;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupDirectoryInterface;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMapper;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMapperInterface;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMapperImp;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMemberUserGroupMapper;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMemberUserGroupMapperInterface;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMemberUserGroupMapperImp;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMemberUserMapper;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMemberUserMapperInterface;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupMemberUserMapperImp;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupModelInterface;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupParentUserGroupMapper;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupParentUserGroupMapperInterface;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupParentUserGroupMapperImp;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupService;
import org.apache.guacamole.auth.jdbc.usergroup.UserGroupServiceInterface;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.builtin.PooledDataSourceProvider;

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

/**
 * Guice module which configures the injections used by the JDBC authentication
 * provider base. This module MUST be included in the Guice injector, or
 * authentication providers based on JDBC will not function.
 */
public class JDBCAuthenticationProviderModule extends MyBatisModule {

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
     *     The environment to use to configure injected classes.
     */
    public JDBCAuthenticationProviderModule(JDBCEnvironment environment) {
        this.environment = environment;
    }

    @Override
    protected void initialize() {
        
        // Datasource
        bindDataSourceProviderType(PooledDataSourceProvider.class);
        
        // Transaction factory
        bindTransactionFactoryType(JdbcTransactionFactory.class);
        
        bind(ConnectionMapperInterface.class).to(ConnectionMapperImp.class);
        bind(ConnectionGroupMapperInterface.class).to(ConnectionGroupMapperImp.class);
        bind(ConnectionGroupPermissionMapperInterface.class).to(ConnectionGroupPermissionMapperImp.class);
        bind(ConnectionPermissionMapperInterface.class).to(ConnectionPermissionMapperImp.class);
        bind(ConnectionRecordMapperInterface.class).to(ConnectionRecordMapperImp.class);
        bind(ConnectionParameterMapperInterface.class).to(ConnectionParameterMapperImp.class);
        bind(EntityMapperInterface.class).to(EntityMapperImp.class);
        bind(PasswordRecordMapperInterface.class).to(PasswordRecordMapperImp.class);
        bind(SharingProfileMapperInterface.class).to(SharingProfileMapperImp.class);
        bind(SharingProfileParameterMapperInterface.class).to(SharingProfileParameterMapperImp.class);
        bind(SharingProfilePermissionMapperInterface.class).to(SharingProfilePermissionMapperImp.class);
        bind(SystemPermissionMapperInterface.class).to(SystemPermissionMapperImp.class);
        bind(new TypeLiteral<UserMapperInterface<UserModelInterface>>(){}).to(UserMapperImp.class);
        bind(UserPermissionMapperInterface.class).to(UserPermissionMapperImp.class);
        bind(UserRecordMapperInterface.class).to(UserRecordMapperImp.class);
        bind(new TypeLiteral<UserGroupMapperInterface<UserGroupModelInterface>>(){}).to(UserGroupMapperImp.class);
        bind(UserGroupPermissionMapperInterface.class).to(UserGroupPermissionMapperImp.class);
        bind(new TypeLiteral<UserParentUserGroupMapperInterface<UserModelInterface>>(){}).to(UserParentUserGroupMapperImp.class);
        bind(new TypeLiteral<UserGroupMemberUserMapperInterface<UserGroupModelInterface>>(){}).to(UserGroupMemberUserMapperImp.class);
        bind(new TypeLiteral<UserGroupMemberUserGroupMapperInterface<UserGroupModelInterface>>(){}).to(UserGroupMemberUserGroupMapperImp.class);
        bind(new TypeLiteral<UserGroupParentUserGroupMapperInterface<UserGroupModelInterface>>(){}).to(UserGroupParentUserGroupMapperImp.class);
        		
        // Add MyBatis mappers
        addMapperClass(ConnectionMapper.class);
        addMapperClass(ConnectionGroupMapper.class);
        addMapperClass(ConnectionGroupPermissionMapper.class);
        addMapperClass(ConnectionPermissionMapper.class);
        addMapperClass(ConnectionRecordMapper.class);
        addMapperClass(ConnectionParameterMapper.class);
        addMapperClass(EntityMapper.class);
        addMapperClass(PasswordRecordMapper.class);
        addMapperClass(SystemPermissionMapper.class);
        addMapperClass(SharingProfileMapper.class);
        addMapperClass(SharingProfileParameterMapper.class);
        addMapperClass(SharingProfilePermissionMapper.class);
        addMapperClass(UserGroupMapper.class);
        addMapperClass(UserGroupMemberUserGroupMapper.class);
        addMapperClass(UserGroupMemberUserMapper.class);
        addMapperClass(UserGroupParentUserGroupMapper.class);
        addMapperClass(UserGroupPermissionMapper.class);
        addMapperClass(UserMapper.class);
        addMapperClass(UserParentUserGroupMapper.class);
        addMapperClass(UserPermissionMapper.class);
        addMapperClass(UserRecordMapper.class);
        
        // Bind core implementations of guacamole-ext classes
        bind(ActiveConnectionDirectory.class);
        bind(ActiveConnectionPermissionSet.class);
        bind(JDBCEnvironment.class).toInstance(environment);
        bind(ConnectionDirectory.class);
        bind(ConnectionGroupDirectoryInterface.class).to(ConnectionGroupDirectory.class);
        bind(ConnectionGroupPermissionSet.class);
        bind(ConnectionPermissionSet.class);
        bind(ModeledConnection.class);
        bind(ModeledConnectionGroup.class);
        bind(ModeledGuacamoleConfiguration.class);
        bind(ModeledSharingProfile.class);
        bind(ModeledUserInterface.class).to(ModeledUser.class);
        bind(ModeledUserContextInterface.class).to(ModeledUserContext.class);
        bind(ModeledUserGroup.class);
        bind(RootConnectionGroup.class);
        bind(SharingProfileDirectoryInterface.class).to(SharingProfileDirectory.class);
        bind(SharingProfilePermissionSet.class);
        bind(SystemPermissionSet.class);
        bind(TrackedActiveConnection.class);
        bind(UserDirectoryInterface.class).to(UserDirectory.class);
        bind(UserGroupDirectoryInterface.class).to(UserGroupDirectory.class);
        bind(UserGroupPermissionSet.class);
        bind(UserPermissionSet.class);
        
        // Bind services
        bind(ActiveConnectionService.class);
        bind(ActiveConnectionPermissionService.class);
        bind(ConnectionGroupPermissionServiceInterface.class).to(ConnectionGroupPermissionService.class);
        bind(ConnectionGroupServiceInterface.class).to(ConnectionGroupService.class);
        bind(ConnectionPermissionServiceInterface.class).to(ConnectionPermissionService.class);
        bind(ConnectionSharingService.class);
        bind(ConnectionServiceInterface.class).to(ConnectionService.class);
        bind(EntityServiceInterface.class).to(EntityService.class);
        bind(GuacamoleTunnelService.class).to(RestrictedGuacamoleTunnelService.class);
        bind(PasswordEncryptionService.class).to(SHA256PasswordEncryptionService.class);
        bind(PasswordPolicyService.class);
        bind(SaltService.class).to(SecureRandomSaltService.class);
        bind(SharedConnectionMap.class).to(HashSharedConnectionMap.class).in(Scopes.SINGLETON);
        bind(ShareKeyGenerator.class).to(SecureRandomShareKeyGenerator.class).in(Scopes.SINGLETON);
        bind(SharingProfilePermissionServiceInterface.class).to(SharingProfilePermissionService.class);
        bind(SharingProfileServiceInterface.class).to(SharingProfileService.class);
        bind(SystemPermissionServiceInterface.class).to(SystemPermissionService.class);
        bind(UserGroupServiceInterface.class).to(UserGroupService.class);
        bind(UserGroupPermissionServiceInterface.class).to(UserGroupPermissionService.class);
        bind(UserPermissionServiceInterface.class).to(UserPermissionService.class);
        bind(UserServiceInterface.class).to(UserService.class);
        
    }

}
