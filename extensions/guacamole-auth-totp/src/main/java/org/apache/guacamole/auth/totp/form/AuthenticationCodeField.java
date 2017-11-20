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

package org.apache.guacamole.auth.totp.form;

import org.apache.guacamole.form.Field;

/**
 * Field which prompts the user for an authentication code generated via TOTP.
 */
public class AuthenticationCodeField extends Field {

    /**
     * The name of the HTTP parameter which will contain the TOTP code provided
     * by the user to verify their identity.
     */
    public static final String PARAMETER_NAME = "guac-totp";

    /**
     * The unique name associated with this field type.
     */
    private static final String FIELD_TYPE_NAME = "GUAC_TOTP_CODE";

    /**
     * Creates a new field which prompts the user for an authentication code
     * generated via TOTP.
     */
    public AuthenticationCodeField() {
        super(PARAMETER_NAME, FIELD_TYPE_NAME);
    }

}
