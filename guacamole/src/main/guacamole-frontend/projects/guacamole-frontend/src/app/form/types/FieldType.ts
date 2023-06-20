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

import { FormFieldBaseComponent } from '../components/form-field-base/form-field-base.component';
import { Type } from '@angular/core';
import { CheckboxFieldComponent } from '../components/checkbox-field/checkbox-field.component';

/**
 * The object used by the formService for describing field types.
 *
 * TODO: Build an Angular version of this class.
 */
export class FieldType {

    component: Type<FormFieldBaseComponent>;

    /**
     * Creates a new FieldType.
     *
     * @param {FieldType|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     FieldType.
     */
    constructor(template: Partial<FieldType> & object = {}) {

        // Copy template values
        // TODO: Change default to text field
        this.component = template.component || CheckboxFieldComponent;
    }
}
