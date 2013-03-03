
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
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.GuacamoleWriter;
import net.sourceforge.guacamole.net.GuacamoleSocket;

/**
 * A MySQL specific wrapper around a ConfiguredGuacamoleSocket.
 * @author James Muehlner
 */
public class MySQLGuacamoleSocket implements GuacamoleSocket {

    /**
     * Injected ActiveConnectionSet which will contain all active connections.
     */
    @Inject
    private ActiveConnectionSet activeConnectionSet;

    /**
     * The wrapped socket.
     */
    private GuacamoleSocket socket;

    /**
     * The ID associated with the connection associated with the wrapped
     * socket.
     */
    private int connectionID;

    /**
     * The ID of the history record associated with this instance of the
     * connection.
     */
    private int historyID;

    /**
     * Initialize this MySQLGuacamoleSocket with the provided GuacamoleSocket.
     *
     * @param socket The ConfiguredGuacamoleSocket to wrap.
     * @param connectionID The ID of the connection associated with the given
     *                     socket.
     * @param historyID The ID of the history record associated with this
     *                  instance of the connection.
     */
    public void init(GuacamoleSocket socket, int connectionID, int historyID) {
        this.socket = socket;
        this.connectionID = connectionID;
        this.historyID = historyID;
    }

    @Override
    public GuacamoleReader getReader() {
        return socket.getReader();
    }

    @Override
    public GuacamoleWriter getWriter() {
        return socket.getWriter();
    }

    @Override
    public void close() throws GuacamoleException {

        // Close socket
        socket.close();

        // Mark this connection as inactive
        activeConnectionSet.closeConnection(connectionID, historyID);
    }

    @Override
    public boolean isOpen() {
        return socket.isOpen();
    }
}
