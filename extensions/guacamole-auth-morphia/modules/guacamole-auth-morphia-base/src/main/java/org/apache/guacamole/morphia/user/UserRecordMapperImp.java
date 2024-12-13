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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.guacamole.morphia.base.ActivityRecordModel;
import org.apache.guacamole.morphia.base.ActivityRecordSearchTerm;
import org.apache.guacamole.morphia.base.ActivityRecordSortPredicate;
import org.apache.guacamole.net.auth.ActivityRecordSet;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for user login activity records.
 */
@SuppressWarnings("deprecation")
public class UserRecordMapperImp implements UserRecordMapper {

    @Inject
    private Datastore datastore;

    /**
     * Returns a collection of all user login records associated with the user
     * having the given username.
     *
     * @param username
     *            The username of the user whose login records are to be
     *            retrieved.
     *
     * @return A collection of all user login records associated with the user
     *         having the given username. This collection will be empty if no
     *         such user exists.
     */
    @Override
    public List<ActivityRecordModel> select(String username) {

        List<UserModel> listTemp = datastore.createQuery(UserModel.class)
                .disableValidation().field("username").equal(username)
                .order("-user.startDate, -user.endDate").asList();

        if (listTemp.isEmpty()) {
            return new ArrayList<ActivityRecordModel>();
        }

        return new ArrayList<ActivityRecordModel>(datastore
                .createQuery(UserRecordModel.class).disableValidation()
                .field("user").hasAnyOf(listTemp).order("user").asList());

    }

    /**
     * Inserts the given user login record.
     *
     * @param record
     *            The user login record to insert.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insert(ActivityRecordModel record) {

        record.setUser(datastore.createQuery(UserModel.class)
                .disableValidation().field("username")
                .equal(record.getUsername()).asList().get(0));

        datastore.save((UserRecordModel) record);

        return 1;

    }

    /**
     * Updates the given user login record.
     *
     * @param record
     *            The user login record to update.
     *
     * @return The number of rows updated.
     */
    @Override
    public int update(ActivityRecordModel object) {

        datastore.update(
                datastore.createQuery(ActivityRecordModel.class)
                        .disableValidation().field("id")
                        .equal(new ObjectId(object.getId())),
                datastore.createUpdateOperations(ActivityRecordModel.class)
                        .set("remoteHost", object.getRemoteHost())
                        .set("user", object.getUser())
                        .set("username", object.getUsername())
                        .set("startDate", object.getStartDate())
                        .set("endDate", object.getEndDate()));

        return 1;

    }

    /**
     * Searches for up to <code>limit</code> user login records that contain the
     * given terms, sorted by the given predicates, regardless of whether the
     * data they are associated with is is readable by any particular user. This
     * should only be called on behalf of a system administrator. If records are
     * needed by a non-administrative user who must have explicit read rights,
     * use searchReadable() instead.
     *
     * @param terms
     *            The search terms that must match the returned records.
     *
     * @param sortPredicates
     *            A list of predicates to sort the returned records by, in order
     *            of priority.
     *
     * @param limit
     *            The maximum number of records that should be returned.
     *
     * @return The results of the search performed with the given parameters.
     */
    @Override
    public List<ActivityRecordModel> search(
            Collection<ActivityRecordSearchTerm> terms,
            List<ActivityRecordSortPredicate> sortPredicates, int limit) {

        Pattern regexp;
        List<String> userIdList = new ArrayList<String>();
        Query<UserRecordModel> query = null;

        for (ActivityRecordSearchTerm item : terms) {

            regexp = Pattern.compile(item.getTerm());

            List<UserModel> listTemp = datastore.createQuery(UserModel.class)
                    .disableValidation().filter("username", regexp).asList();

            for (UserModel itemTemp : listTemp) {
                userIdList.add(itemTemp.getId());
            }

            query = datastore.createQuery(UserRecordModel.class)
                    .disableValidation().field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .hasAnyOf(userIdList).asList());

            if (item.getStartDate() != null && item.getEndDate() != null) {
                query.criteria("startDate").greaterThanOrEq(item.getStartDate())
                        .add(query.criteria("endDate")
                                .lessThan(item.getEndDate()));
            }

            List<String> listSortItems = new ArrayList<String>();

            for (ActivityRecordSortPredicate itemTemp : sortPredicates) {
                if (itemTemp
                        .getProperty() == ActivityRecordSet.SortableProperty.START_DATE) {
                    listSortItems.add(
                            itemTemp.isDescending() ? "-" : "" + "startDate");
                }
                else {
                    listSortItems.add("1");
                }
            }

            query.order(listSortItems.toString()).limit(limit);

        }

        return new ArrayList<ActivityRecordModel>(query.asList());

    }

    /**
     * Searches for up to <code>limit</code> user login records that contain the
     * given terms, sorted by the given predicates. Only records that are
     * associated with data explicitly readable by the given user will be
     * returned. If records are needed by a system administrator (who, by
     * definition, does not need explicit read rights), use search() instead.
     *
     * @param user
     *            The user whose permissions should determine whether a record
     *            is returned.
     *
     * @param terms
     *            The search terms that must match the returned records.
     *
     * @param sortPredicates
     *            A list of predicates to sort the returned records by, in order
     *            of priority.
     *
     * @param limit
     *            The maximum number of records that should be returned.
     *
     * @return The results of the search performed with the given parameters.
     */
    @Override
    public List<ActivityRecordModel> searchReadable(UserModel user,
            Collection<ActivityRecordSearchTerm> terms,
            List<ActivityRecordSortPredicate> sortPredicates, int limit) {

        Pattern regexp;
        List<ObjectId> userIdList = new ArrayList<ObjectId>();
        Query<UserRecordModel> query = null;

        for (ActivityRecordSearchTerm item : terms) {

            regexp = Pattern.compile(item.getTerm());

            List<UserModel> listTemp = datastore.createQuery(UserModel.class)
                    .disableValidation().filter("username", regexp).asList();

            for (UserModel itemTemp : listTemp) {
                userIdList.add(new ObjectId(itemTemp.getId()));
            }

            query = datastore.createQuery(UserRecordModel.class)
                    .disableValidation().field("user")
                    .in(datastore.createQuery(UserModel.class)
                            .disableValidation().field("id")
                            .hasAnyOf(userIdList).asList())
                    .field("affectedUser")
                    .in(datastore.createQuery(UserPermissionModel.class)
                            .disableValidation().field("user")
                            .in(datastore.createQuery(UserModel.class)
                                    .disableValidation().field("id")
                                    .equal(new ObjectId(user.getId())).asList())
                            .field("type").equal("READ"));

            if (item.getStartDate() != null && item.getEndDate() != null) {
                query.criteria("startDate").greaterThanOrEq(item.getStartDate())
                        .add(query.criteria("endDate")
                                .lessThan(item.getEndDate()));
            }

            List<String> listSortItems = new ArrayList<String>();

            for (ActivityRecordSortPredicate itemTemp : sortPredicates) {
                if (itemTemp
                        .getProperty() == ActivityRecordSet.SortableProperty.START_DATE) {
                    listSortItems.add(
                            itemTemp.isDescending() ? "-" : "" + "startDate");
                }
                else {
                    listSortItems.add("1");
                }
            }

            query.order(listSortItems.toString()).limit(limit);

        }

        return new ArrayList<ActivityRecordModel>(query.asList());

    }

}
