
package net.sourceforge.guacamole.net.auth.simple;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;


/**
 * An extremely simple read-only implementation of a Directory of
 * GuacamoleConfigurations which provides access to a pre-defined Map of
 * GuacamoleConfigurations.
 * 
 * @author Michael Jumper
 */
public class SimpleConnectionDirectory 
    implements Directory<String, Connection> {

    /**
     * The Map of Connections to provide access to.
     */
    private Map<String, Connection> connections =
            new HashMap<String, Connection>();

    /**
     * Creates a new SimpleConnectionDirectory which provides
     * access to the configurations contained within the given Map.
     * 
     * @param configs The Map of GuacamoleConfigurations to provide access to.
     */
    public SimpleConnectionDirectory(
            Map<String, GuacamoleConfiguration> configs) {

        // Create connections for each config
        for (Entry<String, GuacamoleConfiguration> entry : configs.entrySet())
            connections.put(entry.getKey(),
                    new SimpleConnection(entry.getKey(), entry.getValue()));
        
    }
    
    @Override
    public Connection get(String identifier)
            throws GuacamoleException {
        return connections.get(identifier);
    }

    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        return connections.keySet();
    }

    @Override
    public void add(Connection connection)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void update(Connection connection)
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
