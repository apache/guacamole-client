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

package org.apache.guacamole.net.auth;

import org.apache.guacamole.GuacamoleException;

public interface Work extends Identifiable, Attributes, Permissions {
    /**
     *
     * @return
     *     The set of all readable works of which this work is a
     *     member.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the works.
     */
    RelatedObjectSet getWorks() throws GuacamoleException;
}
