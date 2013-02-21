
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

import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;


/**
 * Represents a pairing of a GuacamoleConfiguration with a unique,
 * human-readable identifier, and abstracts the connection process. The
 * backing GuacamoleConfiguration may be intentionally obfuscated or tokenized
 * to protect sensitive configuration information.
 * 
 * @author Michael Jumper
 */
public interface Connection {

    /**
     * Returns the unique identifier assigned to this Connection.
     * @return The unique identifier assigned to this Connection.
     */
    public String getIdentifier();

    /**
     * Sets the identifier assigned to this Connection.
     * 
     * @param identifier The identifier to assign.
     */
    public void setIdentifier(String identifier);

    /**
     * Returns the GuacamoleConfiguration associated with this Connection. Note
     * that because configurations may contain sensitive information, some data
     * in this configuration may be omitted or tokenized.
     * 
     * @return The GuacamoleConfiguration associated with this Connection.
     */
    public GuacamoleConfiguration getConfiguration();

    /**
     * Sets the GuacamoleConfiguration associated with this Connection.
     * 
     * @param config The GuacamoleConfiguration to associate with this
     *               Connection.
     */
    public void setConfiguration(GuacamoleConfiguration config);

    /**
     * Establishes a connection to guacd using the GuacamoleConfiguration
     * associated with this Connection, and returns the resulting, connected
     * GuacamoleSocket. The GuacamoleSocket will be pre-configured and will
     * already have passed the handshake stage.
     *
     * @param info Information associated with the connecting client.
     * @return A fully-established GuacamoleSocket.
     * 
     * @throws GuacamoleException If an error occurs while connecting to guacd,
     *                            or if permission to connect is denied.
     */
    public GuacamoleSocket connect(GuacamoleClientInformation info)
            throws GuacamoleException;
    
    /**
     * Returns a list of ConnectionRecords representing the usage history
     * of this Connection, including any active users. ConnectionRecords
     * in this list will be sorted in descending order of end time (active
     * connections are first), and then in descending order of start time
     * (newer connections are first).
     * 
     * @return A list of ConnectionRecrods representing the usage history
     *         of this Connection.
     * 
     * @throws GuacamoleException If an error occurs while reading the history
     *                            of this connection, or if permission is
     *                            denied.
     */
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException;
            
}
