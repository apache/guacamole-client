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

package org.apache.guacamole.auth.ldap;

/**
 * Acceptable values for configuring the dereferencing of aliases in
 * talking to LDAP servers.
 */
public enum DereferenceAliases {

    /**
     * Never dereference aliases.  This is the default.
     */
    NEVER(0),

    /**
     * Aliases are dereferenced below the base object, but not to locate
     * the base object itself.  So, if the base object is itself an alias
     * the search will not complete.
     */
    SEARCHING(1),

    /**
     * Aliases are only dereferenced to locate the base object, but not
     * after that.  So, a search against a base object that is an alias will
     * find any subordinates of the real object the aliase references, but
     * further aliases in the search will not be dereferenced.
     */
    FINDING(2),

    /**
     * Aliases will always be dereferenced, both to locate the base object
     * and when handling results returned by the search.
     */
    ALWAYS(3);

    /**
     * The integer value that the enum represents, which is used in
     * configuring the JLDAP library.
     */
    public final int DEREF_VALUE;

    /**
     * Initializes the dereference aliases object with the integer
     * value the setting maps to per the JLDAP implementation.
     *
     * @param derefValue
     *     The value associated with this dereference setting
     */
    private DereferenceAliases(int derefValue) {
        this.DEREF_VALUE = derefValue;
    }

}
