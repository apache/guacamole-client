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

import { Component, Input } from '@angular/core';
import { Field } from '../../../rest/types/Field';
import { FormControl } from '@angular/forms';


/**
 * Produces the translation string for the given field option
 * value. The translation string will be of the form:
 *
 * <code>NAMESPACE.FIELD_OPTION_NAME_VALUE<code>
 *
 * where <code>NAMESPACE</code> is the provided namespace,
 * <code>NAME</code> is the field name transformed
 * via canonicalize(), and
 * <code>VALUE</code> is the option value transformed via
 * canonicalize()
 *
 * @param field
 *    The field to which the option belongs.
 *
 * @param namespace
 *    The namespace to use when generating the translation string.
 *
 * @param value
 *     The name of the option value.
 *
 * @returns
 *     The translation string which produces the translated name of the
 *     value specified.
 */
export const getFieldOption = (field: Field | undefined, namespace?: string, value?: string): string => {

    // If no field, or no value, then no corresponding translation string
    if (!field || !field.name)
        return '';

    return canonicalize(namespace || 'MISSING_NAMESPACE')
        + '.FIELD_OPTION_' + canonicalize(field.name)
        + '_' + canonicalize(value || 'EMPTY');

}

/**
 * TODO: Move to separate file as this is used in multiple places
 * Given an arbitrary identifier, returns the corresponding translation
 * table identifier. Translation table identifiers are uppercase strings,
 * word components separated by single underscores. For example, the
 * string "Swap red/blue" would become "SWAP_RED_BLUE".
 *
 * @param identifier
 *     The identifier to transform into a translation table identifier.
 *
 * @returns
 *     The translation table identifier.
 */
export const canonicalize = (identifier: string): string => {
    return identifier.replace(/[^a-zA-Z0-9]+/g, '_').toUpperCase();
}

/**
 * Base class for form field components.
 *
 * TODO: move to shared library
 */
@Component({'template': ''})
export abstract class FormFieldBaseComponent {

    /**
     * The translation namespace of the translation strings that will
     * be generated for this field. This namespace is absolutely
     * required. If this namespace is omitted, all generated
     * translation strings will be placed within the MISSING_NAMESPACE
     * namespace, as a warning.
     */
    @Input() namespace: string | undefined;

    /**
     * The field to display.
     */
    @Input({required: true}) field!: Field;

    /**
     * The form control which contains this fields current value. When this
     * field changes, the property will be updated accordingly.
     */
    @Input() control?: FormControl<string | null>;

    /**
     * Whether this field should be rendered as disabled. By default,
     * form fields are enabled.
     */
    @Input() disabled: boolean = false;

    /**
     * Whether this field should be focused.
     */
    @Input() focused: boolean = false;

    /**
     * An ID value which is reasonably likely to be unique relative to
     * other elements on the page. This ID should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    abstract fieldId?: string;

    /**
     * Sets the disabled state of the given control.
     * @param control
     *     The control whose disabled state should be set.
     * @param disabled
     *     Whether the control should be disabled.
     */
    protected setDisabledState(control: FormControl | undefined, disabled: boolean): void {
        if (disabled) {
            control?.disable({emitEvent: false});
        } else {
            control?.enable({emitEvent: false});
        }
    }
}
