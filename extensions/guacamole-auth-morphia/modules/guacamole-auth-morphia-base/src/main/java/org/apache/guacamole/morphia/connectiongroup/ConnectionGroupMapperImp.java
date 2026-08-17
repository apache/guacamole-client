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

import org.apache.guacamole.morphia.connection.ConnectionModel;
import org.apache.guacamole.morphia.connection.ConnectionPermissionModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.google.inject.Inject;

/**
 * Mapper for connection group objects.
 */
public class ConnectionGroupMapperImp implements ConnectionGroupMapper {

    @Inject
    private Datastore datastore;

    /**
     * Selects the identifiers of all objects, regardless of whether they are
     * readable by any particular user. This should only be called on behalf of
     * a system administrator. If identifiers are needed by a non-
     * administrative user who must have explicit read rights, use
     * selectReadableIdentifiers() instead.
     *
     * @return A Set containing all identifiers of all objects.
     */
    @Override
    public Set<String> selectIdentifiers() {
        List<ConnectionGroupModel> itemList = datastore
                .createQuery(ConnectionGroupModel.class).disableValidation()
                .asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionGroupModel item : itemList) {
            itemSet.add(item.getId());
        }

        return itemSet;

    }

    /**
     * Selects the identifiers of all objects that are explicitly readable by
     * the given user. If identifiers are needed by a system administrator (who,
     * by definition, does not need explicit read rights), use
     * selectIdentifiers() instead.
     *
     * @param user
     *            The user whose permissions should determine whether an
     *            identifier is returned.
     *
     * @return A Set containing all identifiers of all readable objects.
     */
    @Override
    public Set<String> selectReadableIdentifiers(UserModel user) {

        List<ConnectionGroupPermissionModel> itemList = datastore
                .createQuery(ConnectionGroupPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionGroupPermissionModel item : itemList) {
            itemSet.add(item.getObjectIdentifier().toString());
        }

        return itemSet;

    }

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
    @Override
    public Set<String> selectIdentifiersWithin(String parentIdentifier) {

        Query<ConnectionGroupModel> query = datastore
                .createQuery(ConnectionGroupModel.class).disableValidation();

        if (parentIdentifier != null) {
            query.field("parent").equal(parentIdentifier);
        }
        else {
            query.field("parent").equal(null);
        }

        List<ConnectionGroupModel> itemList = query.asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionGroupModel item : itemList) {
            itemSet.add(item.getId());
        }

        return itemSet;

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
     * @return A Set containing all identifiers of all readable objects.
     */
    @Override
    public Set<String> selectReadableIdentifiersWithin(UserModel user,
            String parentIdentifier) {

        Query<ConnectionGroupPermissionModel> query = datastore
                .createQuery(ConnectionGroupPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ");

        if (parentIdentifier != null) {
            query.field("connectionGroup")
                    .in(datastore.createQuery(ConnectionGroupModel.class)
                            .disableValidation().field("parent")
                            .equal(parentIdentifier).asList());
        }
        else {
            query.field("connectionGroup")
                    .in(datastore.createQuery(ConnectionGroupModel.class)
                            .disableValidation().field("parent").equal(null)
                            .asList());
        }

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionGroupPermissionModel item : query.asList()) {
            itemSet.add(item.getId());
        }

        return itemSet;

    }

    /**
     * Selects all objects which have the given identifiers. If an identifier
     * has no corresponding object, it will be ignored. This should only be
     * called on behalf of a system administrator. If objects are needed by a
     * non-administrative user who must have explicit read rights, use
     * selectReadable() instead.
     *
     * @param identifiers
     *            The identifiers of the objects to return.
     *
     * @return A Collection of all objects having the given identifiers.
     */
    @Override
    public Collection<ConnectionGroupModel> select(
            Collection<String> identifiers) {

        List<ObjectId> identifiersList = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            identifiersList.add(new ObjectId(item));
        }

        List<ConnectionGroupModel> itemsResult = new ArrayList<ConnectionGroupModel>();
        itemsResult.addAll(datastore.createQuery(ConnectionGroupModel.class)
                .disableValidation().field("id").hasAnyOf(identifiersList)
                .asList());

        for (ConnectionGroupModel item : itemsResult) {

            // connectionGroupIdentifiers
            List<ConnectionGroupModel> listConnectionGroup = datastore
                    .createQuery(ConnectionGroupModel.class).disableValidation()
                    .field("parent").hasAnyOf(identifiers).field("parent")
                    .equal(item.getId().toString()).asList();

            for (ConnectionGroupModel item2 : listConnectionGroup) {
                item.getConnectionGroupIdentifiers().add(item2.getId());
            }

            // connectionIdentifiers
            List<ConnectionModel> listConnection = datastore
                    .createQuery(ConnectionModel.class).disableValidation()
                    .field("parent").hasAnyOf(identifiers).field("parent")
                    .equal(item.getId().toString()).asList();

            for (ConnectionModel item2 : listConnection) {
                item.getConnectionIdentifiers().add(item2.getId());
            }

            // arbitraryAttributes
            List<ConnectionGroupAttributeModel> listConnectionGroupAttribute = datastore
                    .createQuery(ConnectionGroupAttributeModel.class)
                    .disableValidation().field("connectionGroup")
                    .in(datastore.createQuery(ConnectionGroupModel.class)
                            .disableValidation().field("id").equal(item.getId())
                            .field("id").hasAnyOf(identifiersList).asList())
                    .asList();

            for (ConnectionGroupAttributeModel item2 : listConnectionGroupAttribute) {
                item.getArbitraryAttributes().add(item2);
            }
        }

        return itemsResult;

    }

    /**
     * Selects all objects which have the given identifiers and are explicitly
     * readably by the given user. If an identifier has no corresponding object,
     * or the corresponding object is unreadable, it will be ignored. If objects
     * are needed by a system administrator (who, by definition, does not need
     * explicit read rights), use select() instead.
     *
     * @param user
     *            The user whose permissions should determine whether an object
     *            is returned.
     *
     * @param identifiers
     *            The identifiers of the objects to return.
     *
     * @return A Collection of all objects having the given identifiers.
     */
    @Override
    public Collection<ConnectionGroupModel> selectReadable(UserModel user,
            Collection<String> identifiers) {

        List<ConnectionGroupModel> itemsResult = new ArrayList<ConnectionGroupModel>();
        List<ObjectId> identifiersList = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            identifiersList.add(new ObjectId(item));
        }

        List<ConnectionGroupPermissionModel> itemList = datastore
                .createQuery(ConnectionGroupPermissionModel.class)
                .disableValidation().field("connectionGroup")
                .in(datastore.createQuery(ConnectionGroupModel.class)
                        .disableValidation().field("id")
                        .hasAnyOf(identifiersList).asList())
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        for (ConnectionGroupPermissionModel item : itemList) {

            // connectionGroupIdentifiers
            List<ConnectionGroupPermissionModel> listConnectionGroupPermission = datastore
                    .createQuery(ConnectionGroupPermissionModel.class)
                    .disableValidation().field("connectionGroup")
                    .in(datastore.createQuery(ConnectionGroupModel.class)
                            .disableValidation().field("parent")
                            .equal(item.getConnectionGroup().getParent())
                            .field("parent").hasAnyOf(identifiersList).asList())
                    .field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(user.getId())).asList())
                    .field("type").equal("READ").asList();

            for (ConnectionGroupPermissionModel item2 : listConnectionGroupPermission) {
                item.getConnectionGroup().getConnectionGroupIdentifiers()
                        .add(item2.getConnectionGroup().getId());
            }

            // connectionIdentifiers
            List<ConnectionPermissionModel> listConnectionPermission = datastore
                    .createQuery(ConnectionPermissionModel.class)
                    .disableValidation().field("connectionGroup")
                    .in(datastore.createQuery(ConnectionModel.class)
                            .disableValidation().field("parent")
                            .equal(item.getConnectionGroup().getParent())
                            .field("parent").hasAnyOf(identifiersList).asList())
                    .field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(user.getId())).asList())
                    .field("type").equal("READ").asList();

            for (ConnectionPermissionModel item2 : listConnectionPermission) {
                item.getConnectionGroup().getConnectionIdentifiers()
                        .add(item2.getConnection().getId());
            }

            // arbitraryAttributes
            List<ConnectionGroupPermissionModel> listConnectionGroupPermission2 = datastore
                    .createQuery(ConnectionGroupPermissionModel.class)
                    .disableValidation().field("connectionGroup").equal(item)
                    .field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(user.getId())).asList())
                    .field("type").equal("READ").asList();

            if (!listConnectionGroupPermission2.isEmpty()) {
                List<ConnectionGroupAttributeModel> listConnectionGroupAttribute = datastore
                        .createQuery(ConnectionGroupAttributeModel.class)
                        .disableValidation().field("id")
                        .hasAnyOf(identifiersList).field("connectionGroup")
                        .equal(item).asList();

                for (ConnectionGroupAttributeModel item2 : listConnectionGroupAttribute) {
                    item.getConnectionGroup().getArbitraryAttributes()
                            .add(item2);
                }

            }

            itemsResult.add(item.getConnectionGroup());
        }

        return itemsResult;

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
    @Override
    public ConnectionGroupModel selectOneByName(String parentIdentifier,
            String name) {

        Query<ConnectionGroupModel> query = datastore
                .createQuery(ConnectionGroupModel.class).disableValidation();

        if (parentIdentifier != null) {
            query.field("parent").equal(new ObjectId(parentIdentifier));
        }
        else {
            query.field("parent").equal(null);
        }

        query.field("name").equal(name);

        List<ConnectionGroupModel> itemList = query.asList();

        return itemList.size() > 0 ? itemList.get(0) : null;

    }

    /**
     * Deletes the given object into the database. If the object does not exist,
     * this operation has no effect.
     *
     * @param identifier
     *            The identifier of the object to delete.
     *
     * @return The number of rows deleted.
     */
    @Override
    public int delete(String identifier) {
        final Query<ConnectionGroupModel> items = datastore
                .createQuery(ConnectionGroupModel.class).disableValidation()
                .field("id").equal(new ObjectId(identifier));

        datastore.delete(items);

        return items.asList().size();

    }

    /**
     * Inserts the given object into the database. If the object already exists,
     * this will result in an error.
     *
     * @param object
     *            The object to insert.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insert(ConnectionGroupModel object) {
        datastore.save(object);
        return 1;

    }

    /**
     * Updates the given existing object in the database. If the object does not
     * actually exist, this operation has no effect.
     *
     * @param object
     *            The object to update.
     *
     * @return The number of rows updated.
     */
    @Override
    public int update(ConnectionGroupModel object) {

        UpdateOperations<ConnectionGroupModel> updateOperations = datastore
                .createUpdateOperations(ConnectionGroupModel.class)
                .set("sessionAffinityEnabled",
                        object.isSessionAffinityEnabled());

        if (object.getName() != null) {
            updateOperations.set("name", object.getName());
        }

        if (object.getParent() != null) {
            updateOperations.set("parent", object.getParent());
        }
        else {
            updateOperations.unset("parent");
        }

        if (object.getType() != null) {
            updateOperations.set("type", object.getType());
        }

        if (object.getMaxConnections() != null) {
            updateOperations.set("maxConnections", object.getMaxConnections());
        }

        if (object.getMaxConnectionsPerUser() != null) {
            updateOperations.set("maxConnectionsPerUser",
                    object.getMaxConnectionsPerUser());
        }

        datastore.update(datastore.createQuery(ConnectionGroupModel.class)
                .disableValidation().field("id")
                .equal(new ObjectId(object.getId())), updateOperations);

        return 1;

    }

    /**
     * Deletes any arbitrary attributes currently associated with the given
     * object in the database.
     *
     * @param object
     *            The object whose arbitrary attributes should be deleted.
     *
     * @return The number of rows deleted.
     */
    @Override
    public int deleteAttributes(ConnectionGroupModel object) {

        final Query<ConnectionGroupAttributeModel> items = datastore
                .createQuery(ConnectionGroupAttributeModel.class)
                .disableValidation().field("id")
                .equal(new ObjectId(object.getId()));

        datastore.delete(items);

        return items.asList().size();

    }

    /**
     * Inserts all arbitrary attributes associated with the given object.
     *
     * @param object
     *            The object whose arbitrary attributes should be inserted.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insertAttributes(ConnectionGroupModel object) {
        datastore.save((ConnectionGroupAttributeModel) object
                .getArbitraryAttributes());

        return object.getArbitraryAttributes().size();

    }

}
