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

package org.apache.guacamole.morphia.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.guacamole.morphia.user.PasswordRecordMapper;
import org.apache.guacamole.morphia.user.PasswordRecordModel;
import org.apache.guacamole.morphia.user.UserModel;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for historical password records (users' prior passwords, along with
 * the dates they were set).
 */
@SuppressWarnings("deprecation")
public class PasswordRecordMapperImp implements PasswordRecordMapper {

    @Inject
    private Datastore datastore;

    /**
     * Returns a collection of all password records associated with the user
     * having the given username.
     *
     * @param username
     *            The username of the user whose password records are to be
     *            retrieved.
     *
     * @param maxHistorySize
     *            The maximum number of records to maintain for each user.
     *
     * @return A collection of all password records associated with the user
     *         having the given username. This collection will be empty if no
     *         such user exists.
     */
    @Override
    public List<PasswordRecordModel> select(String username,
            int maxHistorySize) {

        return datastore.createQuery(PasswordRecordModel.class)
                .disableValidation().field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("username").equal(username).asList())
                .order("passwordDate").limit(maxHistorySize).asList();

    }

    /**
     * Inserts the given password record. Old records exceeding the maximum
     * history size will be automatically deleted.
     *
     * @param record
     *            The password record to insert.
     *
     * @param maxHistorySize
     *            The maximum number of records to maintain for each user.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insert(PasswordRecordModel record, int maxHistorySize) {
        datastore.save(record);

        List<PasswordRecordModel> listTemp = datastore
                .createQuery(PasswordRecordModel.class).disableValidation()
                .field("user")
                .in(datastore.createQuery(UserModel.class).disableValidation()
                        .field("id")
                        .equal(new ObjectId(record.getUser().getId())).asList())
                .order("-passwordDate").limit(1).offset(maxHistorySize)
                .asList();

        final Query<PasswordRecordModel> items = datastore
                .createQuery(PasswordRecordModel.class).disableValidation()
                .field("id").equal(new ObjectId(listTemp.get(0).getId()));

        datastore.delete(items);

        return 0;

    }

    @Override
    public Set<String> selectIdentifiers() {
        return null;

    }

    @Override
    public Set<String> selectReadableIdentifiers(UserModel user) {
        return null;

    }

    @Override
    public Collection<UserModel> select(Collection<String> identifiers) {
        return null;

    }

    @Override
    public Collection<UserModel> selectReadable(UserModel user,
            Collection<String> identifiers) {
        return null;

    }

    @Override
    public int insert(UserModel object) {
        return 0;

    }

    @Override
    public int delete(String identifier) {
        return 0;

    }

    @Override
    public int update(UserModel object) {
        return 0;

    }

    @Override
    public int deleteAttributes(UserModel object) {
        return 0;

    }

    @Override
    public int insertAttributes(UserModel object) {
        return 0;

    }

}
