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

package org.apache.guacamole.auth.disclaimer.form;

import java.util.Collections;
import java.util.Date;
import org.apache.guacamole.form.Field;

/**
 * A Field that implements a general login disclaimer that must be acknowledged
 * by the user prior to logging in.
 */
public class DisclaimerField extends Field {
    
    /**
     * The name of the HTTP parameter that will contain a boolean value of
     * "true" when the user has acknowledged the disclaimer.
     */
    public static final String PARAMETER_NAME = "guac-acknowledged";
    
    /**
     * The value we should look for in the above parameter to know that the user
     * has acknowledged the disclaimer.
     */
    public static final String PARAMETER_TRUTH_VALUE = "acknowledged";
    
    /**
     * The unique name associated with this field type.
     */
    public static final String FIELD_TYPE_NAME = "GUAC_DISCLAIMER";
    
    /**
     * The title of the disclaimer box.
     */
    private final String title;
    
    /**
     * The text of the disclaimer.
     */
    private final String disclaimer;
    
    /**
     * The last login of the user, or null if the last login is unknown.
     */
    private final Date lastLogin;
    
    /**
     * Whether or not to show the user's last login date.
     */
    private final boolean showLastLogin;
    
    /**
     * Create a new Disclaimer field that the user will be required to
     * acknowledge prior to being able to log in, with the given title,
     * disclaimer body, and last login date of the user (if any).
     * 
     * @param title
     *     The title of the disclaimer dialog.
     * 
     * @param disclaimer
     *     The body of the disclaimer.
     * 
     * @param lastLogin
     *     The last login of the user, if any.
     */
    public DisclaimerField(String title, String disclaimer, Date lastLogin) {
        super(PARAMETER_NAME, FIELD_TYPE_NAME, Collections.singletonList(PARAMETER_TRUTH_VALUE));
        this.title = title;
        this.disclaimer = disclaimer;
        this.lastLogin = lastLogin;
        this.showLastLogin = (lastLogin != null);
    }
    
    /**
     * Return the title of the field.
     * 
     * @return 
     *     The title of the field.
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Return the body of the disclaimer.
     * 
     * @return 
     *     The text body of the disclaimer.
     */
    public String getDisclaimer() {
        return disclaimer;
    }
    
    /**
     * Return the date the user last logged in.
     * 
     * @return 
     *     The date of the last login.
     */
    public Date getLastLogin() {
        return lastLogin;
    }
    
    /**
     * Return true if the last login of the user should be shown, otherwise
     * false.
     * 
     * @return 
     *     true if the last login of the user should be shown, otherwise false.
     */
    public boolean getShowLastLogin() {
        return showLastLogin;
    }
    
    /**
     * Return the date of the last login as a string.
     * 
     * @return 
     *     The date of the last login as a string.
     */
    public String getLastLoginString() {
        return lastLogin.toString();
    }
    
    
    
}
