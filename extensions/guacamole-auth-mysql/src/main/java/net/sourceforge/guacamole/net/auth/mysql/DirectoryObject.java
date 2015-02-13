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

package net.sourceforge.guacamole.net.auth.mysql;

import org.glyptodon.guacamole.net.auth.Identifiable;

/**
 * Common interface for objects that will ultimately be made available through
 * the Directory class. All such objects will need the same base set of queries
 * to fulfill the needs of the Directory class.
 *
 * @author Michael Jumper
 * @param <ModelType>
 *     The type of object contained within the directory whose objects are
 *     mapped by this mapper.
 */
public interface DirectoryObject<ModelType> extends Identifiable {

    /**
     * Initializes this object, associating it with the current authenticated
     * user and populating it with data from the given model object
     *
     * @param currentUser
     *     The user that created or retrieved this object.
     *
     * @param model 
     *     The backing model object.
     */
    public void init(AuthenticatedUser currentUser, ModelType model);

    /**
     * Returns the user that created or queried this object. This user's
     * permissions dictate what operations can be performed on or through this
     * object.
     *
     * @return
     *     The user that created or queried this object.
     */
    public AuthenticatedUser getCurrentUser();

    /**
     * Sets the user that created or queried this object. This user's
     * permissions dictate what operations can be performed on or through this
     * object.
     *
     * @param currentUser 
     *     The user that created or queried this object.
     */
    public void setCurrentUser(AuthenticatedUser currentUser);

    /**
     * Returns the backing model object. Changes to the model object will
     * affect this object, and changes to this object will affect the model
     * object.
     *
     * @return
     *     The user model object backing this MySQLUser.
     */
    public ModelType getModel();

    /**
     * Sets the backing model object. This will effectively replace all data
     * contained within this object.
     *
     * @param model
     *     The backing model object.
     */
    public void setModel(ModelType model);

}