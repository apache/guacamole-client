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

package org.apache.guacamole.templates.form;

import org.apache.guacamole.form.Field;

/**
 * A field that allows users to choose from possible existing connections to
 * which they have access.
 */
public class ConnectionChooserField extends Field {
    
    /**
     * The field type, used to load the correct field within the AngularJS
     * application.
     */
    public static String FIELD_TYPE = "GUAC_CONNECTION_CHOOSER";
    
    /**
     * Create a new ConnectionChooserField with the specified name.
     * 
     * @param name 
     *     The name of the field.
     */
    public ConnectionChooserField(String name) {
        super(name, FIELD_TYPE);
    }
    
}
