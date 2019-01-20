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

package org.apache.guacamole.auth.jdbc.connection;

import java.util.Date;
import java.util.Set;

import org.apache.guacamole.auth.jdbc.base.ChildObjectModelInterface;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;

/**
 * 
 * Object representation of a Guacamole connection, as represented in the
 * database.
 */
public interface ConnectionModelInterface extends ChildObjectModelInterface {

	public String getProtocol();
	
	public String getName();

	public void setName(String name);

	public void setProtocol(String protocol);

	public Set<String> getSharingProfileIdentifiers();

	public Date getLastActive();

	public Integer getMaxConnections();

	public String getProxyHostname();

	public Integer getProxyPort();

	public EncryptionMethod getProxyEncryptionMethod();

	public Integer getMaxConnectionsPerUser();

	public Integer getConnectionWeight();

	public boolean isFailoverOnly();

	public void setMaxConnections(Integer parse);

	public void setMaxConnectionsPerUser(Integer parse);

	public void setProxyHostname(String parse);

	public void setProxyPort(Integer parse);

	public void setProxyEncryptionMethod(EncryptionMethod none);

	public void setConnectionWeight(Integer parse);

	public void setFailoverOnly(boolean equals);

	public void setLastActive(Date lastActive);

}
