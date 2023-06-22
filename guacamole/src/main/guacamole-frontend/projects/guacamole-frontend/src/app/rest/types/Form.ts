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

import { Field } from './Field';

/**
 * The object returned by REST API calls when representing the data
 * associated with a form or set of configuration parameters.
 */
export class Form {
    /**
     * The name which uniquely identifies this form, or null if this form
     * has no name.
     */
    name?: string;

    /**
     * All fields contained within this form.
     */
    fields: Field[];

    /**
     * Create a new Form.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Form.
     */
    constructor(template: Partial<Form> = {}) {
        this.name = template.name;
        this.fields = template.fields || [];
    }
}
