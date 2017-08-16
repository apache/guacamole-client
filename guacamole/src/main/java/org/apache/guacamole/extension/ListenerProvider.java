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

import org.apache.guacamole.net.event.listener.AuthenticationFailureListener;
import org.apache.guacamole.net.event.listener.AuthenticationSuccessListener;
import org.apache.guacamole.net.event.listener.TunnelCloseListener;
import org.apache.guacamole.net.event.listener.TunnelConnectListener;

/**
 * A provider of an event listener. While an implementation of this interface
 * must implement all of the specified listener interfaces, an implementation
 * may selectively deliver event notifications to an underlying delegate based
 * on the specific listener interfaces implemented by the delegate.
 */
public interface ListenerProvider extends AuthenticationSuccessListener,
        AuthenticationFailureListener, TunnelConnectListener,
        TunnelCloseListener {
}
