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

package org.apache.guacamole.auth.jdbc.tunnel;

import java.util.Date;

import org.apache.guacamole.auth.jdbc.connection.ConnectionRecordModel;

import com.google.inject.Singleton;

/**
 * Base implementation of the GuacamoleTunnelService, handling retrieval of
 * connection parameters, load balancing, and connection usage counts. The
 * implementation of concurrency rules is up to policy-specific subclasses.
 */
@Singleton
public class RestrictedGuacamoleTunnelService extends RestrictedGuacamoleTunnelServiceAbstract {

    /**
     * Saves the given ActiveConnectionRecord to the database. The end date of
     * the saved record will be populated with the current time.
     *
     * @param record
     *     The record to save.
     */
	@Override
	protected void saveConnectionRecord(ActiveConnectionRecord record) {

        // Get associated models
        ConnectionRecordModel recordModel = new ConnectionRecordModel();

        // Copy user information and timestamps into new record
        recordModel.setUsername(record.getUsername());
        recordModel.setConnectionIdentifier(record.getConnectionIdentifier());
        recordModel.setConnectionName(record.getConnectionName());
        recordModel.setRemoteHost(record.getRemoteHost());
        recordModel.setSharingProfileIdentifier(record.getSharingProfileIdentifier());
        recordModel.setSharingProfileName(record.getSharingProfileName());
        recordModel.setStartDate(record.getStartDate());
        recordModel.setEndDate(new Date());

        // Insert connection record
        connectionRecordMapper.insert(recordModel);

    }

}
