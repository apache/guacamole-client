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


package org.apache.guacamole.auth.jdbc.reset;

import java.util.Date;

/**
 * A single password reset token, as stored in the database. Only the SHA-256
 * hash of the token is ever stored; the token itself exists only in the
 * e-mail sent to the user.
 */
public class PasswordResetTokenModel {

    /**
     * The SHA-256 hash of the token.
     */
    private byte[] tokenHash;

    /**
     * The ID of the user the token resets the password of.
     */
    private Integer userID;

    /**
     * The username of that user. Populated only when reading a token.
     */
    private String username;

    /**
     * When the token was issued.
     */
    private Date created;

    /**
     * When the token stops being valid.
     */
    private Date expires;

    /**
     * When the token was redeemed or superseded, or null if it is unused.
     */
    private Date used;

    public byte[] getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(byte[] tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public Date getUsed() {
        return used;
    }

    public void setUsed(Date used) {
        this.used = used;
    }

}
