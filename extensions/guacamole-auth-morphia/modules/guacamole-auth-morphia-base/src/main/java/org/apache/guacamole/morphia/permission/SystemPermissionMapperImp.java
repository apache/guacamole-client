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

package org.apache.guacamole.morphia.permission;

import java.util.Collection;
import java.util.List;

import org.apache.guacamole.morphia.user.UserModel;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for system-level permissions.
 */
public class SystemPermissionMapperImp implements SystemPermissionMapper {

    @Inject
    private Datastore datastore;

    /**
     * Retrieves all permissions associated with the given user.
     *
     * @param user
     *            The user to retrieve permissions for.
     *
     * @return All permissions associated with the given user.
     */
    @Override
    public Collection<SystemPermissionModel> select(UserModel user) {

        return datastore.createQuery(SystemPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .asList();

    }

    /**
     * Retrieve the permission of the given type associated with the given user,
     * if it exists. If no such permission exists, null is returned.
     *
     * @param user
     *            The user to retrieve permissions for.
     * 
     * @param type
     *            The type of permission to return.
     *
     * @return The requested permission, or null if no such permission is
     *         granted to the given user.
     */
    @Override
    public SystemPermissionModel selectOne(UserModel user,
            SystemPermission.Type type) {

        List<SystemPermissionModel> resultList = (List<SystemPermissionModel>) datastore
                .createQuery(SystemPermissionModel.class).disableValidation()
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal(type).asList();

        return !resultList.isEmpty() ? resultList.get(0) : null;

    }

    /**
     * Inserts the given permissions into the database. If any permissions
     * already exist, they will be ignored.
     *
     * @param permissions
     *            The permissions to insert.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insert(Collection<SystemPermissionModel> types) {

        datastore.save(types);

        return types.size();

    }

    /**
     * Deletes the given permissions from the database. If any permissions do
     * not exist, they will be ignored.
     *
     * @param permissions
     *            The permissions to delete.
     *
     * @return The number of rows deleted.
     */
    @Override
    public int delete(Collection<SystemPermissionModel> types) {

        int counter = 0;
        for (SystemPermissionModel item : types) {
            final Query<SystemPermissionModel> items = datastore
                    .createQuery(SystemPermissionModel.class)
                    .disableValidation().field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(item.getUser().getId()))
                            .asList())
                    .field("type").equal(item.getType());

            datastore.delete(items);
            counter++;
        }

        return counter;

    }

}
