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

package org.apache.guacamole.vault.openbao.user;

import org.apache.guacamole.form.Form;
import org.apache.guacamole.vault.conf.VaultAttributeService;

import java.util.Collection;
import java.util.Collections;

/**
 * OpenBao implementation of VaultAttributeService.
 * Defines attributes that trigger OpenBao secret lookups.
 */
public class OpenBaoAttributeService implements VaultAttributeService {

    @Override
    public Collection<Form> getConnectionAttributes() {
        // No additional connection attributes needed for OpenBao
        // The password field in RDP connections will automatically use OPENBAO:password token
        return Collections.emptyList();
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        // No additional connection group attributes
        return Collections.emptyList();
    }

    @Override
    public Collection<Form> getUserAttributes() {
        // No additional user attributes
        return Collections.emptyList();
    }

    @Override
    public Collection<Form> getUserPreferenceAttributes() {
        // No additional user preference attributes
        return Collections.emptyList();
    }
}
