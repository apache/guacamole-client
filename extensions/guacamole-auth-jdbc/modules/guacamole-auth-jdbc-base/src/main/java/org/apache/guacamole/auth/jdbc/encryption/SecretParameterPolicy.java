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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.protocols.ProtocolInfo;

/**
 * Decides which parameter names hold secrets. Independent of protocol: a
 * parameter name means the same thing in every protocol.
 */
public class SecretParameterPolicy {

    /**
     * Secret parameters that protocol descriptors do not type as PASSWORD.
     */
    public static final List<String> BUILT_IN = Collections.unmodifiableList(
            Arrays.asList("private-key", "sftp-private-key", "client-key"));

    /**
     * Every secret parameter name.
     */
    private final Set<String> names;

    /**
     * Creates a policy treating as secret the built-in names, every field
     * of type PASSWORD in the given protocols, and the given extra names.
     *
     * @param protocols
     *     The protocols whose PASSWORD fields are secret.
     *
     * @param extraNames
     *     Comma-separated additional secret names, or null.
     */
    public SecretParameterPolicy(Collection<ProtocolInfo> protocols,
            String extraNames) {

        Set<String> secret = new HashSet<>(BUILT_IN);
        for (ProtocolInfo protocol : protocols) {
            addPasswordFields(secret, protocol.getConnectionForms());
            addPasswordFields(secret, protocol.getSharingProfileForms());
        }

        if (extraNames != null)
            for (String name : extraNames.split(","))
                if (!name.trim().isEmpty())
                    secret.add(name.trim());

        this.names = Collections.unmodifiableSet(secret);

    }

    /**
     * Adds the name of every PASSWORD field in the given forms.
     *
     * @param secret
     *     The set receiving the names.
     *
     * @param forms
     *     The forms to inspect, or null.
     */
    private static void addPasswordFields(Set<String> secret, Collection<Form> forms) {
        if (forms == null)
            return;
        for (Form form : forms)
            for (Field field : form.getFields())
                if (Field.Type.PASSWORD.equals(field.getType()))
                    secret.add(field.getName());
    }

    /**
     * Returns whether the parameter with the given name holds a secret.
     *
     * @param name
     *     The parameter name.
     *
     * @return
     *     true if the parameter is secret, false otherwise.
     */
    public boolean isSecret(String name) {
        return names.contains(name);
    }

    /**
     * Returns every secret parameter name.
     *
     * @return
     *     An unmodifiable set of secret parameter names.
     */
    public Set<String> getSecretNames() {
        return names;
    }

}
