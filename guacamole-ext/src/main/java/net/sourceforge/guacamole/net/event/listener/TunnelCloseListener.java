package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.event.TunnelCloseEvent;

/**
 * A listener whose tunnelClosed() hook will fire immediately after an
 * existing tunnel is closed.
 * 
 * @author Michael Jumper
 */
public interface TunnelCloseListener {
   
    /**
     * Event hook which fires immediately after an existing tunnel is closed.
     * The return value of this hook dictates whether the tunnel is allowed to
     * be closed.
     * 
     * @param e The TunnelCloseEvent describing the tunnel being closed and
     *          any associated credentials.
     * @return true if the tunnel should be allowed to be closed, or false
     *         if the attempt should be denied, causing the attempt to
     *         effectively fail.
     * @throws GuacamoleException If an error occurs while handling the
     *                            tunnel close event. Throwing an exception
     *                            will also stop the tunnel from being closed.
     */
    public boolean tunnelClosed(TunnelCloseEvent e)
            throws GuacamoleException;

}
