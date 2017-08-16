/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.guacamole.rest.event;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.extension.ListenerProvider;
import org.apache.guacamole.net.event.AuthenticationFailureEvent;
import org.apache.guacamole.net.event.AuthenticationSuccessEvent;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.net.event.listener.AuthenticationFailureListener;
import org.apache.guacamole.net.event.listener.AuthenticationSuccessListener;
import org.apache.guacamole.net.event.listener.TunnelCloseListener;
import org.apache.guacamole.net.event.listener.TunnelConnectListener;

import java.util.List;

/**
 * A service used to notify listeners registered by extensions when events of
 * interest occur.
 *
 * @author Carl Harris
 */
public class ListenerService implements ListenerProvider {

    @Inject
    private List<ListenerProvider> listeners;

    /**
     * Notifies all bound listeners of an authentication success event. Listeners
     * are allowed to veto a successful authentication by returning false from the
     * listener method. Regardless of whether a particular listener rejects the
     * successful authentication, all listeners are notified.
     * @param e
     *      The AuthenticationSuccessEvent describing the successful authentication
     *      that just occurred.
     *
     * @return
     *      false if any bound listener returns false, else true
     *
     * @throws GuacamoleException
     *      If any bound listener throws this exception. If a listener throws an exception
     *      some listeners may not receive the authentication success event notification.
     */
    @Override
    public boolean authenticationSucceeded(AuthenticationSuccessEvent e)
            throws GuacamoleException {
        boolean result = true;
        for (AuthenticationSuccessListener listener : listeners) {
            result = result && listener.authenticationSucceeded(e);
        }
        return result;
    }

    /**
     * Notifies all bound listeners of an authentication failure event.
     *
     * @param e
     *      The AuthenticationSuccessEvent describing the authentication failure
     *      that just occurred.
     *
     * @throws GuacamoleException
     *      If any bound listener throws this exception. If a listener throws an exception
     *      some listeners may not receive the authentication failure event notification.
     */
    @Override
    public void authenticationFailed(AuthenticationFailureEvent e)
            throws GuacamoleException {
        for (AuthenticationFailureListener listener : listeners) {
            listener.authenticationFailed(e);
        }
    }

    /**
     * Notifies all bound listeners of an tunnel connected event. Listeners
     * are allowed to veto a tunnel connection by returning false from the
     * listener method. Regardless of whether a particular listener rejects the
     * tunnel connection, all listeners are notified.
     * @param e
     *      The TunnelConnectedEvent describing the tunnel that was just connected
     *
     * @return
     *      false if any bound listener returns false, else true
     *
     * @throws GuacamoleException
     *      If any bound listener throws this exception. If a listener throws an exception
     *      some listeners may not receive the tunnel connected event notification.
     */
    @Override
    public boolean tunnelConnected(TunnelConnectEvent e)
            throws GuacamoleException {
        boolean result = true;
        for (TunnelConnectListener listener : listeners) {
            result = result && listener.tunnelConnected(e);
        }
        return result;
    }

    /**
     * Notifies all bound listeners of an tunnel close event. Listeners
     * are allowed to veto the request to close a tunnel by returning false from
     * the listener method. Regardless of whether a particular listener rejects the
     * tunnel close request, all listeners are notified.
     * @param e
     *      The TunnelCloseEvent describing the tunnel that is to be closed
     *
     * @return
     *      false if any bound listener returns false, else true
     *
     * @throws GuacamoleException
     *      If any bound listener throws this exception. If a listener throws an exception
     *      some listeners may not receive the tunnel close event notification.
     */
    @Override
    public boolean tunnelClosed(TunnelCloseEvent e) throws GuacamoleException {
        boolean result = true;
        for (TunnelCloseListener listener : listeners) {
            result = result && listener.tunnelClosed(e);
        }
        return result;
    }

}
