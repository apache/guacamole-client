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

package org.glyptodon.guacamole.auth.jdbc.base;

import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;

/**
 * Common base class for objects that are associated with the users that
 * obtain them.
 *
 * @author Michael Jumper
 */
public abstract class RestrictedObject {

    /**
     * The user this object belongs to. Access is based on his/her permission
     * settings.
     */
    private AuthenticatedUser currentUser;

    /**
     * Initializes this object, associating it with the current authenticated
     * user and populating it with data from the given model object
     *
     * @param currentUser
     *     The user that created or retrieved this object.
     */
    public void init(AuthenticatedUser currentUser) {
        setCurrentUser(currentUser);
    }

    /**
     * Returns the user that created or queried this object. This user's
     * permissions dictate what operations can be performed on or through this
     * object.
     *
     * @return
     *     The user that created or queried this object.
     */
    public AuthenticatedUser getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the user that created or queried this object. This user's
     * permissions dictate what operations can be performed on or through this
     * object.
     *
     * @param currentUser 
     *     The user that created or queried this object.
     */
    public void setCurrentUser(AuthenticatedUser currentUser) {
        this.currentUser = currentUser;
    }

}
