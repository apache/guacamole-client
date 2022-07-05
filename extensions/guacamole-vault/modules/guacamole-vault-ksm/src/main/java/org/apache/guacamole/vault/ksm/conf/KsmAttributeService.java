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

package org.apache.guacamole.vault.ksm.conf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.guacamole.form.Form;
import org.apache.guacamole.form.TextField;
import org.apache.guacamole.vault.conf.VaultAttributeService;

import com.google.inject.Singleton;

/**
 * A service that exposes KSM-specific attributes, allowing setting KSM
 * configuration through the admin interface.
 */
@Singleton
public class KsmAttributeService implements VaultAttributeService {

    /**
     * The name of the attribute which can contain a KSM configuration blob
     * associated with a connection group.
     */
    public static final String KSM_CONFIGURATION_ATTRIBUTE = "ksm-config";

    /**
     * All attributes related to configuring the KSM vault on a
     * per-connection-group basis.
     */
    public static final Form KSM_CONFIGURATION_FORM = new Form("ksm-config",
            Arrays.asList(new TextField(KSM_CONFIGURATION_ATTRIBUTE)));

    /**
     * All KSM-specific connection group attributes, organized by form.
     */
    public static final Collection<Form> KSM_CONNECTION_GROUP_ATTRIBUTES =
            Collections.unmodifiableCollection(Arrays.asList(KSM_CONFIGURATION_FORM));

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return KSM_CONNECTION_GROUP_ATTRIBUTES;
    }

}
