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

import org.apache.guacamole.morphia.base.ActivityRecordModel;
import org.mongodb.morphia.annotations.Entity;

/**
 * 
 * A single connection record representing a past usage of a particular
 * connection. If the connection was being shared, the sharing profile used to
 * join the connection is included in the record.
 * 
 * guacamole_user_history: { id: string, user: UserModel, username: string,
 * remote_host: string, start_date: date, end_date: date }
 *
 */
@Entity("guacamole_user_history")
public class UserRecordModel extends ActivityRecordModel {

}
