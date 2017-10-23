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

package org.apache.guacamole.auth.sqlserver;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * A property whose value is a SQLServerDriver.  The incoming string values of "jtds", "datadirect",
 * "microsoft", and "microsoft2005" into the corresponding SQLServerDriver enum value.  Any
 * values that are not valid result in a parse error.
 */
public abstract class SQLServerDriverProperty implements GuacamoleProperty<SQLServerDriver> {

    @Override
    public SQLServerDriver parseValue(String value) throws GuacamoleException {

        // If no value provided, return null.
        if (value == null)
            return null;

        // jTDS Driver
        if (value.equals("jtds"))
            return SQLServerDriver.JTDS;

        // Progress DataDirect Driver
        if (value.equals("datadirect"))
            return SQLServerDriver.DATA_DIRECT;

        // Microsoft Legacy Driver
        if (value.equals("microsoft"))
            return SQLServerDriver.MICROSOFT_LEGACY;

        // Microsoft 2005 Driver
        if (value.equals("microsoft2005"))
            return SQLServerDriver.MICROSOFT_2005;

        throw new GuacamoleServerException("SQLServer driver must be one of \"jtds\", \"datadirect\", \"microsoft\", \"microsoft2005\".");

    }

}
