package org.glyptodon.guacamole.net.event;

import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;

/**
 * An event which is triggered whenever a user's credentials pass
 * authentication. The credentials that passed authentication are included
 * within this event, and can be retrieved using getCredentials().
 * 
 * @author Michael Jumper
 */
public class AuthenticationSuccessEvent implements UserEvent, CredentialEvent {

    /**
     * The UserContext associated with the request that is connecting the
     * tunnel, if any.
     */
    private UserContext context;

    /**
     * The credentials which passed authentication.
     */
    private Credentials credentials;

    /**
     * Creates a new AuthenticationSuccessEvent which represents a successful
     * authentication attempt with the given credentials.
     *
     * @param context The UserContext created as a result of successful
     *                authentication.
     * @param credentials The credentials which passed authentication.
     */
    public AuthenticationSuccessEvent(UserContext context, Credentials credentials) {
        this.context = context;
        this.credentials = credentials;
    }

    @Override
    public UserContext getUserContext() {
        return context;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

}
