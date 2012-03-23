package net.sourceforge.guacamole.net.event;

import net.sourceforge.guacamole.net.GuacamoleTunnel;
import net.sourceforge.guacamole.net.auth.Credentials;

/**
 * An event which is triggered whenever a tunnel is being connected. The tunnel
 * being connected can be accessed through getTunnel(), and the set of all
 * credentials available from the request which is connecting the tunnel can be
 * retrieved using getCredentials().
 * 
 * @author Michael Jumper
 */
public class TunnelConnectEvent implements CredentialEvent, TunnelEvent {
    
    /**
     * The credentials associated with the request that is connecting the
     * tunnel, if any.
     */
    private Credentials credentials;

    /**
     * The tunnel being connected.
     */
    private GuacamoleTunnel tunnel;

    /**
     * Creates a new TunnelConnectEvent which represents the connecting of the
     * given tunnel via a request associated with the given credentials.
     * 
     * @param credentials The credentials associated with the request
     *                    connecting the tunnel.
     * @param tunnel The tunnel being connected.
     */
    public TunnelConnectEvent(Credentials credentials, GuacamoleTunnel tunnel) {
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
