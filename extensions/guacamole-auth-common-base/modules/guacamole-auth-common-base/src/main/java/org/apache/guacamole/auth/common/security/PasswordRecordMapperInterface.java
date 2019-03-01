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

package org.apache.guacamole.auth.common.security;

import java.util.List;
import org.apache.guacamole.auth.common.user.PasswordRecordModelInterface;

/**
 * Mapper for historical password records (users' prior passwords, along with
 * the dates they were set).
 */
public interface PasswordRecordMapperInterface {

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
    public List<PasswordRecordModelInterface> select(String username,
            int maxHistorySize);

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
    public int insert(PasswordRecordModelInterface record, int maxHistorySize);

}
