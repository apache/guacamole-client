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

package org.apache.guacamole.auth.radius.form;

import org.apache.guacamole.form.Field;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadiusStateField extends Field {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RadiusStateField.class);

    /**
     * The parameter returned by the RADIUS state.
     */
    public static final String PARAMETER_NAME = "guac-radius-state";

    /**
     * The type of field to initialize for the state.
     */
    private static final String RADIUS_FIELD_TYPE = "GUAC_RADIUS_STATE";

    /**
     * The state of the connection passed by the previous RADIUS attempt.
     */
    private final String radiusState;

    /**
     * Initialize the field with the reply message and the state.
     */
    public RadiusStateField(String radiusState) {
        super(PARAMETER_NAME, RADIUS_FIELD_TYPE);
        logger.debug("Initializing the RADIUS state field: {}", radiusState);

        this.radiusState = radiusState;

    }

    public String getRadiusState() {
        return radiusState;
    }

}
