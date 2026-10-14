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

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

/**
 * 
 * A single password record representing a previous password of a particular
 * user, along with the time/date that password was set.
 * 
 * guacamole_user_password_history: { id: string, user: UserModel,
 * password_hash: string, password_salt: string, password_date: date }
 *
 */
@Entity("guacamole_user_password_history")
public class PasswordRecordModel {

    /** The id. */
    @Id
    @Property("id")
    private ObjectId id;

    /**
     * The database ID of the user associated with this password record.
     */
    @Reference(value = "user")
    private UserModel user;

    /**
     * The hash of the password and salt.
     */
    @Property("password_hash")
    private byte[] passwordHash;

    /**
     * The random salt that was appended to the password prior to hashing.
     */
    @Property("password_salt")
    private byte[] passwordSalt;

    /**
     * The date and time when this password was first set for the associated
     * user.
     */
    @Property("password_date")
    private Date passwordDate;

    /**
     * Creates a new, empty PasswordRecordModel.
     */
    public PasswordRecordModel() {
    }

    /**
     * Creates a new PasswordRecordModel associated with the given user and
     * populated with that user's password hash and salt.
     *
     * @param user
     *            The user to associate with this PasswordRecordModel.
     */
    public PasswordRecordModel(UserModel user) {
        this.user = user;
        this.passwordHash = user.getPasswordHash();
        this.passwordSalt = user.getPasswordSalt();
        this.passwordDate = user.getPasswordDate();
    }

    public String getId() {
        return id.toString();
    }

    public void setId(String id) {
        this.id = new ObjectId(id);
    }

    /**
     * Returns the database ID of the user associated with this password record.
     *
     * @return The database ID of the user associated with this password record.
     */
    public UserModel getUser() {
        return user;
    }

    /**
     * Sets the database ID of the user associated with this password record.
     *
     * @param user
     *            The database ID of the user to associate with this password
     *            record.
     */
    public void setUser(UserModel user) {
        this.user = user;
    }

    /**
     * Returns the hash of the password and password salt.
     *
     * @return The hash of the password and password salt.
     */
    public byte[] getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hash of the password and password salt.
     *
     * @param passwordHash
     *            The hash of the password and password salt.
     */
    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the random salt that was used when generating the password hash.
     *
     * @return The random salt that was used when generating the password hash.
     */
    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    /**
     * Sets the random salt that was used when generating the password hash.
     *
     * @param passwordSalt
     *            The random salt used when generating the password hash.
     */
    public void setPasswordSalt(byte[] passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    /**
     * Returns the date that this password was first set for the associated
     * user.
     *
     * @return The date that this password was first set for the associated
     *         user.
     */
    public Date getPasswordDate() {
        return passwordDate;
    }

    /**
     * Sets the date that this password was first set for the associated user.
     *
     * @param passwordDate
     *            The date that this password was first set for the associated
     *            user.
     */
    public void setPasswordDate(Date passwordDate) {
        this.passwordDate = passwordDate;
    }

}
