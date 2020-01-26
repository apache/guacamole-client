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

package org.apache.guacamole.auth.mysql;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * A property whose value is a MySQL-compatible JDBC driver.  The string values
 * of either "mysql" or "mariadb" are parsed into the corresponding MySQLDriver
 * enum value. Any values that are not valid result in a parse error.
 */
public abstract class MySQLDriverProperty implements GuacamoleProperty<MySQLDriver> {

    @Override
    public MySQLDriver parseValue(String value) throws GuacamoleException {

        // If no value provided, return null.
        if (value == null)
            return null;

        // MySQL Driver
        if (value.equals("mysql"))
            return MySQLDriver.MYSQL;

        // MariaDB Driver
        if (value.equals("mariadb"))
            return MySQLDriver.MARIADB;

        throw new GuacamoleServerException("MySQL driver must be one of \"mysql\" or \"mariadb\".");

    }

}