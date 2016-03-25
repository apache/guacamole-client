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

package org.apache.guacamole.protocols;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.guacamole.form.Form;

/**
 * Describes a protocol and all forms associated with it, as required by
 * a protocol plugin for guacd. This class allows known forms for a
 * protocol to be exposed to the user as friendly fields.
 *
 * @author Michael Jumper
 */
public class ProtocolInfo {

    /**
     * The unique name associated with this protocol.
     */
    private String name;

    /**
     * A collection of all associated protocol forms.
     */
    private Collection<Form> forms;

    /**
     * Creates a new ProtocolInfo with no associated name or forms.
     */
    public ProtocolInfo() {
        this.forms = new ArrayList<Form>();
    }

    /**
     * Creates a new ProtocolInfo having the given name, but without any forms.
     *
     * @param name
     *     The unique name associated with the protocol.
     */
    public ProtocolInfo(String name) {
        this.name  = name;
        this.forms = new ArrayList<Form>();
    }

    /**
     * Creates a new ProtocolInfo having the given name and forms.
     *
     * @param name
     *     The unique name associated with the protocol.
     *
     * @param forms
     *     The forms to associate with the protocol.
     */
    public ProtocolInfo(String name, Collection<Form> forms) {
        this.name  = name;
        this.forms = forms;
    }

    /**
     * Returns the unique name of this protocol. The protocol name is the
     * value required by the corresponding protocol plugin for guacd.
     *
     * @return The unique name of this protocol.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique name of this protocol. The protocol name is the value
     * required by the corresponding protocol plugin for guacd.
     *
     * @param name The unique name of this protocol.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a mutable collection of the protocol forms associated with
     * this protocol. Changes to this collection affect the forms exposed
     * to the user.
     *
     * @return A mutable collection of protocol forms.
     */
    public Collection<Form> getForms() {
        return forms;
    }

    /**
     * Sets the collection of protocol forms associated with this
     * protocol.
     *
     * @param forms
     *     The collection of forms to associate with this protocol.
     */
    public void setForms(Collection<Form> forms) {
        this.forms = forms;
    }
    
}
