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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.vault.openbao.secret;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.event.listener.Listener;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenBaoTunnelEventListener implements Listener {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoTunnelEventListener.class);

    /**
     * A Provider for the OpenBaoClient that is injected by Guice
     */
    @Inject
    private static Provider<OpenBaoClient> clientProvider;

    /**
     * Function to get an instance of the Singleton OpenBaoClient Class
     */
    private OpenBaoClient client() {
        return clientProvider.get();
    }

    /**
     * Default constructor for ProviderFactory
     */
    public OpenBaoTunnelEventListener() {
        logger.debug("OpenBaoTunnelEventListener constructed");
    }

    /**
     * The LDAP session interface checks out session that then can not be
     * used till they are checked in. Use a TunnelConnectEvent listener to
     * add a checkin task to the sessions
     *
     * @param event
     *      A tunnel close event
     */
    @Override
    public void handleEvent(Object event) throws GuacamoleException {
        logger.debug("Called OpenBaoListener : {}", event.getClass().getName());
        if (event instanceof TunnelConnectEvent || event instanceof TunnelCloseEvent) {
            client().connectLdapSession(event);
        }
    }
}
