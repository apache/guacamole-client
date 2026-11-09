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
import java.util.regex.Pattern;

import org.apache.guacamole.morphia.base.ActivityRecordSearchTerm;
import org.apache.guacamole.morphia.base.ActivityRecordSortPredicate;
import org.apache.guacamole.morphia.user.UserModel;
import org.apache.guacamole.morphia.user.UserPermissionModel;
import org.apache.guacamole.net.auth.ActivityRecordSet;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for connection record objects.
 */
@SuppressWarnings("deprecation")
public class ConnectionRecordMapperImp implements ConnectionRecordMapper {

    @Inject
    private Datastore datastore;

    /**
     * Returns a collection of all connection records associated with the
     * connection having the given identifier.
     *
     * @param identifier
     *            The identifier of the connection whose records are to be
     *            retrieved.
     *
     * @return A collection of all connection records associated with the
     *         connection having the given identifier. This collection will be
     *         empty if no such connection exists.
     */
    @Override
    public List<ConnectionRecordModel> select(String identifier) {

        return datastore.createQuery(ConnectionRecordModel.class)
                .field("connection")
                .in(datastore.createQuery(ConnectionModel.class).field("id")
                        .equal(new ObjectId(identifier)).asList())
                .asList();

    }

    /**
     * Inserts the given connection record.
     *
     * @param record
     *            The connection record to insert.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insert(ConnectionRecordModel record) {

        List<UserModel> listTemp = datastore.createQuery(UserModel.class)
                .field("username").equal(record.getUsername()).asList();

        if (!listTemp.isEmpty()) {
            record.setUsername(listTemp.get(0).getIdentifier());
        }

        datastore.save(record);

        return 1;

    }

    /**
     * Searches for up to <code>limit</code> connection records that contain the
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
    public List<ConnectionRecordModel> search(
            Collection<ActivityRecordSearchTerm> terms,
            List<ActivityRecordSortPredicate> sortPredicates, int limit) {

        Pattern regexp;
        List<ConnectionRecordModel> resultList = new ArrayList<ConnectionRecordModel>();

        Query<ConnectionRecordModel> query = datastore
                .createQuery(ConnectionRecordModel.class);
        query.disableValidation();

        for (ActivityRecordSearchTerm item : terms) {

            regexp = Pattern.compile(item.getTerm());

            List<UserModel> listTemp = datastore.createQuery(UserModel.class)
                    .filter("username", regexp).asList();

            List<ConnectionModel> listTemp2 = datastore
                    .createQuery(ConnectionModel.class).filter("name", regexp)
                    .asList();

            List<Criteria> criteriasList = new ArrayList<Criteria>();
            criteriasList.add(query.criteria("user").in(listTemp));
            criteriasList.add(query.criteria("connection").in(listTemp2));

            if (item.getStartDate() != null && item.getEndDate() != null) {
                criteriasList.add(query.and(
                        query.criteria("startDate")
                                .greaterThanOrEq(item.getStartDate()),
                        query.criteria("endDate").lessThan(item.getEndDate())));
            }

            Criteria[] criterias = criteriasList
                    .toArray(new Criteria[criteriasList.size()]);

            query.and(query.or(criterias));
        }

        List<String> listSortItems = new ArrayList<String>();

        for (ActivityRecordSortPredicate itemTemp : sortPredicates) {
            if (itemTemp
                    .getProperty() == ActivityRecordSet.SortableProperty.START_DATE) {
                listSortItems.add(
                        (itemTemp.isDescending() ? "-" : "") + "startDate");
            }
            else {
                listSortItems.add("1");
            }
        }

        query.order(listSortItems.toString()).limit(limit);

        resultList = query.asList();

        return resultList;

    }

    /**
     * Searches for up to <code>limit</code> connection records that contain the
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
    public List<ConnectionRecordModel> searchReadable(UserModel user,
            Collection<ActivityRecordSearchTerm> terms,
            List<ActivityRecordSortPredicate> sortPredicates, int limit) {

        List<ConnectionModel> connectionList = new ArrayList<ConnectionModel>();
        List<UserModel> userList = new ArrayList<UserModel>();

        List<ConnectionPermissionModel> connectionPermissionList = datastore
                .createQuery(ConnectionPermissionModel.class).field("user")
                .in(datastore.createQuery(UserModel.class).field("id")
                        .equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        for (ConnectionPermissionModel item : connectionPermissionList) {
            connectionList.add(item.getConnection());
        }

        List<UserPermissionModel> userPermissionList = datastore
                .createQuery(UserPermissionModel.class).field("user")
                .in(datastore.createQuery(UserModel.class).field("id")
                        .equal(new ObjectId(user.getId())).asList())
                .field("type").equal("READ").asList();

        for (UserPermissionModel item : userPermissionList) {
            userList.add(item.getAffectedUser());
        }

        Query<ConnectionRecordModel> query = datastore
                .createQuery(ConnectionRecordModel.class).field("connection")
                .in(connectionList).field("user").in(userList);

        Pattern regexp;

        for (ActivityRecordSearchTerm item : terms) {

            regexp = Pattern.compile(item.getTerm());

            List<UserModel> listTemp = datastore.createQuery(UserModel.class)
                    .filter("username", regexp).asList();

            List<ConnectionModel> listTemp2 = datastore
                    .createQuery(ConnectionModel.class).filter("name", regexp)
                    .asList();

            List<Criteria> criteriasList = new ArrayList<Criteria>();
            criteriasList.add(query.criteria("user").in(listTemp));
            criteriasList.add(query.criteria("connection").in(listTemp2));

            if (item.getStartDate() != null && item.getEndDate() != null) {
                criteriasList.add(query.and(
                        query.criteria("startDate")
                                .greaterThanOrEq(item.getStartDate()),
                        query.criteria("endDate").lessThan(item.getEndDate())));
            }

            Criteria[] criterias = criteriasList
                    .toArray(new Criteria[criteriasList.size()]);

            query.and(query.or(criterias));
        }

        List<String> listSortItems = new ArrayList<String>();

        for (ActivityRecordSortPredicate itemTemp : sortPredicates) {
            if (itemTemp
                    .getProperty() == ActivityRecordSet.SortableProperty.START_DATE) {
                listSortItems.add(
                        (itemTemp.isDescending() ? "-" : "") + "startDate");
            }
            else {
                listSortItems.add("1");
            }
        }

        query.order(listSortItems.toString()).limit(limit);

        return query.asList();

    }

}
