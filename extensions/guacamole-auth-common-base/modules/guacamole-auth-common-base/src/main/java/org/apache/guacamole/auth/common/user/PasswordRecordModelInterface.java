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

package org.apache.guacamole.auth.common.user;

import java.util.Date;

/**
 * 
 * A single password record representing a previous password of a particular
 * user, along with the time/date that password was set.
 * 
 */
public interface PasswordRecordModelInterface {

    /**
     * Returns the hash of the password and password salt.
     *
     * @return The hash of the password and password salt.
     */
    public byte[] getPasswordHash();

    /**
     * Returns the random salt that was used when generating the password hash.
     *
     * @return The random salt that was used when generating the password hash.
     */
    public byte[] getPasswordSalt();

    /**
     * Returns the date that this password was first set for the associated
     * user.
     *
     * @return The date that this password was first set for the associated
     *         user.
     */
    public Date getPasswordDate();

    /**
     * Returns the database ID of the user associated with this password record.
     *
     * @return The database ID of the user associated with this password record.
     */
    public Integer getUserID();

}
