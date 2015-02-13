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

package net.sourceforge.guacamole.net.auth.mysql.model;

/**
 * Object representation of a Guacamole user, as represented in the database.
 *
 * @author Michael Jumper
 */
public class UserModel {

    /**
     * The ID of this user in the database, if any.
     */
    private Integer userID;

    /**
     * The unique username which identifies this user.
     */
    private String username;
    
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
     * Creates a new, empty user.
     */
    public UserModel() {
    }

    /**
     * Returns the username that uniquely identifies this user.
     *
     * @return
     *     The username that uniquely identifies this user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username that uniquely identifies this user.
     *
     * @param username
     *     The username that uniquely identifies this user.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the ID of this user in the database, if it exists.
     *
     * @return
     *     The ID of this user in the database, or null if this user was not
     *     retrieved from the database.
     */
    public Integer getUserID() {
        return userID;
    }

    /**
     * Sets the ID of this user to the given value.
     *
     * @param userID
     *     The ID to assign to this user.
     */
    public void setUserID(Integer userID) {
        this.userID = userID;
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

}
