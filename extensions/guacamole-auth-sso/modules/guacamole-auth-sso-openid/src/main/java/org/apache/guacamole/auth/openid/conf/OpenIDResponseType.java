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

package org.apache.guacamole.auth.openid.conf;

import org.apache.guacamole.properties.EnumGuacamoleProperty.PropertyValue;

/**
 * This enum represents the valid OIDC reponse types that can be used
 */
 public enum OpenIDResponseType {
     /*
      * Response type "id_token" used for implciit flow as specified in the OpenID standard
      */
     @PropertyValue("id_token")
     ID_TOKEN("id_token"),
     
     /* Response type "token" used for implicit flow by certain Identity Providers, notably
      * AWS Cognito. This corresponds to the official OIDC response_type "id_token token" 
      * that returns both the "id_token" and "access_token" parameters
      */
     @PropertyValue("token")
     TOKEN("token"),
     
     /**
      * Response type "code" used for code flow authentication
      */
     @PropertyValue("code")
     CODE("code");
     
     /*
      * The string value of the response type used
      */
     public final String STRING_VALUE;
     
    /**
     * Initializes the response type such that it is associated with the
     * given string value.
     *
     * @param value
     *     The string value that will be associated with the enum value.
     */
    private OpenIDResponseType(String value) {
        this.STRING_VALUE = value;
    }
    
    @Override
    public String toString() {
        return STRING_VALUE;
    }
}
