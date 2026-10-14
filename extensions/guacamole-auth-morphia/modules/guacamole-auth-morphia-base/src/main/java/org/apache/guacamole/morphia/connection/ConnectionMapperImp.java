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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.guacamole.morphia.sharingprofile.SharingProfileModel;
import org.apache.guacamole.morphia.sharingprofile.SharingProfilePermissionModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.google.inject.Inject;

/**
 * Mapper for connection objects.
 */
public class ConnectionMapperImp implements ConnectionMapper {

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
        List<ConnectionModel> itemList = datastore
                .createQuery(ConnectionModel.class).disableValidation()
                .asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionModel item : itemList) {
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
        List<ConnectionPermissionModel> itemList = datastore
                .createQuery(ConnectionPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionPermissionModel item : itemList) {
            itemSet.add(item.getObjectIdentifier());
        }

        return itemSet;

    }

    /**
     * Selects the identifiers of all connections within the given parent
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

        Query<ConnectionModel> query = datastore
                .createQuery(ConnectionModel.class).disableValidation();

        if (parentIdentifier != null) {
            query.field("parent").equal(new ObjectId(parentIdentifier));
        }
        else {
            query.field("parent").equal(null);
        }

        List<ConnectionModel> itemList = query.asList();

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionModel item : itemList) {
            itemSet.add(item.getId());
        }

        return itemSet;

    }

    /**
     * Selects the identifiers of all connections within the given parent
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

        Query<ConnectionPermissionModel> query = datastore
                .createQuery(ConnectionPermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ");

        if (parentIdentifier != null) {
            query.field("connection")
                    .in(datastore.createQuery(ConnectionModel.class)
                            .disableValidation().field("parent")
                            .equal(new ObjectId(parentIdentifier)).asList());
        }
        else {
            query.field("connection")
                    .in(datastore.createQuery(ConnectionModel.class)
                            .disableValidation().field("parent").doesNotExist()
                            .asList());
        }

        Set<String> itemSet = new HashSet<String>();

        for (ConnectionPermissionModel item : query.asList()) {
            itemSet.add(item.getConnection().getId());
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
    public Collection<ConnectionModel> select(Collection<String> identifiers) {

        List<ObjectId> identifiersList = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            identifiersList.add(new ObjectId(item));
        }

        List<ConnectionModel> itemList = datastore
                .createQuery(ConnectionModel.class).disableValidation()
                .field("id").hasAnyOf(identifiersList).asList();

        for (ConnectionModel item : itemList) {

            List<ConnectionRecordModel> itemListTemp = datastore
                    .createQuery(ConnectionRecordModel.class)
                    .disableValidation().field("connection").equal(item)
                    .order("-startDate").asList();

            for (ConnectionRecordModel itemTemp : itemListTemp) {
                if (item.getLastActive() == null) {
                    item.setLastActive(
                            new Timestamp(itemTemp.getStartDate().getTime()));
                    break;
                }
            }

            // sharingProfileIdentifiers
            List<SharingProfileModel> listSharingProfile = datastore
                    .createQuery(SharingProfileModel.class).disableValidation()
                    .field("parent").hasAnyOf(identifiers).asList();

            for (SharingProfileModel item2 : listSharingProfile) {
                item.getSharingProfileIdentifiers().add(item2.getId());
            }

            // arbitraryAttributes
            List<ConnectionAttributeModel> listConnectionAttribute = datastore
                    .createQuery(ConnectionAttributeModel.class)
                    .disableValidation().field("connection").equal(item)
                    .field("connection")
                    .in(datastore.createQuery(ConnectionModel.class)
                            .disableValidation().field("id")
                            .hasAnyOf(identifiersList).asList())
                    .asList();

            for (ConnectionAttributeModel item2 : listConnectionAttribute) {
                item.getArbitraryAttributes().add(item2);
            }

        }

        return itemList;

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
    public Collection<ConnectionModel> selectReadable(UserModel user,
            Collection<String> identifiers) {

        List<ConnectionModel> itemsResult = new ArrayList<ConnectionModel>();
        List<ObjectId> identifiersList = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            identifiersList.add(new ObjectId(item));
        }

        List<ConnectionPermissionModel> itemList = datastore
                .createQuery(ConnectionPermissionModel.class)
                .disableValidation().field("connection")
                .in(datastore
                        .createQuery(ConnectionModel.class).disableValidation()
                        .field("username").hasAnyOf(identifiersList).asList())
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        for (ConnectionPermissionModel item : itemList) {

            List<ConnectionRecordModel> itemListTemp = datastore
                    .createQuery(ConnectionRecordModel.class)
                    .disableValidation().field("connection")
                    .equal(item.getConnection()).order("-startDate").asList();

            for (ConnectionRecordModel itemTemp : itemListTemp) {
                if (item.getConnection().getLastActive() == null) {
                    item.getConnection().setLastActive(
                            new Timestamp(itemTemp.getStartDate().getTime()));
                    break;
                }
            }

            // sharingProfileIdentifiers
            List<SharingProfilePermissionModel> listConnectionPermission = datastore
                    .createQuery(SharingProfilePermissionModel.class)
                    .disableValidation().field("id")
                    .equal(item.getConnection().getParent())
                    .field("sharingProfile")
                    .in(datastore.createQuery(SharingProfileModel.class)
                            .disableValidation().field("connection")
                            .in(datastore.createQuery(ConnectionModel.class)
                                    .disableValidation().field("id")
                                    .hasAnyOf(identifiersList).asList())
                            .field("user")
                            .in(datastore.createQuery(UserModel.class)
                                    .disableValidation().field("id")
                                    .equal(new ObjectId(user.getId())).asList())
                            .field("type").equal("READ"))
                    .asList();

            for (SharingProfilePermissionModel item2 : listConnectionPermission) {
                item.getConnection().getSharingProfileIdentifiers()
                        .add(item2.getSharingProfile().getId());
            }

            // arbitraryAttributes
            List<ConnectionPermissionModel> listConnectionPermission2 = datastore
                    .createQuery(ConnectionPermissionModel.class)
                    .disableValidation().field("connection").equal(item)
                    .field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(user.getId())).asList())
                    .field("type").equal("READ").asList();

            if (!listConnectionPermission2.isEmpty()) {
                List<ConnectionAttributeModel> listConnectionAttribute = datastore
                        .createQuery(ConnectionAttributeModel.class)
                        .disableValidation().field("id")
                        .hasAnyOf(identifiersList).field("connection")
                        .equal(item).asList();

                for (ConnectionAttributeModel item2 : listConnectionAttribute) {
                    item.getConnection().getArbitraryAttributes().add(item2);
                }
            }

            itemsResult.add(item.getConnection());
        }

        return itemsResult;

    }

    /**
     * Selects the connection within the given parent group and having the given
     * name. If no such connection exists, null is returned.
     *
     * @param parentIdentifier
     *            The identifier of the parent group to search within.
     *
     * @param name
     *            The name of the connection to find.
     *
     * @return The connection having the given name within the given parent
     *         group, or null if no such connection exists.
     */
    @Override
    public ConnectionModel selectOneByName(String parentIdentifier,
            String name) {

        Query<ConnectionModel> query = datastore
                .createQuery(ConnectionModel.class).disableValidation();

        if (parentIdentifier != null) {
            query.field("parent").equal(new ObjectId(parentIdentifier));
        }
        else {
            query.field("parent").doesNotExist();
        }

        query.field("name").equal(name);

        return !query.asList().isEmpty() ? query.asList().get(0) : null;

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
        final Query<ConnectionModel> query = datastore
                .createQuery(ConnectionModel.class).disableValidation()
                .field("id").equal(new ObjectId(identifier));

        datastore.delete(query);

        // Update connection history
        UpdateOperations<ConnectionRecordModel> updateOperations = datastore
                .createUpdateOperations(ConnectionRecordModel.class);

        Query<ConnectionRecordModel> queryTemp = datastore
                .createQuery(ConnectionRecordModel.class).disableValidation()
                .field("connection").in(query);
        if (queryTemp.asList() != null) {
            updateOperations.unset("connection");
        }

        datastore.update(queryTemp, updateOperations);

        return query.asList().size();

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
    public int insert(ConnectionModel object) {
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
    public int update(ConnectionModel object) {

        UpdateOperations<ConnectionModel> updateOperations = datastore
                .createUpdateOperations(ConnectionModel.class)
                .set("name", object.getName())
                .set("protocol", object.getProtocol())
                .set("failoverOnly", object.isFailoverOnly());

        if (object.getParent() != null) {
            updateOperations.set("parent", object.getParent());
        }
        else {
            updateOperations.unset("parent");
        }

        if (object.getMaxConnections() != null) {
            updateOperations.set("maxConnections", object.getMaxConnections());
        }

        if (object.getMaxConnectionsPerUser() != null) {
            updateOperations.set("maxConnectionsPerUser",
                    object.getMaxConnectionsPerUser());
        }

        if (object.getProxyHostname() != null) {
            updateOperations.set("proxyHostname", object.getProxyHostname());
        }

        if (object.getProxyPort() != null) {
            updateOperations.set("proxyPort", object.getProxyPort());
        }

        if (object.getProxyEncryptionMethod() != null) {
            updateOperations.set("proxyEncryptionMethod",
                    object.getProxyEncryptionMethod());
        }

        if (object.getConnectionWeight() != null) {
            updateOperations.set("connectionWeight",
                    object.getConnectionWeight());
        }

        datastore.update(
                datastore.createQuery(ConnectionModel.class).disableValidation()
                        .field("id").equal(new ObjectId(object.getId())),
                updateOperations);

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
    public int deleteAttributes(ConnectionModel object) {

        final Query<ConnectionAttributeModel> items = datastore
                .createQuery(ConnectionAttributeModel.class).disableValidation()
                .field("id").equal(new ObjectId(object.getId()));

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
    public int insertAttributes(ConnectionModel object) {
        datastore.save(
                (ConnectionAttributeModel) object.getArbitraryAttributes());

        return object.getArbitraryAttributes().size();

    }

}
