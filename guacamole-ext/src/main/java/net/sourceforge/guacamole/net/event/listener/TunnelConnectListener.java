package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.event.TunnelConnectEvent;

/**
 * A listener whose tunnelConnected() hook will fire immediately after a new
 * tunnel is connected.
 * 
 * @author Michael Jumper
 */
public interface TunnelConnectListener {

   /**
     * Event hook which fires immediately after a new tunnel is connected.
     * The return value of this hook dictates whether the tunnel is made visible
     * to the session.
     * 
     * @param e The TunnelConnectEvent describing the tunnel being connected and
     *          any associated credentials.
     * @return true if the tunnel should be allowed to be connected, or false
     *         if the attempt should be denied, causing the attempt to
     *         effectively fail.
     * @throws GuacamoleException If an error occurs while handling the
     *                            tunnel connect event. Throwing an exception
     *                            will also stop the tunnel from being made
     *                            visible to the session.
     */
    public boolean tunnelConnected(TunnelConnectEvent e)
            throws GuacamoleException;

}
