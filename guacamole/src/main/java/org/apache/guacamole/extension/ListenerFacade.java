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

package org.apache.guacamole.extension;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.event.AuthenticationFailureEvent;
import org.apache.guacamole.net.event.AuthenticationSuccessEvent;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.apache.guacamole.net.event.listener.*;

/**
 * Provides a wrapper around a Listener subclass, allowing listener
 * extensions to be bound without regard for which specific listener interfaces
 * are implemented.
 */
class ListenerFacade implements ListenerProvider {

    private final Listener delegate;

    /**
     * Creates a new ListenerFacade which delegates all listener methods
     * calls to an instance of the given Listener subclass. If
     * an instance of the given class cannot be created, creation of this
     * facade will still succeed. Errors will be logged at the time listener
     * creation fails, but subsequent events directed to the listener will be
     * silently dropped.
     *
     * @param listenerClass
     *     The Listener subclass to instantiate.
     */
    public ListenerFacade(Class<? extends Listener> listenerClass) {
        delegate = ProviderFactory.newInstance("listener", listenerClass);
    }

    /**
     * Notifies the delegate listener of an authentication success event, if the
     * listener implements the AuthenticationSuccessListener interface.
     *
     * @param
     *      e The AuthenticationSuccessEvent describing the authentication
     *        success that just occurred.
     * @return
     *      false if the delegate listener rejects the successful authentication,
     *      else true
     *
     * @throws GuacamoleException
     *      if the delegate listener throws this exception
     */
    @Override
    public boolean authenticationSucceeded(AuthenticationSuccessEvent e)
            throws GuacamoleException {
        return !(delegate instanceof AuthenticationSuccessListener)
                || ((AuthenticationSuccessListener) delegate).authenticationSucceeded(e);
    }

    /**
     * Notifies the delegate listener of an authentication failure event, if the
     * listener implements the AuthenticationSuccessListener interface.
     *
     * @param
     *      e The AuthenticationFailureEvent describing the authentication
     *        failure that just occurred.
     *
     * @throws GuacamoleException
     *      if the delegate listener throws this exception
     */
    @Override
    public void authenticationFailed(AuthenticationFailureEvent e)
            throws GuacamoleException {
        if (delegate instanceof AuthenticationFailureListener) {
            ((AuthenticationFailureListener) delegate).authenticationFailed(e);
        }
    }

    /**
     * Notifies the delegate listener of a tunnel connected event, if the
     * listener implements the TunnelConnectListener interface.
     *
     * @param
     *      e The TunnelConnectEvent describing the tunnel that was just connected

     * @return
     *      false if the delegate listener rejects the tunnel connection,
     *      else true
     *
     * @throws GuacamoleException
     *      if the delegate listener throws this exception
     */
    @Override
    public boolean tunnelConnected(TunnelConnectEvent e)
            throws GuacamoleException {
        return !(delegate instanceof TunnelConnectListener)
                || ((TunnelConnectListener) delegate).tunnelConnected(e);
    }

    /**
     * Notifies the delegate listener of a tunnel close event, if the
     * listener implements the TunnelCloseListener interface.
     *
     * @param
     *      e The TunnelCloseEvent describing the tunnel that is to be close

     * @return
     *      false if the delegate listener rejects the tunnel close request,
     *      else true
     *
     * @throws GuacamoleException
     *      if the delegate listener throws this exception
     */
    @Override
    public boolean tunnelClosed(TunnelCloseEvent e) throws GuacamoleException {
        return !(delegate instanceof TunnelCloseListener)
                || ((TunnelCloseListener) delegate).tunnelClosed(e);
    }

}
