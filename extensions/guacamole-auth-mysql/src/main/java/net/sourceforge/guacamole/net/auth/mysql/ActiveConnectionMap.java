
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.mysql.dao.ConnectionHistoryMapper;
import net.sourceforge.guacamole.net.auth.mysql.model.ConnectionHistory;

/**
 * Represents the map of currently active Connections to the count of the number
 * of current users. Whenever a socket is opened, the connection count should be
 * incremented, and whenever a socket is closed, the connection count should be 
 * decremented.
 *
 * @author James Muehlner
 */
public class ActiveConnectionMap {
    
    /**
     * Represents the count of users currently using a MySQL connection.
     */
    public class Connection {
        
        /**
         * The ID of the MySQL connection that this Connection represents.
         */
        private int connectionID;
        
        /**
         * The number of users currently using this connection.
         */
        private int currentUserCount;
        
        /**
         * Returns the ID of the MySQL connection that this Connection 
         * represents.
         * 
         * @return the ID of the MySQL connection that this Connection 
         * represents.
         */
        public int getConnectionID() {
            return connectionID;
        }
        
        /**
         * Returns the number of users currently using this connection.
         * 
         * @return the number of users currently using this connection.
         */
        public int getCurrentUserCount() {
            return currentUserCount;
        }
        
        /**
         * Set the current user count for this connection.
         * 
         * @param currentUserCount The new user count for this Connection.
         */
        public void setCurrentUserCount(int currentUserCount) {
            this.currentUserCount = currentUserCount;
        }
        
        /**
         * Create a new Connection for the given connectionID with a zero
         * current user count.
         * 
         * @param connectionID The ID of the MySQL connection that this 
         *                     Connection represents.
         */
        public Connection(int connectionID) {
            this.connectionID = connectionID;
            this.currentUserCount = 0;
        }
    }

    /**
     * DAO for accessing connection history.
     */
    @Inject
    private ConnectionHistoryMapper connectionHistoryDAO;

    /**
     * Map of all the connections that are currently active the
     * count of current users.
     */
    private Map<Integer, Connection> activeConnectionMap =
            new HashMap<Integer, Connection>();
    
    /**
     * Returns the ID of the connection with the lowest number of current
     * active users, if found.
     * 
     * @param connectionIDs The subset of connection IDs to find the least
     *                      used connection within.
     * 
     * @return The ID of the connection with the lowest number of current
     *         active users, if found.
     */
    public Integer getLeastUsedConnection(Collection<Integer> connectionIDs) {
        
        if(connectionIDs.isEmpty())
            return null;
        
        int minUserCount = Integer.MAX_VALUE;
        Integer minConnectionID = null;
        
        for(Integer connectionID : connectionIDs) {
            Connection connection = activeConnectionMap.get(connectionID);
            
            /*
             * If the connection is not found in the map, it has not been used,
             * and therefore will be count 0.
             */
            if(connection == null) {
                minUserCount = 0;
                minConnectionID = connectionID;
            }
            // If this is the least active connection
            else if(connection.getCurrentUserCount() < minUserCount) {
                minUserCount = connection.getCurrentUserCount();
                minConnectionID = connection.getConnectionID();
            }
        }
        
        return minConnectionID;
    }
    
    /**
     * Returns the count of currently active users for the given connectionID.
     * @return the count of currently active users for the given connectionID.
     */
    public int getCurrentUserCount(int connectionID) {
        Connection connection = activeConnectionMap.get(connectionID);
        
        if(connection == null)
            return 0;
        
        return connection.getCurrentUserCount();
    }
    
    /**
     * Decrement the current user count for this Connection.
     * 
     * @param connectionID The ID of the MySQL connection that this 
     *                     Connection represents.
     * 
     * @throws GuacamoleException If the connection is not found.
     */
    private void decrementUserCount(int connectionID)
            throws GuacamoleException {
        Connection connection = activeConnectionMap.get(connectionID);
        
        if(connection == null)
            throw new GuacamoleException
                    ("Connection to decrement does not exist.");
        
        // Decrement the current user count
        connection.setCurrentUserCount(connection.getCurrentUserCount() - 1);
    }
    
    /**
     * Increment the current user count for this Connection.
     * 
     * @param connectionID The ID of the MySQL connection that this 
     *                     Connection represents.
     * 
     * @throws GuacamoleException If the connection is not found.
     */
    private void incrementUserCount(int connectionID) {
        Connection connection = activeConnectionMap.get(connectionID);
        
        // If the Connection does not exist, it should be created
        if(connection == null) {
            connection = new Connection(connectionID);
            activeConnectionMap.put(connectionID, connection);
        }
        
        // Increment the current user count
        connection.setCurrentUserCount(connection.getCurrentUserCount() + 1);
    }

    /**
     * Check if a connection is currently in use.
     * @param connectionID The connection to check the status of.
     * @return true if the connection is currently in use.
     */
    public boolean isActive(int connectionID) {
        return getCurrentUserCount(connectionID) > 0;
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

        // Increment the user count
        incrementUserCount(connectionID);

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

        // Decrement the user count.
        decrementUserCount(connectionID);
    }
}
