
package net.sourceforge.guacamole.net.auth.mysql;

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
 * The Original Code is guacamole-auth-mysql.
 *
 * The Initial Developer of the Original Code is
 * James Muehlner.
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

import com.google.inject.Inject;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionHistoryMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionHistory;

/**
 * Represents the set of currently active Connections. Whenever a socket is
 * opened, the connection ID should be added to this set, and whenever a socket
 * is closed, the connection ID should be removed from this set.
 *
 * @author James Muehlner
 */
public class ActiveConnectionSet {

    /**
     * DAO for accessing connection history.
     */
    @Inject
    private ConnectionHistoryMapper connectionHistoryDAO;
    
    /**
     * Set of all the connections that are currently active.
     */
    private Set<Integer> activeConnectionSet = new HashSet<Integer>();
    
    /**
     * Check if a connection is currently in use.
     * @param connectionID The connection to check the status of.
     * @return true if the connection is currently in use.
     */
    public boolean isActive(int connectionID) {
        return activeConnectionSet.contains(connectionID);
    }
    
    /**
     * Set a connection as open.
     * @param connectionID The ID of the connection that is being opened.
     * @param userID The ID of the user who is opening the connection.
     * @return The ID of the history record created for this open connection.
     */
    public int openConnection(int connectionID, int userID) {
        
        // Create the connection history record
        ConnectionHistory connectionHistory = new ConnectionHistory();
        connectionHistory.setConnection_id(connectionID);
        connectionHistory.setUser_id(userID);
        connectionHistory.setStart_date(new Date());
        connectionHistoryDAO.insert(connectionHistory);
        
        // Mark the connection as active
        activeConnectionSet.add(connectionID);
        
        return connectionHistory.getHistory_id();
    }
    
    /**
     * Set a connection as closed.
     * @param connectionID The ID of the connection that is being opened.
     * @param historyID The ID of the history record about the open connection.
     * @throws GuacamoleException If the open connection history is not found.
     */
    public void closeConnection(int connectionID, int historyID)
            throws GuacamoleException {
        
        // Get the existing history record
        ConnectionHistory connectionHistory = 
                connectionHistoryDAO.selectByPrimaryKey(historyID);
        
        if(connectionHistory == null)
            throw new GuacamoleException("History record not found.");
        
        // Update the connection history record to mark that it is now closed
        connectionHistory.setEnd_date(new Date());
        connectionHistoryDAO.updateByPrimaryKey(connectionHistory);
        
        // Remove the connection from the set of active connections.
        activeConnectionSet.remove(connectionID);
    }
}
