
package net.sourceforge.guacamole.servlet;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleTunnel;

/**
 * Provides abstract access to the tunnels associated with a Guacamole session.
 *
 * @author Michael Jumper
 */
public class GuacamoleSession {

    private ConcurrentMap<String, GuacamoleTunnel> tunnels;

    /**
     * Creates a new GuacamoleSession, storing and retrieving tunnels from the
     * given HttpSession. Note that the true Guacamole session is tied to the
     * HttpSession provided, thus creating a new GuacamoleSession does not
     * create a new Guacamole session; it merely creates a new object for
     * accessing the tunnels of an existing Guacamole session represented by
     * the provided HttpSession.
     *
     * @param session The HttpSession to use as tunnel storage.
     * @throws GuacamoleException If session is null.
     */
    public GuacamoleSession(HttpSession session) throws GuacamoleException {

        if (session == null)
            throw new GuacamoleException("User has no session.");

        synchronized (session) {

            tunnels = (ConcurrentMap<String, GuacamoleTunnel>) session.getAttribute("GUAC_TUNNELS");
            if (tunnels == null) {
                tunnels = new ConcurrentHashMap<String, GuacamoleTunnel>();
                session.setAttribute("GUAC_TUNNELS", tunnels);
            }

        }

    }

    /**
     * Attaches the given tunnel to this GuacamoleSession.
     * @param tunnel The tunnel to attach to this GucacamoleSession.
     */
    public void attachTunnel(GuacamoleTunnel tunnel) {
        tunnels.put(tunnel.getUUID().toString(), tunnel);
    }

    /**
     * Detaches the given tunnel to this GuacamoleSession.
     * @param tunnel The tunnel to detach to this GucacamoleSession.
     */
    public void detachTunnel(GuacamoleTunnel tunnel) {
        tunnels.remove(tunnel.getUUID().toString());
    }

    /**
     * Returns the tunnel with the given UUID attached to this GuacamoleSession,
     * if any.
     *
     * @param tunnelUUID The UUID of an attached tunnel.
     * @return The tunnel corresponding to the given UUID, if attached, or null
     *         if no such tunnel is attached.
     */
    public GuacamoleTunnel getTunnel(String tunnelUUID) {
        return tunnels.get(tunnelUUID);
    }

}
