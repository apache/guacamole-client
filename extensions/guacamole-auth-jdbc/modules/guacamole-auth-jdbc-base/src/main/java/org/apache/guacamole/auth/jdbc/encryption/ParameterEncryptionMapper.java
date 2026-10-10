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

package org.apache.guacamole.auth.jdbc.encryption;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper used by the parameter encryption sweep to find stored parameters
 * that do not match the configured encryption, and to rewrite them with a
 * compare-and-set.
 */
public interface ParameterEncryptionMapper {

    /**
     * Returns the connection parameters the sweep must handle: in encrypt
     * mode, secret parameters that are plain text or on a non-current key;
     * in decrypt mode, every encrypted parameter.
     *
     * @param names
     *     The secret parameter names. Never empty.
     *
     * @param currentKeyId
     *     The ID of the current key.
     *
     * @param decrypt
     *     Whether the sweep is restoring plain text.
     *
     * @return
     *     The candidate rows.
     */
    List<ParameterRow> selectConnectionCandidates(@Param("names") Collection<String> names,
            @Param("currentKeyId") String currentKeyId, @Param("decrypt") boolean decrypt);

    /**
     * Same as selectConnectionCandidates(), for sharing profile parameters.
     *
     * @param names
     *     The secret parameter names. Never empty.
     *
     * @param currentKeyId
     *     The ID of the current key.
     *
     * @param decrypt
     *     Whether the sweep is restoring plain text.
     *
     * @return
     *     The candidate rows.
     */
    List<ParameterRow> selectSharingProfileCandidates(@Param("names") Collection<String> names,
            @Param("currentKeyId") String currentKeyId, @Param("decrypt") boolean decrypt);

    /**
     * Replaces a connection parameter only if it still holds the value and
     * encryption the sweep read.
     *
     * @param row
     *     The row as read.
     *
     * @param newValue
     *     The value to store.
     *
     * @param newEncryption
     *     The key ID of the new value, or null for plain text.
     *
     * @return
     *     1 if the row was replaced, 0 if it changed since it was read.
     */
    int updateConnectionParameter(@Param("row") ParameterRow row,
            @Param("newValue") String newValue, @Param("newEncryption") String newEncryption);

    /**
     * Same as updateConnectionParameter(), for sharing profile parameters.
     *
     * @param row
     *     The row as read.
     *
     * @param newValue
     *     The value to store.
     *
     * @param newEncryption
     *     The key ID of the new value, or null for plain text.
     *
     * @return
     *     1 if the row was replaced, 0 if it changed since it was read.
     */
    int updateSharingProfileParameter(@Param("row") ParameterRow row,
            @Param("newValue") String newValue, @Param("newEncryption") String newEncryption);

}
