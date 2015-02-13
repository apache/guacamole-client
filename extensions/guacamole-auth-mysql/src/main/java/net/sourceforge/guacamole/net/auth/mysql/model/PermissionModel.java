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
 * Generic base permission model which grants a permission of a particular type
 * to a specific user.
 *
 * @author Michael Jumper
 * @param <PermissionType>
 *     The type of permissions allowed within this model.
 */
public abstract class PermissionModel<PermissionType> {

    /**
     * The database ID of the user to whom this permission is granted.
     */
    private Integer userID;

    /**
     * The username of the user to whom this permission is granted.
     */
    private String username;

    /**
     * The type of action granted by this permission.
     */
    private PermissionType type;
    
    /**
     * Returns the database ID of the user to whom this permission is granted.
     * 
     * @return
     *     The database ID of the user to whom this permission is granted.
     */
    public Integer getUserID() {
        return userID;
    }

    /**
     * Sets the database ID of the user to whom this permission is granted.
     *
     * @param userID
     *     The database ID of the user to whom this permission is granted.
     */
    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    /**
     * Returns the username of the user to whom this permission is granted.
     * 
     * @return
     *     The username of the user to whom this permission is granted.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user to whom this permission is granted.
     *
     * @param username
     *     The username of the user to whom this permission is granted.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the type of action granted by this permission.
     *
     * @return
     *     The type of action granted by this permission.
     */
    public PermissionType getType() {
        return type;
    }

    /**
     * Sets the type of action granted by this permission.
     *
     * @param type
     *     The type of action granted by this permission.
     */
    public void setType(PermissionType type) {
        this.type = type;
    }

}
