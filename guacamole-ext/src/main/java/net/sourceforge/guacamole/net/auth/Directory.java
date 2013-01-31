package net.sourceforge.guacamole.net.auth;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-ext.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;


/**
 * Provides access to a collection of all objects with associated identifiers,
 * and allows user manipulation and removal. Objects stored within a
 * Directory are not necessarily returned to the use as references to
 * the stored objects, thus updating an object requires calling an update
 * function.
 * 
 * @author Michael Jumper
 */
public interface Directory<IdentifierType, ObjectType> {

    /**
     * Returns the object having the given identifier. Note that changes to
     * the object returned will not necessarily affect the object stored within
     * the Directory. To update an object stored within an
     * Directory such that future calls to get() will return the updated
     * object, you must call update() on the object after modification.
     * 
     * @param identifier The identifier to use when locating the object to
     *                   return.
     * @return The object having the given identifier, or null if no such object
     *         exists.
     * 
     * @throws GuacamoleException If an error occurs while retrieving the
     *                            object, or if permission for retrieving the
     *                            object is denied.
     */
    ObjectType get(IdentifierType identifier) throws GuacamoleException;
    
    /**
     * Returns a Set containing all identifiers for all objects within this
     * Directory.
     *
     * @return A Set of all identifiers.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            the identifiers.
     */
    Set<IdentifierType> getIdentifiers() throws GuacamoleException;

    /**
     * Adds the given object to the overall set.
     * 
     * @param identifier The identifier to use when adding the object.
     * @param object The object to add.
     * 
     * @throws GuacamoleException If an error occurs while adding the object , or
     *                            if adding the object is not allowed.
     */
    void add(IdentifierType identifier, ObjectType object)
            throws GuacamoleException;
    
    /**
     * Updates the stored object with the data contained in the given object.
     * The object to update is identified using the identifier given.
     * 
     * @param identifier The identifier of the object to update.
     * @param object The object which will supply the data for the update.
     * 
     * @throws GuacamoleException If an error occurs while updating the object,
     *                            or if updating the object is not allowed.
     */
    void update(IdentifierType identifier, ObjectType object)
            throws GuacamoleException;
    
    /**
     * Removes the object with the given identifier from the overall set.
     * 
     * @param identifier The identifier of the object to remove.
     * 
     * @throws GuacamoleException If an error occurs while removing the object,
     *                            or if removing object is not allowed.
     */
    void remove(IdentifierType identifier) throws GuacamoleException;
 
}
