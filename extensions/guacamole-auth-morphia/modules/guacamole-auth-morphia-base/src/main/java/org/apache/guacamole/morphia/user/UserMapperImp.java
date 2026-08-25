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

package org.apache.guacamole.morphia.user;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.google.inject.Inject;

/**
 * Mapper for user objects.
 */
public class UserMapperImp implements UserMapper {

    @Inject
    private Datastore datastore;

    /**
     * Returns the user having the given username, if any. If no such user
     * exists, null is returned.
     *
     * @param username
     *            The username of the user to return.
     *
     * @return The user having the given username, or null if no such user
     *         exists.
     */
    @Override
    public UserModel selectOne(String username) {

        UserModel resultItem = null;

        List<UserModel> itemList = datastore.createQuery(UserModel.class)
                .disableValidation().field("username").equal(username).asList();

        for (UserModel item : itemList) {

            resultItem = item;

            List<UserRecordModel> itemListTemp = datastore
                    .createQuery(UserRecordModel.class).disableValidation()
                    .field("user").equal(item).order("-startDate").asList();

            for (UserRecordModel itemTemp : itemListTemp) {
                if (item.getLastActive() == null) {
                    item.setLastActive(
                            new Timestamp(itemTemp.getStartDate().getTime()));
                    break;
                }
            }

            // arbitraryAttributes
            List<UserAttributeModel> listUserAttribute = datastore
                    .createQuery(UserAttributeModel.class).disableValidation()
                    .field("user").equal(item).asList();

            for (UserAttributeModel item2 : listUserAttribute) {
                item.getArbitraryAttributes().add(item2);
            }

        }

        return resultItem;

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
        List<UserModel> itemList = datastore.createQuery(UserModel.class)
                .disableValidation().asList();

        Set<String> itemSet = new HashSet<String>();

        for (UserModel item : itemList) {
            itemSet.add(item.getUsername());
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
        List<UserPermissionModel> itemList = datastore
                .createQuery(UserPermissionModel.class).disableValidation()
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        Set<String> itemSet = new HashSet<String>();

        for (UserPermissionModel item : itemList) {
            itemSet.add(item.getUser().getUsername());
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
    public Collection<UserModel> select(Collection<String> identifiers) {

        List<UserModel> itemList = datastore.createQuery(UserModel.class)
                .disableValidation().field("username").hasAnyOf(identifiers)
                .asList();

        for (UserModel item : itemList) {

            List<UserRecordModel> itemListTemp = datastore
                    .createQuery(UserRecordModel.class).disableValidation()
                    .field("user").equal(item).order("-startDate").asList();

            for (UserRecordModel itemTemp : itemListTemp) {
                if (item.getLastActive() == null) {
                    item.setLastActive(
                            new Timestamp(itemTemp.getStartDate().getTime()));
                    break;
                }
            }

            // arbitraryAttributes
            List<UserAttributeModel> listUserAttribute = datastore
                    .createQuery(UserAttributeModel.class).disableValidation()
                    .field("user").equal(item).field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("username")
                            .hasAnyOf(identifiers).asList())
                    .asList();

            for (UserAttributeModel item2 : listUserAttribute) {
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
    public Collection<UserModel> selectReadable(UserModel user,
            Collection<String> identifiers) {

        List<UserModel> itemsResult = new ArrayList<UserModel>();

        List<UserPermissionModel> itemList = datastore
                .createQuery(UserPermissionModel.class).disableValidation()
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("username").hasAnyOf(identifiers).asList())
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id").equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        for (UserPermissionModel item : itemList) {

            List<UserRecordModel> itemListTemp = datastore
                    .createQuery(UserRecordModel.class).disableValidation()
                    .field("user").equal(item.getUser()).order("-startDate")
                    .asList();

            for (UserRecordModel itemTemp : itemListTemp) {
                if (item.getUser().getLastActive() == null) {
                    item.getUser().setLastActive(
                            new Timestamp(itemTemp.getStartDate().getTime()));
                    break;
                }
            }

            // arbitraryAttributes
            List<UserPermissionModel> listUserPermission = datastore
                    .createQuery(UserPermissionModel.class).disableValidation()
                    .field("user").equal(item).field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .equal(new ObjectId(user.getId())).asList())
                    .field("type").equal("READ").asList();

            if (!listUserPermission.isEmpty()) {
                List<UserAttributeModel> listUserAttribute = datastore
                        .createQuery(UserAttributeModel.class)
                        .disableValidation().field("user").equal(item)
                        .field("user")
                        .in(datastore.createQuery(UserModel.class)
                                .disableValidation().field("username")
                                .hasAnyOf(identifiers).asList())
                        .asList();

                for (UserAttributeModel item2 : listUserAttribute) {
                    item.getUser().getArbitraryAttributes().add(item2);
                }
            }

            itemsResult.add(item.getUser());

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
    public int insert(UserModel object) {
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
        final Query<UserModel> items = datastore.createQuery(UserModel.class)
                .disableValidation().field("username").equal(identifier);

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
    public int update(UserModel object) {

        UpdateOperations<UserModel> updateOperations = datastore
                .createUpdateOperations(UserModel.class)
                .set("passwordHash", object.getPasswordHash())
                .set("passwordSalt", object.getPasswordSalt())
                .set("passwordDate", object.getPasswordDate())
                .set("disabled", object.isDisabled())
                .set("expired", object.isExpired());

        if (object.getValidFrom() != null) {
            updateOperations.set("validFrom", object.getValidFrom());
        }

        if (object.getValidUntil() != null) {
            updateOperations.set("validUntil", object.getValidUntil());
        }

        if (object.getTimeZone() != null) {
            updateOperations.set("timeZone", object.getTimeZone());
        }

        if (object.getFullName() != null) {
            updateOperations.set("fullName", object.getFullName());
        }

        if (object.getEmailAddress() != null) {
            updateOperations.set("emailAddress", object.getEmailAddress());
        }

        if (object.getAccessWindowStart() != null) {
            updateOperations.set("accessWindowStart",
                    object.getAccessWindowStart());
        }

        if (object.getAccessWindowEnd() != null) {
            updateOperations.set("accessWindowEnd",
                    object.getAccessWindowEnd());
        }

        if (object.getOrganization() != null) {
            updateOperations.set("organization", object.getOrganization());
        }

        if (object.getOrganizationalRole() != null) {
            updateOperations.set("organizationalRole",
                    object.getOrganizationalRole());
        }

        datastore.update(
                datastore.createQuery(UserModel.class).disableValidation()
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
    public int deleteAttributes(UserModel object) {
        final Query<UserAttributeModel> items = datastore
                .createQuery(UserAttributeModel.class).disableValidation()
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
    public int insertAttributes(UserModel object) {
        datastore.save((UserAttributeModel) object.getArbitraryAttributes());

        return object.getArbitraryAttributes().size();

    }

}
