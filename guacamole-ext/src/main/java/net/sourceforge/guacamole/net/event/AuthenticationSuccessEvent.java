package net.sourceforge.guacamole.net.event;

import net.sourceforge.guacamole.net.auth.Credentials;

/**
 * An event which is triggered whenever a user's credentials pass
 * authentication. The credentials that passed authentication are included
 * within this event, and can be retrieved using getCredentials().
 * 
 * @author Michael Jumper
 */
public class AuthenticationSuccessEvent implements CredentialEvent {
    
    /**
     * The credentials which passed authentication
     */
    private Credentials credentials;

    /**
     * Creates a new AuthenticationSuccessEvent which represents a successful
     * authentication attempt with the given credentials.
     * 
     * @param credentials The credentials which passed authentication.
     */
    public AuthenticationSuccessEvent(Credentials credentials) {
        this.credentials = credentials;
    }
 
    @Override
    public Credentials getCredentials() {
        return credentials;
    }
    
}
