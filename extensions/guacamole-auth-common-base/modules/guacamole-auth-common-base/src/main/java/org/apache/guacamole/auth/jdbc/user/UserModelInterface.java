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

package org.apache.guacamole.auth.jdbc.user;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.guacamole.auth.jdbc.base.ObjectEntityModelInterface;

/**
 * 
 * Object representation of a Guacamole user, as represented in the database.
 * 
 */
public interface UserModelInterface extends ObjectEntityModelInterface {

	public boolean isDisabled();

	public boolean isExpired();

	public Timestamp getLastActive();

	public Date getValidUntil();

	public Time getAccessWindowEnd();

	public String getTimeZone();

	public void setOrganizationalRole(String parse);

	public void setOrganization(String parse);

	public void setEmailAddress(String parse);

	public void setFullName(String parse);

	public void setTimeZone(String parse);

	public void setValidUntil(Date parseDate);

	public void setValidFrom(Date parseDate);

	public void setAccessWindowEnd(Time parseTime);

	public void setAccessWindowStart(Time parseTime);

	public void setExpired(boolean equals);

	public void setDisabled(boolean equals);

	public String getOrganizationalRole();

	public String getOrganization();

	public String getEmailAddress();

	public String getFullName();

	public void setPasswordSalt(byte[] generateSalt);

	public void setPasswordHash(byte[] generateSalt);

	public void setPasswordDate(Timestamp passwordDate);

	public byte[] getPasswordHash();

	public String getIdentifier();

	public byte[] getPasswordSalt();

	public Date getPasswordDate();

	public void setLastActive(Timestamp timestamp);

	public Date getValidFrom();

	public Time getAccessWindowStart();

}
