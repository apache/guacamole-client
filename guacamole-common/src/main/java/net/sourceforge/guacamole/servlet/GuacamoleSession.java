
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

public class GuacamoleSession {

    private final HttpSession session;
    private ConcurrentMap<String, GuacamoleTunnel> tunnels;

    public GuacamoleSession(HttpSession session) throws GuacamoleException {

        if (session == null)
            throw new GuacamoleException("User has no session.");

        this.session = session;

        synchronized (session) {

            tunnels = (ConcurrentMap<String, GuacamoleTunnel>) session.getAttribute("GUAC_TUNNELS");
            if (tunnels == null) {
                tunnels = new ConcurrentHashMap<String, GuacamoleTunnel>();
                session.setAttribute("GUAC_TUNNELS", tunnels);
            }

        }

    }

    public void invalidate() {
        session.invalidate();
    }

    public void attachTunnel(GuacamoleTunnel tunnel) throws GuacamoleException {
        tunnels.put(tunnel.getUUID().toString(), tunnel);
    }

    public void detachTunnel(GuacamoleTunnel tunnel) throws GuacamoleException {
        tunnels.remove(tunnel.getUUID().toString());
    }

    public GuacamoleTunnel getTunnel(String tunnelUUID) {
        return tunnels.get(tunnelUUID);
    }

}
