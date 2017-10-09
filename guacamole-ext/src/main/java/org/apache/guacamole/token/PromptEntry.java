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

/**
 * A class that collects all of the information required to
 * to display a prompt to the user during client connection.
 */
public class PromptEntry {

    /**
     * The Field data associated with the prompt.
     */
    private Field field;

    /**
     * A 0-indexed list of String data up to the position of each
     * prompt.
     */
    private List<String> positions;

    /**
     * Constructor that collects all of the data and assigns it.
     *
     * @param field
     *     The Field object for the prompt.
     *
     * @param positions
     *     0-indexed list of text leading up to the position of each
     *     prompt.
     */
    public PromptEntry(Field field, List<String> positions) {

        this.field = field;
        this.positions = positions;
    }

    /**
     * Constructor that takes only field data and fills in defaults
     * for the rest of the entries, assuming the entire parameter
     * is being prompted.
     *
     * @param field
     * Field object for this prompt.
     */
    public PromptEntry(Field field) {
        this.field = field;
        this.positions = Collections.<String>singletonList("");
    }

    /**
     * Return the Field object for this PromptEntry.
     *
     * @return
     *     The Field object for this prompt.
     */
    public Field getField() {
        return field;
    }

    /**
     * Set the Field object for this PromptEntry.
     *
     * @param field
     *     The Field object for this prompt.
     */
    public void setField(Field field) {
        this.field = field;
    }

    /**
     * Return the list of positions which is
     * a 0-indexed list of strings where the
     * string is any text preceeding the prompt
     * token.
     *
     * @return
     *     A 0-indexed list of strings, where
     *     each string is any text leading up to
     *     the prompt token.
     */
    public List<String> getPositions() {
        return positions;
    }

    /**
     * Set the list of positions, where the list
     * is a 0-indexed list of Strings and where
     * the string object is any text leading
     * up to the prompt token.
     *
     * @param positions
     *     A 0-indexed list of Strings, where
     *     each string is any text leading up to
     *     the prompt token.
     */
    public void setPositions(List<String> positions) {
        this.positions = positions;
    }

}
