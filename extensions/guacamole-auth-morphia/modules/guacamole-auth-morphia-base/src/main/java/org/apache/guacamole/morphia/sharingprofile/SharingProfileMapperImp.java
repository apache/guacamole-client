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

package org.apache.guacamole.morphia.sharingprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.guacamole.morphia.connection.ConnectionAttributeModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.google.inject.Inject;

/**
 * Mapper for sharing profile objects.
 */
public class SharingProfileMapperImp implements SharingProfileMapper {

    @Inject
    private Datastore datastore;

    /**
     * Selects the sharing profile associated with the given primary connection
     * and having the given name. If no such sharing profile exists, null is
     * returned.
     *
     * @param parentIdentifier
     *            The identifier of the primary connection to search against.
     *
     * @param name
     *            The name of the sharing profile to find.
     *
     * @return The sharing profile having the given name and associated with the
     *         given primary connection, or null if no such sharing profile
     *         exists.
     */
    @Override
    public SharingProfileModel selectOneByName(String parentIdentifier,
            String name) {

        List<SharingProfileModel> resultList = datastore
                .createQuery(SharingProfileModel.class).disableValidation()
                .field("id").equal(new ObjectId(parentIdentifier)).field("name")
                .equal(name).asList();

        return !resultList.isEmpty() ? resultList.get(0) : null;

    }

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

        List<SharingProfileModel> itemList = datastore
                .createQuery(SharingProfileModel.class).disableValidation()
                .asList();

        Set<String> itemSet = new HashSet<String>();

        for (SharingProfileModel item : itemList) {
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
        List<SharingProfilePermissionModel> itemList = datastore
                .createQuery(SharingProfilePermissionModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        Set<String> itemSet = new HashSet<String>();

        for (SharingProfilePermissionModel item : itemList) {
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
    public Collection<SharingProfileModel> select(
            Collection<String> identifiers) {

        List<ObjectId> listIdentifiers = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            if (item != null) {
                listIdentifiers.add(new ObjectId(item));
            }
        }

        List<SharingProfileModel> itemsResult = datastore
                .createQuery(SharingProfileModel.class).disableValidation()
                .field("id").hasAnyOf(listIdentifiers).asList();

        for (SharingProfileModel item : itemsResult) {

            // arbitraryAttributes
            List<SharingProfileAttributeModel> listSharingProfileAttribute = datastore
                    .createQuery(SharingProfileAttributeModel.class)
                    .disableValidation().field("sharingProfile").equal(item)
                    .field("sharingProfile")
                    .in(datastore.createQuery(SharingProfileModel.class)
                            .disableValidation().field("id")
                            .hasAnyOf(listIdentifiers).asList())
                    .asList();

            for (SharingProfileAttributeModel item2 : listSharingProfileAttribute) {
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
    public Collection<SharingProfileModel> selectReadable(UserModel user,
            Collection<String> identifiers) {
        List<SharingProfileModel> itemsResult = new ArrayList<SharingProfileModel>();
        List<ObjectId> listIdentifiers = new ArrayList<ObjectId>();

        for (String item : identifiers) {
            listIdentifiers.add(new ObjectId(item));
        }

        List<SharingProfilePermissionModel> itemList = datastore
                .createQuery(SharingProfilePermissionModel.class)
                .disableValidation().field("sharingProfile")
                .in(datastore.createQuery(SharingProfileModel.class)
                        .disableValidation().field("id")
                        .hasAnyOf(listIdentifiers).asList())
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(user.getId()).asList())
                .field("type").equal("READ").asList();

        for (SharingProfilePermissionModel item : itemList) {

            // arbitraryAttributes
            List<SharingProfilePermissionModel> listSharingProfilePermission = datastore
                    .createQuery(SharingProfilePermissionModel.class)
                    .disableValidation().field("sharingProfile")
                    .equal(item.getSharingProfile()).field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id").equal(user.getId())
                            .asList())
                    .field("type").equal("READ").asList();
            if (!listSharingProfilePermission.isEmpty()) {
                List<SharingProfileAttributeModel> listSharingProfileAttribute = datastore
                        .createQuery(SharingProfileAttributeModel.class)
                        .disableValidation().field("id")
                        .hasAnyOf(listIdentifiers).field("sharingProfile")
                        .equal(item).asList();
                for (SharingProfileAttributeModel item2 : listSharingProfileAttribute) {
                    item.getSharingProfile().getArbitraryAttributes()
                            .add(item2);
                }

            }

            itemsResult.add(item.getSharingProfile());

        }

        return itemsResult;

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
    public int insert(SharingProfileModel object) {
        datastore.save(object);
        return 1;
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
        final Query<SharingProfileModel> items = datastore
                .createQuery(SharingProfileModel.class).disableValidation()
                .field("id").equal(new ObjectId(identifier));

        datastore.delete(items);

        return items.asList().size();

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
    public int update(SharingProfileModel object) {

        UpdateOperations<SharingProfileModel> updateOperations = datastore
                .createUpdateOperations(SharingProfileModel.class)
                .set("name", object.getName());

        if (object.getParent() != null) {
            updateOperations.set("parent", object.getParent());
        }
        else {
            updateOperations.unset("parent");
        }

        datastore.update(datastore.createQuery(SharingProfileModel.class)
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
    public int deleteAttributes(SharingProfileModel object) {
        final Query<SharingProfileAttributeModel> items = datastore
                .createQuery(SharingProfileAttributeModel.class)
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
    public int insertAttributes(SharingProfileModel object) {
        datastore.save(
                (ConnectionAttributeModel) object.getArbitraryAttributes());

        return object.getArbitraryAttributes().size();

    }

}
