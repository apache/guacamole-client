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

/**
 * Mapper for connection parameter objects.
 */
public interface ConnectionParameterMapper {

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
    Collection<ConnectionParameterModel> select(String identifier);

    /**
     * Inserts each of the parameter model objects in the given collection as
     * new connection parameters.
     *
     * @param parameters
     *            The connection parameters to insert.
     *
     * @return The number of rows inserted.
     */
    int insert(Collection<ConnectionParameterModel> parameters);

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
    int delete(String identifier);

}
