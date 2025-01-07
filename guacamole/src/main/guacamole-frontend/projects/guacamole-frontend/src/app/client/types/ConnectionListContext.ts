

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

/**
 * Arbitrary context that should be exposed to the guacGroupList directive
 * displaying the dropdown list of available connections within the
 * Guacamole menu.
 */
export interface ConnectionListContext {

    /**
     * The set of clients desired within the current view. For each client
     * that should be present within the current view, that client's ID
     * will map to "true" here.
     */
    attachedClients: Record<string, boolean>;

    /**
     * Notifies that the client with the given ID has been added or
     * removed from the set of clients desired within the current view,
     * and the current view should be updated accordingly.
     *
     * @param id
     *     The ID of the client that was added or removed from the current
     *     view.
     */
    updateAttachedClients(id: string): void;

}
