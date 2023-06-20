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

import { Component, Input, OnChanges, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { FormFieldBaseComponent, getFieldOption } from '../form-field-base/form-field-base.component';

/**
 * Component for text fields.
 */
@Component({
    selector: 'guac-text-field',
    templateUrl: './text-field.component.html',
    encapsulation: ViewEncapsulation.None
})
export class TextFieldComponent extends FormFieldBaseComponent implements OnChanges, OnInit {

    /**
     * The ID value that should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    @Input() fieldId?: string;

    /**
     * The ID of the datalist element that should be associated with the text
     * field, providing a set of known-good values. If no such values are
     * defined, this will be null.
     */
    dataListId: string | null = null;

    ngOnInit(): void {
        // Generate unique ID for datalist, if applicable
        if (this.field.options && this.field.options.length)
            this.dataListId = this.fieldId + '-datalist';
    }

    /**
     * Apply disabled state to form control.
     */
    ngOnChanges(changes: SimpleChanges): void {

        if (changes['disabled']) {

            const disabled: boolean = changes['disabled'].currentValue;
            this.setDisabledState(this.control, disabled);

        }
    }

    /**
     * @borrows FormFieldBaseComponent.getFieldOption
     */
    protected readonly getFieldOption = getFieldOption;
}
