package net.sourceforge.guacamole.net.event;

import net.sourceforge.guacamole.net.GuacamoleTunnel;
import net.sourceforge.guacamole.net.auth.Credentials;

/**
 * An event which is triggered whenever a tunnel is being closed. The tunnel
 * being closed can be accessed through getTunnel(), and the set of all
 * credentials available from the request which is closing the tunnel can be
 * retrieved using getCredentials().
 * 
 * @author Michael Jumper
 */
public class TunnelCloseEvent implements CredentialEvent, TunnelEvent {

    /**
     * The credentials associated with the request that is closing the
     * tunnel, if any.
     */
    private Credentials credentials;

    /**
     * The tunnel being closed.
     */
    private GuacamoleTunnel tunnel;

    /**
     * Creates a new TunnelCloseEvent which represents the closing of the
     * given tunnel via a request associated with the given credentials.
     * 
     * @param credentials The credentials associated with the request
     *                    closing the tunnel.
     * @param tunnel The tunnel being closed.
     */
    public TunnelCloseEvent(Credentials credentials, GuacamoleTunnel tunnel) {
        this.credentials = credentials;
        this.tunnel = tunnel;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public GuacamoleTunnel getTunnel() {
        return tunnel;
    }

}
