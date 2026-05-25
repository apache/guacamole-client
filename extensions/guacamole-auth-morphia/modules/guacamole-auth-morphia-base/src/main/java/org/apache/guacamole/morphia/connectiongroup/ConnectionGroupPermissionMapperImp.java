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

package org.apache.guacamole.morphia.connectiongroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.guacamole.morphia.permission.ConnectionGroupPermissionMapper;
import org.apache.guacamole.morphia.permission.ObjectPermissionModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for connection group permissions.
 */
public class ConnectionGroupPermissionMapperImp
        implements ConnectionGroupPermissionMapper {

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
                .createQuery(ConnectionGroupPermissionModel.class)
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

        return datastore.createQuery(ConnectionGroupPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal(type).field("objectIdentifier")
                .equal(identifier).asList().get(0);

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

        List<ConnectionGroupPermissionModel> itemList = datastore
                .createQuery(ConnectionGroupPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("objectIdentifier").hasAnyOf(identifiers).field("type")
                .hasAnyOf(permissions).asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionGroupPermissionModel item : itemList) {
            itemSet.add(item.getObjectIdentifier());
        }

        return itemSet;

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
        datastore.save(permissions);
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

        int counter = 0;
        for (ObjectPermissionModel item : permissions) {
            final Query<ConnectionGroupPermissionModel> items = datastore
                    .createQuery(ConnectionGroupPermissionModel.class)
                    .disableValidation().field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(item.getUser().getId()))
                            .asList())
                    .field("type").equal(item.getType())
                    .field("objectIdentifier")
                    .equal(item.getObjectIdentifier());

            datastore.delete(items);
            counter++;
        }

        return counter;

    }

}
