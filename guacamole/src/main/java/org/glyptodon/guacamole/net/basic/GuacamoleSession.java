/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains Guacamole-specific user information which is tied to the current
 * session, such as the UserContext and current clipboard state.
 *
 * @author Michael Jumper
 */
public class GuacamoleSession {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GuacamoleSession.class);

    /**
     * The credentials provided when the user authenticated.
     */
    private Credentials credentials;
    
    /**
     * The user context associated with this session.
     */
    private UserContext userContext;

    /**
     * Collection of all event listeners configured in guacamole.properties.
     */
    private final Collection<Object> listeners = new ArrayList<Object>();
    
    /**
     * The current clipboard state.
     */
    private final ClipboardState clipboardState = new ClipboardState();

    /**
     * All currently-active tunnels, indexed by tunnel UUID.
     */
    private final Map<String, GuacamoleTunnel> tunnels = new ConcurrentHashMap<String, GuacamoleTunnel>();

    /**
     * The last time this session was accessed.
     */
    private long lastAccessedTime;
    
    /**
     * Creates a new Guacamole session associated with the given user context.
     *
     * @param credentials The credentials provided by the user during login.
     * @param userContext The user context to associate this session with.
     * @throws GuacamoleException If an error prevents the session from being
     *                            created.
     */
    public GuacamoleSession(Credentials credentials, UserContext userContext) throws GuacamoleException {

        this.lastAccessedTime = System.currentTimeMillis();
        this.credentials = credentials;
        this.userContext = userContext;

        // Load listeners from guacamole.properties
        try {

            // Get all listener classes from properties
            Collection<Class> listenerClasses =
                    GuacamoleProperties.getProperty(BasicGuacamoleProperties.EVENT_LISTENERS);

            // Add an instance of each class to the list
            if (listenerClasses != null) {
                for (Class listenerClass : listenerClasses) {

                    // Instantiate listener
                    Object listener = listenerClass.getConstructor().newInstance();

                    // Add listener to collection of listeners
                    listeners.add(listener);

                }
            }

        }
        catch (InstantiationException e) {
            throw new GuacamoleException("Listener class is abstract.", e);
        }
        catch (IllegalAccessException e) {
            throw new GuacamoleException("No access to listener constructor.", e);
        }
        catch (IllegalArgumentException e) {
            // This should not happen, given there ARE no arguments
            throw new GuacamoleException("Illegal arguments to listener constructor.", e);
        }
        catch (InvocationTargetException e) {
            throw new GuacamoleException("Error while instantiating listener.", e);
        }
        catch (NoSuchMethodException e) {
            throw new GuacamoleException("Listener has no default constructor.", e);
        }
        catch (SecurityException e) {
            throw new GuacamoleException("Security restrictions prevent instantiation of listener.", e);
        }

    }

    /**
     * Returns the credentials used when the user associated with this session
     * authenticated.
     *
     * @return
     *     The credentials used when the user associated with this session
     *     authenticated.
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Replaces the credentials associated with this session with the given
     * credentials.
     *
     * @param credentials
     *     The credentials to associate with this session.
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }
    
    /**
     * Returns the UserContext associated with this session.
     *
     * @return The UserContext associated with this session.
     */
    public UserContext getUserContext() {
        return userContext;
    }

    /**
     * Replaces the user context associated with this session with the given
     * user context.
     *
     * @param userContext
     *     The user context to associate with this session.
     */
    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }
    
    /**
     * Returns the ClipboardState associated with this session.
     *
     * @return The ClipboardState associated with this session.
     */
    public ClipboardState getClipboardState() {
        return clipboardState;
    }

    /**
     * Returns a collection which iterates over instances of all listeners
     * defined in guacamole.properties. For each listener defined in
     * guacamole.properties, a new instance is created and stored in this
     * collection.
     *
     * @return A collection which iterates over instances of all listeners
     *         defined in guacamole.properties.
     */
    public Collection<Object> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    /**
     * Returns whether this session has any associated active tunnels.
     *
     * @return true if this session has any associated active tunnels,
     *         false otherwise.
     */
    public boolean hasTunnels() {
        return !tunnels.isEmpty();
    }

    /**
     * Returns a map of all active tunnels associated with this session, where
     * each key is the String representation of the tunnel's UUID. Changes to
     * this map immediately affect the set of tunnels associated with this
     * session. A tunnel need not be present here to be used by the user
     * associated with this session, but tunnels not in this set will not
     * be taken into account when determining whether a session is in use.
     *
     * @return A map of all active tunnels associated with this session.
     */
    public Map<String, GuacamoleTunnel> getTunnels() {
        return tunnels;
    }

    /**
     * Associates the given tunnel with this session, such that it is taken
     * into account when determining session activity.
     *
     * @param tunnel The tunnel to associate with this session.
     */
    public void addTunnel(GuacamoleTunnel tunnel) {
        tunnels.put(tunnel.getUUID().toString(), tunnel);
    }

    /**
     * Disassociates the tunnel having the given UUID from this session.
     *
     * @param uuid The UUID of the tunnel to disassociate from this session.
     * @return true if the tunnel existed and was removed, false otherwise.
     */
    public boolean removeTunnel(String uuid) {
        return tunnels.remove(uuid) != null;
    }

    /**
     * Updates this session, marking it as accessed.
     */
    public void access() {
        lastAccessedTime = System.currentTimeMillis();
    }

    /**
     * Returns the time this session was last accessed, as the number of
     * milliseconds since midnight January 1, 1970 GMT. Session access must
     * be explicitly marked through calls to the access() function.
     *
     * @return The time this session was last accessed.
     */
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * Closes all associated tunnels and prevents any further use of this
     * session.
     */
    public void invalidate() {

        // Close all associated tunnels, if possible
        for (GuacamoleTunnel tunnel : tunnels.values()) {
            try {
                tunnel.close();
            }
            catch (GuacamoleException e) {
                logger.debug("Unable to close tunnel.", e);
            }
        }

    }
    
}
