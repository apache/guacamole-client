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

package org.apache.guacamole.auth.ldap.conf;

import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty with a value of AliasDerefMode. The possible strings
 * "never", "searching", "finding", and "always" are mapped to their values as
 * an AliasDerefMode object. Anything else results in a parse error.
 */
public abstract class DereferenceAliasesProperty implements GuacamoleProperty<AliasDerefMode> {

    @Override
    public AliasDerefMode parseValue(String value) throws GuacamoleException {

        // No value provided, so return null.
        if (value == null)
            return null;

        // Never dereference aliases
        if (value.equals("never"))
            return AliasDerefMode.NEVER_DEREF_ALIASES;

        // Dereference aliases during search operations, but not at base
        if (value.equals("searching"))
            return AliasDerefMode.DEREF_IN_SEARCHING;

        // Dereference aliases to locate base, but not during searches
        if (value.equals("finding"))
            return AliasDerefMode.DEREF_FINDING_BASE_OBJ;

        // Always dereference aliases
        if (value.equals("always"))
            return AliasDerefMode.DEREF_ALWAYS;

        // Anything else is invalid and results in an error
        throw new GuacamoleServerException("Dereference aliases must be one of \"never\", \"searching\", \"finding\", or \"always\".");

    }

}
