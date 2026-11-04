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

package org.apache.guacamole.cluster;

/**
 * Receives a request to close a tunnel which may or may not be owned by this
 * replica. Requests are broadcast to every replica; an implementation closes
 * the tunnel only if it owns it, and otherwise does nothing.
 */
public interface ClusterKillHandler {

    /**
     * Closes the tunnel with the given history record UUID, if this replica
     * owns it.
     *
     * @param recordUuid
     *     The UUID of the history record identifying the tunnel to close.
     */
    void killLocalTunnel(String recordUuid);

}
