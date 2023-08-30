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

package org.apache.guacamole.auth.jdbc.work;


import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.guacamole.auth.jdbc.base.ModeledChildDirectoryObject;
import org.apache.guacamole.form.DateField;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModeledWork extends ModeledChildDirectoryObject<WorkModel> implements Work {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ModeledWork.class);

    /**
     * The name of the attribute which date to start work.
     */
    public static final String START_DATE_NAME = "start-date";

    /**
     * The name of the attribute which date to end work.
     */
    public static final String END_DATE_NAME = "end-date";

    /**
     * All attributes related to restricting user accounts, within a logical
     * form.
     */
    public static final Form PERIOD = new Form("period", Arrays.<Field>asList(
        new DateField(START_DATE_NAME),
        new DateField(END_DATE_NAME)
    ));

    /**
     * All possible attributes of connection group objects organized as
     * individual, logical forms.
     */
    public static final Collection<Form> ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(
        PERIOD
    ));

    /**
     * The names of all attributes which are explicitly supported by this
     * extension's ConnectionGroup objects.
     */
    public static final Set<String> ATTRIBUTE_NAMES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                START_DATE_NAME,
                END_DATE_NAME
            )));

    @Override
    public String getName() {
        return getModel().getName();
    }

    @Override
    public void setName(String name) {
        getModel().setName(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        
        // Include any defined arbitrary attributes
        Map<String, String> attributes = super.getAttributes();
        
        // Set start date attribute
        attributes.put(START_DATE_NAME, DateField.format(getModel().getStartDate()));

        // Set end date attribute
        attributes.put(END_DATE_NAME, DateField.format(getModel().getEndDate()));

        return attributes;   
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {

        // Set arbitrary attributes
        super.setAttributes(attributes);
        
        // Translate start date attribute
        try {
            getModel().setStartDate(DateField.parse(attributes.get(START_DATE_NAME)));
        }
        catch (ParseException e) {
            logger.warn("Ignoring invalid start date \"{}\" for work \"{}\".", attributes.get(START_DATE_NAME), getName());
        }

        // Translate end date attribute
        try {
            getModel().setEndDate(DateField.parse(attributes.get(END_DATE_NAME)));
        }
        catch (ParseException e) {
            logger.warn("Ignoring invalid end date \"{}\" for work \"{}\".", attributes.get(END_DATE_NAME), getName());
        }
    }

}