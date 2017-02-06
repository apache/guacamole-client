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

public class RadiusChallengeResponseField extends Field {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RadiusChallengeResponseField.class);

    /**
     * The field returned by the RADIUS challenge/response.
     */
    private static final String RADIUS_FIELD_NAME = "guac-radius-challenge-response";

    /**
     * The type of field to initialize for the challenge/response.
     */
    private static final String RADIUS_FIELD_TYPE = "GUAC_RADIUS_CHALLENGE_RESPONSE";

    /**
     * The state of the connection passed by the previous RADIUS attempt.
     */
    private final String radiusState;

    /**
     * The message the RADIUS server sent back in the challenge.
     */
    private final String replyMsg;

    /**
     * Initialize the field with the reply message and the state.
     */
    public RadiusChallengeResponseField(String replyMsg, String radiusState) {
        super(RADIUS_FIELD_NAME, RADIUS_FIELD_TYPE);
        logger.debug("Initializing the RADIUS challenge/response field: {}", replyMsg);

        this.replyMsg = replyMsg;
        this.radiusState = radiusState;

    }

    public String getRadiusState() {
        return radiusState;
    }

    public String getReplyMsg() {
        return replyMsg;
    }
}
