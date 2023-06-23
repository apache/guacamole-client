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

import { Injectable, Type } from '@angular/core';
import { FormFieldComponentData } from "./FormFieldComponentData";

/**
 * Service for managing all registered field types.
 */
@Injectable({
    providedIn: 'root'
})
export class FieldTypeService {

    /**
     * Map of all registered field type definitions by name.
     */
    private fieldTypes: Record<string, Type<FormFieldComponentData>> = {};

    /**
     * Registers a new field type under the given name.
     *
     * @param fieldTypeName
     *     The name which uniquely identifies the field type being registered.
     *
     * @param component
     *     The component type to associate with the given name.
     */
    registerFieldType(fieldTypeName: string, component: Type<FormFieldComponentData>): void {

        // Store field type
        this.fieldTypes[fieldTypeName] = component;

    }

    /**
     * Returns the component associated with the given name, if any.
     *
     * @param fieldTypeName
     *     The name which uniquely identifies the field type to retrieve.
     */
    getComponent(fieldTypeName: string): Type<FormFieldComponentData> | null {
        return this.fieldTypes[fieldTypeName] || null;
    }


}
