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

package org.apache.guacamole.auth.common.connection;

import java.util.Date;
import java.util.Set;
import org.apache.guacamole.auth.common.base.ChildObjectModelInterface;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;

/**
 * 
 * Object representation of a Guacamole connection, as represented in the
 * database.
 */
public interface ConnectionModelInterface extends ChildObjectModelInterface {

    /**
     * Returns the name of the protocol to use when connecting to this
     * connection.
     *
     * @return The name of the protocol to use when connecting to this
     *         connection.
     */
    public String getProtocol();

    /**
     * Returns the name associated with this connection.
     *
     * @return The name associated with this connection.
     */
    public String getName();

    /**
     * Sets the name associated with this connection.
     *
     * @param name
     *            The name to associate with this connection.
     */
    public void setName(String name);

    /**
     * Sets the name of the protocol to use when connecting to this connection.
     *
     * @param protocol
     *            The name of the protocol to use when connecting to this
     *            connection.
     */
    public void setProtocol(String protocol);

    /**
     * Returns the identifiers of all readable sharing profiles associated with
     * this connection. This is set only when the connection is queried, and has
     * no effect when a connection is inserted, updated, or deleted.
     *
     * @return The identifiers of all readable sharing profiles associated with
     *         this connection.
     */
    public Set<String> getSharingProfileIdentifiers();

    /**
     * Returns the date and time that this connection was last used, or null if
     * this connection has never been used.
     *
     * @return The date and time that this connection was last used, or null if
     *         this connection has never been used.
     */
    public Date getLastActive();

    /**
     * Returns the maximum number of connections that can be established to this
     * connection concurrently.
     *
     * @return The maximum number of connections that can be established to this
     *         connection concurrently, zero if no restriction applies, or null
     *         if the default restrictions should be applied.
     */
    public Integer getMaxConnections();

    /**
     * Returns the hostname of the guacd instance to use. If the hostname of the
     * default guacd instance should be used instead, null is returned.
     *
     * @return The hostname of the guacd instance to use, or null if the
     *         hostname of the default guacd instance should be used.
     */
    public String getProxyHostname();

    /**
     * Returns the port of the guacd instance to use. If the port of the default
     * guacd instance should be used instead, null is returned.
     *
     * @return The port of the guacd instance to use, or null if the port of the
     *         default guacd instance should be used.
     */
    public Integer getProxyPort();

    /**
     * Returns the type of encryption required by the desired guacd instance. If
     * the encryption method of the default guacd instance should be used
     * instead, null is returned.
     *
     * @return The type of encryption required by the desired guacd instance, or
     *         null if the encryption method of the default guacd instance
     *         should be used.
     */
    public EncryptionMethod getProxyEncryptionMethod();

    /**
     * Returns the maximum number of connections that can be established to this
     * connection concurrently by any one user.
     *
     * @return The maximum number of connections that can be established to this
     *         connection concurrently by any one user, zero if no restriction
     *         applies, or null if the default restrictions should be applied.
     */
    public Integer getMaxConnectionsPerUser();

    /**
     * Returns the connection weight used in applying weighted load balancing
     * algorithms.
     *
     * @return The connection weight used in applying weighted load balancing
     *         aglorithms.
     */
    public Integer getConnectionWeight();

    /**
     * Returns whether this connection should be reserved for failover.
     * Failover-only connections within a balancing group are only used when all
     * non-failover connections are unavailable.
     *
     * @return true if this connection should be reserved for failover, false
     *         otherwise.
     */
    public boolean isFailoverOnly();

    /**
     * Sets the maximum number of connections that can be established to this
     * connection concurrently.
     *
     * @param maxConnections
     *            The maximum number of connections that can be established to
     *            this connection concurrently, zero if no restriction applies,
     *            or null if the default restrictions should be applied.
     */
    public void setMaxConnections(Integer parse);

    /**
     * Sets the maximum number of connections that can be established to this
     * connection concurrently by any one user.
     *
     * @param maxConnectionsPerUser
     *            The maximum number of connections that can be established to
     *            this connection concurrently by any one user, zero if no
     *            restriction applies, or null if the default restrictions
     *            should be applied.
     */
    public void setMaxConnectionsPerUser(Integer parse);

    /**
     * Sets the hostname of the guacd instance to use.
     *
     * @param proxyHostname
     *            The hostname of the guacd instance to use, or null if the
     *            hostname of the default guacd instance should be used.
     */
    public void setProxyHostname(String parse);

    /**
     * Sets the port of the guacd instance to use.
     *
     * @param proxyPort
     *            The port of the guacd instance to use, or null if the port of
     *            the default guacd instance should be used.
     */
    public void setProxyPort(Integer parse);

    /**
     * Sets the type of encryption which should be used when connecting to
     * guacd, if any.
     *
     * @param proxyEncryptionMethod
     *            The type of encryption required by the desired guacd instance,
     *            or null if the encryption method of the default guacd instance
     *            should be used.
     */
    public void setProxyEncryptionMethod(EncryptionMethod none);

    /**
     * Sets the connection weight for load balancing.
     *
     * @param connectionWeight
     *            The weight of the connection used in load balancing. The value
     *            is not required for the connection (null), and values less
     *            than 1 will prevent the connection from being used.
     */
    public void setConnectionWeight(Integer parse);

    /**
     * Sets whether this connection should be reserved for failover.
     * Failover-only connections within a balancing group are only used when all
     * non-failover connections are unavailable.
     *
     * @param failoverOnly
     *            true if this connection should be reserved for failover, false
     *            otherwise.
     */
    public void setFailoverOnly(boolean equals);

    /**
     * Sets the date and time that this connection was last used. This value is
     * expected to be set automatically via queries, derived from connection
     * history records. It does NOT correspond to an actual column, and values
     * set manually through invoking this function will not persist.
     *
     * @param lastActive
     *            The date and time that this connection was last used, or null
     *            if this connection has never been used.
     */
    public void setLastActive(Date lastActive);

}
