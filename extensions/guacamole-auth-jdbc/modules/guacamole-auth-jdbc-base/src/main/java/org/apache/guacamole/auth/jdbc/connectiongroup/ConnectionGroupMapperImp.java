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

package org.apache.guacamole.auth.jdbc.connectiongroup;

import java.util.Collection;
import java.util.Set;
import org.apache.guacamole.auth.common.connectiongroup.ConnectionGroupMapperInterface;
import org.apache.guacamole.auth.common.user.UserModelInterface;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapperImp;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import com.google.inject.Inject;

/**
 * Mapper for connection group objects.
 */
public class ConnectionGroupMapperImp extends
        ModeledDirectoryObjectMapperImp<ConnectionGroupModel, ConnectionGroupMapper>
        implements ConnectionGroupMapperInterface {

    @Inject
    private ConnectionGroupMapper connectionGroupMapper;

    /**
     * Selects the identifiers of all connection groups within the given parent
     * connection group, regardless of whether they are readable by any
     * particular user. This should only be called on behalf of a system
     * administrator. If identifiers are needed by a non-administrative user who
     * must have explicit read rights, use selectReadableIdentifiersWithin()
     * instead.
     *
     * @param parentIdentifier
     *            The identifier of the parent connection group, or null if the
     *            root connection group is to be queried.
     *
     * @return A Set containing all identifiers of all objects.
     */
    public Set<String> selectIdentifiersWithin(String parentIdentifier) {
        return connectionGroupMapper.selectIdentifiersWithin(parentIdentifier);
    }

    /**
     * Selects the identifiers of all connection groups within the given parent
     * connection group that are explicitly readable by the given user. If
     * identifiers are needed by a system administrator (who, by definition,
     * does not need explicit read rights), use selectIdentifiersWithin()
     * instead.
     *
     * @param user
     *            The user whose permissions should determine whether an
     *            identifier is returned.
     *
     * @param parentIdentifier
     *            The identifier of the parent connection group, or null if the
     *            root connection group is to be queried.
     *
     * @param effectiveGroups
     *            The identifiers of all groups that should be taken into
     *            account when determining the permissions effectively granted
     *            to the user. If no groups are given, only permissions directly
     *            granted to the user will be used.
     * 
     * @return A Set containing all identifiers of all readable objects.
     */
    public Set<String> selectReadableIdentifiersWithin(UserModelInterface user,
            String parentIdentifier, Collection<String> effectiveGroups) {
        return connectionGroupMapper.selectReadableIdentifiersWithin(
                (UserModel) user, parentIdentifier, effectiveGroups);
    }

    /**
     * Selects the connection group within the given parent group and having the
     * given name. If no such connection group exists, null is returned.
     *
     * @param parentIdentifier
     *            The identifier of the parent group to search within.
     *
     * @param name
     *            The name of the connection group to find.
     *
     * @return The connection group having the given name within the given
     *         parent group, or null if no such connection group exists.
     */
    public ConnectionGroupModel selectOneByName(String parentIdentifier,
            String name) {
        return connectionGroupMapper.selectOneByName(parentIdentifier, name);
    }

    @Override
    protected ConnectionGroupMapper getMapper() {
        return connectionGroupMapper;
    }

}
