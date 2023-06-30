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

import { Component, Input, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { FormService } from '../../service/form.service';
import { FormControl, FormGroup } from '@angular/forms';
import { canonicalize } from '../../../locale/service/translation.service';

/**
 * A component that allows editing of a collection of fields.
 */
@Component({
    selector: 'guac-form',
    templateUrl: './form.component.html',
    encapsulation: ViewEncapsulation.None
})
export class FormComponent implements OnChanges {

    /**
     * The translation namespace of the translation strings that will
     * be generated for all fields. This namespace is absolutely
     * required. If this namespace is omitted, all generated
     * translation strings will be placed within the MISSING_NAMESPACE
     * namespace, as a warning.
     */
    @Input() namespace?: string | undefined;

    /**
     * The form content to display. This may be a form, an array of
     * forms, or a simple array of fields.
     */
    @Input() content?: Form[] | Form | Field[] | Field;

    /**
     * A form group which will receive all field values. Each field value
     * will be assigned to a form control of this group having the same
     * name.
     */
    @Input() modelFormGroup?: FormGroup;

    /**
     * Whether the contents of the form should be restricted to those
     * fields/forms which match properties defined within the given
     * model object. By default, all fields will be shown.
     */
    @Input() modelOnly = false

    /**
     * Whether the contents of the form should be rendered as disabled.
     * By default, form fields are enabled.
     */
    @Input() disabled = false;

    /**
     * The name of the field to be focused, if any.
     */
    @Input() focusedField: string | undefined;

    /**
     * The array of all forms to display.
     */
    forms: Form[] = [];

    constructor(private formService: FormService) {
    }

    /**
     * Produces the translation string for the section header of the
     * given form. The translation string will be of the form:
     *
     * <code>NAMESPACE.SECTION_HEADER_NAME<code>
     *
     * where <code>NAMESPACE</code> is the namespace provided to the
     * directive and <code>NAME</code> is the form name transformed
     * via canonicalize().
     *
     * @param form
     *     The form for which to produce the translation string.
     *
     * @returns
     *     The translation string which produces the translated header
     *     of the form.
     */
    getSectionHeader(form: Form): string {

        // If no form, or no name, then no header
        if (!form || !form.name)
            return '';

        return canonicalize(this.namespace || 'MISSING_NAMESPACE')
            + '.SECTION_HEADER_' + canonicalize(form.name);

    }

    /**
     * Returns an object as would be provided to the ngClass directive
     * that defines the CSS classes that should be applied to the given
     * form.
     *
     * @param form
     *     The form to generate the CSS classes for.
     *
     * @return
     *     The ngClass object defining the CSS classes for the given
     *     form.
     */
    getFormClasses(form: Form): Record<string, boolean> {
        return this.formService.getClasses('form-', form);
    }

    ngOnChanges(changes: SimpleChanges): void {

        // Produce set of forms from any given content
        if (changes['content']) {

            const content: Form[] | Form | Field[] | Field = changes['content'].currentValue;

            // Transform content to always be an array of forms
            this.forms = this.formService.asFormArray(content);

        }

    }

    /**
     * Returns whether the given field should be focused or not.
     *
     * @param field
     *     The field to check.
     *
     * @returns
     *     true if the given field should be focused, false otherwise.
     */
    isFocused(field: Field): boolean {
        return field && (field.name === this.focusedField);
    }

    /**
     * Returns whether the given field should be displayed to the
     * current user.
     *
     * @param field
     *     The field to check.
     *
     * @returns
     *     true if the given field should be visible, false otherwise.
     */
    isVisible(field: Field): boolean {

        // All fields are visible if contents are not restricted to
        // model properties only
        if (!this.modelOnly)
            return true;

        // Otherwise, fields are only visible if they are present
        // within the form group
        return field && !!this.modelFormGroup && !!this.modelFormGroup.get(field.name);

    }

    /**
     * Returns whether at least one of the given fields should be
     * displayed to the current user.
     *
     * @param fields
     *     The array of fields to check.
     *
     * @returns
     *     true if at least one field within the given array should be
     *     visible, false otherwise.
     */
    containsVisible(fields: Field[]): boolean {

        // If fields are defined, check whether at least one is visible
        if (fields) {
            for (let i = 0; i < fields.length; i++) {
                if (this.isVisible(fields[i]))
                    return true;
            }
        }

        // Otherwise, there are no visible fields
        return false;

    }

    /**
     * Returns the form control that is associated to the given field.
     *
     * @param field
     *     The field to get the control for.
     *
     * @returns
     *     The form control associated to the given field. If no form
     *     group has been provided, a dummy control will be returned.
     */
    getControl(field: Field): FormControl {
        return this.modelFormGroup ? this.modelFormGroup.get(field.name) as FormControl : new FormControl('not found');
    }
}
