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

package org.apache.guacamole.rest.activeconnection;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.net.auth.ActiveConnection;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.rest.directory.DirectoryObjectResource;
import org.apache.guacamole.rest.directory.DirectoryObjectTranslator;

/**
 * A REST resource which abstracts the operations available on an existing
 * ActiveConnection.
 *
 * @author Michael Jumper
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActiveConnectionResource
        extends DirectoryObjectResource<ActiveConnection, APIActiveConnection> {

    /**
     * Creates a new ActiveConnectionResource which exposes the operations and
     * subresources available for the given ActiveConnection.
     *
     * @param userContext
     *     The UserContext associated with the given Directory.
     *
     * @param directory
     *     The Directory which contains the given ActiveConnection.
     *
     * @param connection
     *     The ActiveConnection that this ActiveConnectionResource should
     *     represent.
     *
     * @param translator
     *     A DirectoryObjectTranslator implementation which handles
     *     ActiveConnections.
     */
    @AssistedInject
    public ActiveConnectionResource(@Assisted UserContext userContext,
            @Assisted Directory<ActiveConnection> directory,
            @Assisted ActiveConnection connection,
            DirectoryObjectTranslator<ActiveConnection, APIActiveConnection> translator) {
        super(directory, connection, translator);
    }

}
