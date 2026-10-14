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

package org.apache.guacamole.morphia.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.guacamole.morphia.permission.ConnectionPermissionMapper;
import org.apache.guacamole.morphia.permission.ObjectPermissionModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for connection permissions.
 */
public class ConnectionPermissionMapperImp
        implements ConnectionPermissionMapper {

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
    public Collection<ObjectPermissionModel> select(UserModel user) {

        return new ArrayList<ObjectPermissionModel>(datastore
                .createQuery(ConnectionPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .asList());

    }

    /**
     * Retrieve the permission of the given type associated with the given user
     * and object, if it exists. If no such permission exists, null is returned.
     *
     * @param user
     *            The user to retrieve permissions for.
     * 
     * @param type
     *            The type of permission to return.
     * 
     * @param identifier
     *            The identifier of the object affected by the permission to
     *            return.
     *
     * @return The requested permission, or null if no such permission is
     *         granted to the given user for the given object.
     */
    @Override
    public ObjectPermissionModel selectOne(UserModel user, Type type,
            String identifier) {

        return datastore.createQuery(ConnectionPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal(type).field("id")
                .equal(new ObjectId(identifier)).asList().get(0);

    }

    /**
     * Retrieves the subset of the given identifiers for which the given user
     * has at least one of the given permissions.
     *
     * @param user
     *            The user to check permissions of.
     *
     * @param permissions
     *            The permissions to check. An identifier will be included in
     *            the resulting collection if at least one of these permissions
     *            is granted for the associated object
     *
     * @param identifiers
     *            The identifiers of the objects affected by the permissions
     *            being checked.
     *
     * @return A collection containing the subset of identifiers for which at
     *         least one of the specified permissions is granted.
     */
    @Override
    public Collection<String> selectAccessibleIdentifiers(UserModel user,
            Collection<Type> permissions, Collection<String> identifiers) {

        List<ObjectId> identifiersList = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            identifiersList.add(new ObjectId(item));
        }

        List<ConnectionPermissionModel> itemList = datastore
                .createQuery(ConnectionPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").hasAnyOf(permissions).field("id")
                .hasAnyOf(identifiersList).asList();

        List<String> resultList = new ArrayList<String>();

        for (ConnectionPermissionModel item : itemList) {
            resultList.add(item.getObjectIdentifier());
        }

        return resultList;

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
    public int insert(Collection<ObjectPermissionModel> permissions) {
        datastore.save((List<ObjectPermissionModel>) permissions);

        return permissions.size();

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
    public int delete(Collection<ObjectPermissionModel> permissions) {
        int count = 0;

        for (ObjectPermissionModel item : permissions) {
            final Query<ConnectionPermissionModel> items = datastore
                    .createQuery(ConnectionPermissionModel.class)
                    .disableValidation().field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(item.getId())).asList())
                    .field("type").equal(item.getType()).field("connection")
                    .in(datastore.createQuery(ConnectionModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(item.getId())).asList());

            datastore.delete(items);

            count += items.asList().size();
        }

        return count;

    }

}