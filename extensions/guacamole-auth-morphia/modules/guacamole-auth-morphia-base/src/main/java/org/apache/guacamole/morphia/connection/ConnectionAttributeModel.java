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

package org.apache.guacamole.morphia.connection;

import org.apache.guacamole.morphia.base.ArbitraryAttributeModel;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 * 
 * guacamole_connection_attribute: { id: string, attribute_name: string,
 * attribute_value, string, connection: ConnectionModel (connection_id) }
 * 
 */
@Entity("guacamole_connection_attribute")
public class ConnectionAttributeModel extends ArbitraryAttributeModel {

    /** The connection. */
    @Reference(value = "connection")
    private ConnectionModel connection;

    /**
     * Gets the connection.
     *
     * @return the connection
     */
    public ConnectionModel getConnection() {
        return connection;
    }

    /**
     * Sets the connection.
     *
     * @param connection
     *            the new connection
     */
    public void setConnection(ConnectionModel connection) {
        this.connection = connection;
    }

}
