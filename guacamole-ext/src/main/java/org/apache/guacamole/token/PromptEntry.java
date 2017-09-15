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

package org.apache.guacamole.token;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.form.Field;

public class PromptEntry {

    private Field field;

    private String value;

    private Boolean wholeParameter;

    private List<String> positions;

    public PromptEntry(Field field, String value, Boolean wholeParameter,
            List<String> positions) {
        this.field = field;
        this.value = value;
        this.wholeParameter = wholeParameter;
        this.positions = positions;
    }

    public PromptEntry(Field field) {
        this.field = field;
        this.value = "-1";
        this.wholeParameter = true;
        this.positions = Collections.<String>singletonList("-1");
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getWholeParameter() {
        return wholeParameter;
    }

    public void setWholeParameter(Boolean wholeParameter) {
        this.wholeParameter = wholeParameter;
    }

    public List<String> getPositions() {
        return positions;
    }

    public void setPositions(List<String> positions) {
        this.positions = positions;
    }

}
