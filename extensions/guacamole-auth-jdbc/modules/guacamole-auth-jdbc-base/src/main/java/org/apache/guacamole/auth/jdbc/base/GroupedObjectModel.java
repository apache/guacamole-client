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

package org.apache.guacamole.auth.jdbc.base;

/**
 * Object representation of a Guacamole object, such as a user or connection,
 * as represented in the database.
 *
 * @author Michael Jumper
 */
public abstract class GroupedObjectModel extends ObjectModel {

    /**
     * The unique identifier which identifies the parent of this object.
     */
    private String parentIdentifier;
    
    /**
     * Creates a new, empty object.
     */
    public GroupedObjectModel() {
    }

    /**
     * Returns the identifier of the parent connection group, or null if the
     * parent connection group is the root connection group.
     *
     * @return 
     *     The identifier of the parent connection group, or null if the parent
     *     connection group is the root connection group.
     */
    public String getParentIdentifier() {
        return parentIdentifier;
    }

    /**
     * Sets the identifier of the parent connection group.
     *
     * @param parentIdentifier
     *     The identifier of the parent connection group, or null if the parent
     *     connection group is the root connection group.
     */
    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

}
