/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic.extension;

import java.lang.reflect.InvocationTargetException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a safe wrapper around an AuthenticationProvider subclass, such that
 * authentication attempts can cleanly fail, and errors can be properly logged,
 * even if the AuthenticationProvider cannot be instantiated.
 *
 * @author Michael Jumper
 */
public class AuthenticationProviderFacade implements AuthenticationProvider {

    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(AuthenticationProviderFacade.class);

    /**
     * The underlying authentication provider, or null if the authentication
     * provider could not be instantiated.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Creates a new AuthenticationProviderFacade which delegates all function
     * calls to an instance of the given AuthenticationProvider subclass. If
     * an instance of the given class cannot be created, creation of this
     * facade will still succeed, but its use will result in errors being
     * logged, and all authentication attempts will fail.
     *
     * @param authProviderClass
     *     The AuthenticationProvider subclass to instantiate.
     */
    public AuthenticationProviderFacade(Class<? extends AuthenticationProvider> authProviderClass) {

        AuthenticationProvider instance = null;
        
        try {
            // Attempt to instantiate the authentication provider
            instance = authProviderClass.getConstructor().newInstance();
        }
        catch (NoSuchMethodException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("AuthenticationProvider is missing a default constructor.", e);
        }
        catch (SecurityException e) {
            logger.error("The Java security mananager is preventing authentication extensions "
                       + "from being loaded. Please check the configuration of Java or your "
                       + "servlet container.");
            logger.debug("Creation of AuthenticationProvider disallowed by security manager.", e);
        }
        catch (InstantiationException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("AuthenticationProvider cannot be instantiated.", e);
        }
        catch (IllegalAccessException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("Default constructor of AuthenticationProvider is not public.", e);
        }
        catch (IllegalArgumentException e) {
            logger.error("The authentication extension in use is not properly defined. "
                       + "Please contact the developers of the extension or, if you "
                       + "are the developer, turn on debug-level logging.");
            logger.debug("Default constructor of AuthenticationProvider cannot accept zero arguments.", e);
        } 
        catch (InvocationTargetException e) {

            // Obtain causing error - create relatively-informative stub error if cause is unknown
            Throwable cause = e.getCause();
            if (cause == null)
                cause = new GuacamoleException("Error encountered during initialization.");
            
            logger.error("Authentication extension failed to start: {}", cause.getMessage());
            logger.debug("AuthenticationProvider instantiation failed.", e);

        }
       
        // Associate instance, if any
        authProvider = instance;

    }

    @Override
    public UserContext getUserContext(Credentials credentials)
            throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("Authentication attempt denied because the authentication system could not be loaded. Please for errors earlier in the logs.");
            throw new GuacamoleInsufficientCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);
        }

        // Delegate to underlying auth provider
        return authProvider.getUserContext(credentials);
        
    }

    @Override
    public UserContext updateUserContext(UserContext context, Credentials credentials)
            throws GuacamoleException {

        // Ignore auth attempts if no auth provider could be loaded
        if (authProvider == null) {
            logger.warn("Reauthentication attempt denied because the authentication system could not be loaded. Please for errors earlier in the logs.");
            throw new GuacamoleInsufficientCredentialsException("Permission denied.", CredentialsInfo.USERNAME_PASSWORD);
        }

        // Delegate to underlying auth provider
        return authProvider.updateUserContext(context, credentials);
        
    }

}
