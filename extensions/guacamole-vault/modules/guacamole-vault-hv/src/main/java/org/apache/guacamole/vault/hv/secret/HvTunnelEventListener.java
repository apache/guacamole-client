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

package org.apache.guacamole.vault.hv.secret;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.event.listener.Listener;
import org.apache.guacamole.net.event.TunnelCloseEvent;
import org.apache.guacamole.net.event.TunnelConnectEvent;

/**
 * Listen for TunnelConnectEvent and TunnelCLoseEvent and pass to
 * HvClientProvider to see if they correspond to an active ldap
 * service account connection
 */
public class HvTunnelEventListener implements Listener {

    /**
     * Default constructor for ProviderFactory
     */
    public HvTunnelEventListener() { 
        // Constructor deliberately empty
    }

    /**
     * The LDAP session interface checks out session that then can not be
     * used till they are checked in. Use a TunnelConnectEvent listener to
     * add a checkin task to the sessions
     *
     * @param event
     *      A tunnel connect/close event
     */
    @Override
    public void handleEvent(final Object event) throws GuacamoleException {
        if (event instanceof TunnelConnectEvent || event instanceof TunnelCloseEvent) {
            HvClientProvider.treatLdapSession(event);
        }
    }
}
