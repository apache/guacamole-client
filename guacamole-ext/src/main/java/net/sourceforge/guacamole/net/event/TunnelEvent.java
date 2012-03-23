package net.sourceforge.guacamole.net.event;

import net.sourceforge.guacamole.net.GuacamoleTunnel;

/**
 * Abstract basis for events associated with tunnels.
 * 
 * @author Michael Jumper
 */
public interface TunnelEvent {
    
    /**
     * Returns the tunnel associated with this event, if any.
     * 
     * @return The tunnel associated with this event, if any, or null if no
     *         tunnel is associated with this event.
     */
    public GuacamoleTunnel getTunnel();

}
