/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.jdbc.user;

import org.glyptodon.guacamole.auth.jdbc.base.ObjectModel;

/**
 * Object representation of a Guacamole user, as represented in the database.
 *
 * @author Michael Jumper
 */
public class UserModel extends ObjectModel {

    /**
     * The SHA-256 hash of the password and salt.
     */
    private byte[] passwordHash;

    /**
     * The 32-byte random binary password salt that was appended to the
     * password prior to hashing.
     */
    private byte[] passwordSalt;

    /**
     * Whether the user account is disabled. Disabled accounts exist and can
     * be modified, but cannot be used.
     */
    private boolean disabled;

    /**
     * Whether the user's password is expired. If a user's password is expired,
     * it must be changed immediately upon login, and the account cannot be
     * used until this occurs.
     */
    private boolean expired;

    /**
     * Creates a new, empty user.
     */
    public UserModel() {
    }

    /**
     * Returns the hash of this user's password and password salt. This may be
     * null if the user was not retrieved from the database, and setPassword()
     * has not yet been called.
     *
     * @return
     *     The hash of this user's password and password salt.
     */
    public byte[] getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hash of this user's password and password salt. This is
     * normally only set upon retrieval from the database, or through a call
     * to the higher-level setPassword() function.
     *
     * @param passwordHash
     *     The hash of this user's password and password salt.
     */
    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the random salt that was used when generating this user's
     * password hash. This may be null if the user was not retrieved from the
     * database, and setPassword() has not yet been called.
     *
     * @return
     *     The random salt that was used when generating this user's password
     *     hash.
     */
    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    /**
     * Sets the random salt that was used when generating this user's password
     * hash. This is normally only set upon retrieval from the database, or
     * through a call to the higher-level setPassword() function.
     *
     * @param passwordSalt
     *     The random salt used when generating this user's password hash.
     */
    public void setPasswordSalt(byte[] passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    /**
     * Returns whether the user has been disabled. Disabled users are not
     * allowed to login. Although their account data exists, all login attempts
     * will fail as if the account does not exist.
     *
     * @return
     *     true if the account is disabled, false otherwise.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets whether the user is disabled. Disabled users are not allowed to
     * login. Although their account data exists, all login attempts will fail
     * as if the account does not exist.
     *
     * @param disabled
     *     true if the account should be disabled, false otherwise.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Returns whether the user's password has expired. If a user's password is
     * expired, it must be immediately changed upon login. A user account with
     * an expired password cannot be used until the password has been changed.
     *
     * @return
     *     true if the user's password has expired, false otherwise.
     */
    public boolean isExpired() {
        return expired;
    }

    /**
     * Sets whether the user's password is expired. If a user's password is
     * expired, it must be immediately changed upon login. A user account with
     * an expired password cannot be used until the password has been changed.
     *
     * @param expired
     *     true to expire the user's password, false otherwise.
     */
    public void setExpired(boolean expired) {
        this.expired = expired;
    }

}
