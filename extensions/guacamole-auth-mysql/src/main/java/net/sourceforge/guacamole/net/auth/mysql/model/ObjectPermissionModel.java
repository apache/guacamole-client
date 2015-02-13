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

import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;

/**
 * Object representation of an object-related Guacamole permission, as
 * represented in the database.
 *
 * @author Michael Jumper
 */
public class ObjectPermissionModel extends PermissionModel<ObjectPermission.Type> {

    /**
     * The database ID of the object affected by this permission.
     */
    private Integer affectedID;

    /**
     * The unique identifier of the object affected by this permission.
     */
    private String affectedIdentifier;

    /**
     * Creates a new, empty object permission.
     */
    public ObjectPermissionModel() {
    }

    /**
     * Returns the database ID of the object affected by this permission.
     *
     * @return
     *     The database ID of the object affected by this permission.
     */
    public Integer getAffectedID() {
        return affectedID;
    }

    /**
     * Sets the database ID of the object affected by this permission.
     *
     * @param affectedID 
     *     The database ID of the object affected by this permission.
     */
    public void setAffectedID(Integer affectedID) {
        this.affectedID = affectedID;
    }

    /**
     * Returns the unique identifier of the object affected by this permission.
     *
     * @return
     *     The unique identifier of the object affected by this permission.
     */
    public String getAffectedIdentifier() {
        return affectedIdentifier;
    }

    /**
     * Sets the unique identifier of the object affected by this permission.
     *
     * @param affectedIdentifier 
     *     The unique identifier of the object affected by this permission.
     */
    public void setAffectedIdentifier(String affectedIdentifier) {
        this.affectedIdentifier = affectedIdentifier;
    }

}
