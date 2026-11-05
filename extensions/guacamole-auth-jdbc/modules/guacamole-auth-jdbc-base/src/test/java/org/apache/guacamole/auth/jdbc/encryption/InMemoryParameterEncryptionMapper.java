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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Single "connection" table; sharing profiles are always empty. */
class InMemoryParameterEncryptionMapper implements ParameterEncryptionMapper {

    final List<ParameterRow> rows = new ArrayList<>();

    /** Hook run before each update, to simulate a concurrent writer. */
    Runnable beforeUpdate = () -> {};

    static ParameterRow row(String id, String name, String value, String kid) {
        ParameterRow r = new ParameterRow();
        r.setOwnerIdentifier(id); r.setName(name); r.setValue(value); r.setEncryption(kid);
        return r;
    }

    private static ParameterRow copy(ParameterRow r) {
        return row(r.getOwnerIdentifier(), r.getName(), r.getValue(), r.getEncryption());
    }

    @Override
    public synchronized List<ParameterRow> selectConnectionCandidates(Collection<String> names,
            String currentKeyId, boolean decrypt) {
        List<ParameterRow> out = new ArrayList<>();
        for (ParameterRow r : rows) {
            boolean match = decrypt ? r.getEncryption() != null
                    : r.getEncryption() != null ? !r.getEncryption().equals(currentKeyId)
                    : names.contains(r.getName()) && !r.getValue().isEmpty();
            if (match) out.add(copy(r));
        }
        return out;
    }

    @Override
    public List<ParameterRow> selectSharingProfileCandidates(Collection<String> names,
            String currentKeyId, boolean decrypt) {
        return new ArrayList<>();
    }

    @Override
    public int updateConnectionParameter(ParameterRow old, String newValue, String newEncryption) {
        beforeUpdate.run();
        synchronized (this) {
            for (ParameterRow r : rows)
                if (r.getOwnerIdentifier().equals(old.getOwnerIdentifier())
                        && r.getName().equals(old.getName())
                        && r.getValue().equals(old.getValue())
                        && Objects.equals(r.getEncryption(), old.getEncryption())) {
                    r.setValue(newValue); r.setEncryption(newEncryption);
                    return 1;
                }
            return 0;
        }
    }

    @Override
    public int updateSharingProfileParameter(ParameterRow old, String newValue, String newEncryption) {
        return 0;
    }

    @Override
    public synchronized int countConnectionEncrypted(String otherThanKeyId) {
        int n = 0;
        for (ParameterRow r : rows)
            if (r.getEncryption() != null && !r.getEncryption().equals(otherThanKeyId))
                n++;
        return n;
    }

    @Override
    public int countSharingProfileEncrypted(String otherThanKeyId) {
        return 0;
    }

}
