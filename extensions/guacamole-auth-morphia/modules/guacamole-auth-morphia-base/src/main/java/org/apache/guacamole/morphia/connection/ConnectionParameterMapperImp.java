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

import java.util.Collection;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import com.google.inject.Inject;

/**
 * Mapper for connection parameter objects.
 */
public class ConnectionParameterMapperImp implements ConnectionParameterMapper {

    @Inject
    private Datastore datastore;

    /**
     * Returns a collection of all parameters associated with the connection
     * having the given identifier.
     *
     * @param identifier
     *            The identifier of the connection whose parameters are to be
     *            retrieved.
     *
     * @return A collection of all parameters associated with the connection
     *         having the given identifier. This collection will be empty if no
     *         such connection exists.
     */
    @Override
    public Collection<ConnectionParameterModel> select(String identifier) {
        return datastore.createQuery(ConnectionParameterModel.class)
                .disableValidation().field("connection")
                .in(datastore.createQuery(ConnectionModel.class)
                        .disableValidation().field("id")
                        .equal(new ObjectId(identifier)).asList())
                .asList();

    }

    /**
     * Inserts each of the parameter model objects in the given collection as
     * new connection parameters.
     *
     * @param parameters
     *            The connection parameters to insert.
     *
     * @return The number of rows inserted.
     */
    @Override
    public int insert(Collection<ConnectionParameterModel> parameters) {

        datastore.save(parameters);

        return parameters.size();

    }

    /**
     * Deletes all parameters associated with the connection having the given
     * identifier.
     *
     * @param identifier
     *            The identifier of the connection whose parameters should be
     *            deleted.
     *
     * @return The number of rows deleted.
     */
    @Override
    public int delete(String identifier) {

        final Query<ConnectionParameterModel> items = datastore
                .createQuery(ConnectionParameterModel.class).disableValidation()
                .field("id").equal(new ObjectId(identifier));

        datastore.delete(items);

        return items.asList().size();

    }

}
