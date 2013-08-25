
package org.glyptodon.guacamole.net.auth.simple;

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
 * The Original Code is guacamole-auth.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s): James Muehlner
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;


/**
 * An extremely simple read-only implementation of a Directory of
 * ConnectionGroup which provides which provides access to a pre-defined
 * Collection of ConnectionGroups.
 *
 * @author James Muehlner
 */
public class SimpleConnectionGroupDirectory
    implements Directory<String, ConnectionGroup> {

    /**
     * The Map of ConnectionGroups to provide access to.
     */
    private Map<String, ConnectionGroup> connectionGroups =
            new HashMap<String, ConnectionGroup>();

    /**
     * Creates a new SimpleConnectionGroupDirectory which contains the given
     * groups.
     * 
     * @param groups A Collection of all groups that should be present in this
     *               connection group directory.
     */
    public SimpleConnectionGroupDirectory(Collection<ConnectionGroup> groups) {

        // Add all given groups
        for (ConnectionGroup group : groups)
            connectionGroups.put(group.getIdentifier(), group);

    }

    @Override
    public ConnectionGroup get(String identifier)
            throws GuacamoleException {
        return connectionGroups.get(identifier);
    }

    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        return connectionGroups.keySet();
    }

    @Override
    public void add(ConnectionGroup connectionGroup)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void update(ConnectionGroup connectionGroup)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void move(String identifier, Directory<String, ConnectionGroup> directory) 
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    /**
     * An internal method for modifying the ConnectionGroups in this Directory.
     * Returns the previous connection group for the given identifier, if found.
     * 
     * @param connectionGroup The connection group to add or update the
     *                        Directory with.
     * @return The previous connection group for the connection group
     *         identifier, if found.
     */
    public ConnectionGroup putConnectionGroup(ConnectionGroup connectionGroup) {
        return connectionGroups.put(connectionGroup.getIdentifier(), connectionGroup);
    }
    
    /**
     * An internal method for removing a ConnectionGroup from this Directory.
     * 
     * @param identifier The identifier of the ConnectionGroup to remove.
     * @return The previous connection group for the given identifier, if found.
     */
    public ConnectionGroup removeConnectionGroup(String identifier) {
        return connectionGroups.remove(identifier);
    }

}
