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

import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ComponentRef,
    ElementRef,
    Input,
    OnChanges,
    SimpleChange,
    SimpleChanges,
    ViewChild,
    ViewContainerRef,
    ViewEncapsulation,
    ChangeDetectionStrategy
} from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormFieldComponentData } from 'guacamole-frontend-ext-lib';
import { canonicalize } from '../../../locale/service/translation.service';
import { LogService } from '../../../util/log.service';
import { FormService } from '../../service/form.service';
import { FormFieldBaseComponent } from '../form-field-base/form-field-base.component';

/**
 * A component that allows editing of a field.
 */
@Component({
    selector: 'guac-form-field',
    templateUrl: './form-field.component.html',
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class FormFieldComponent extends FormFieldBaseComponent implements AfterViewInit, OnChanges {

    /**
     * Reference to the field content element.
     */
    @ViewChild('formField') fieldContentRef!: ElementRef;

    /**
     * The element which should contain any compiled field content. The
     * actual content of a field is dynamically determined by its type.
     */
    fieldContent?: Element;

    /**
     * View container reference which is used to dynamically create and insert
     * field components.
     */
    @ViewChild('formField', { read: ViewContainerRef }) viewContainer!: ViewContainerRef;

    /**
     * Reference to the dynamically created field component.
     */
    fieldComponent: ComponentRef<FormFieldComponentData & Partial<OnChanges>> | undefined;

    /**
     * All current field values in the form, keyed by field name.
     * Required to evaluate conditionalLabel conditions.
     */
    @Input() values?: FormGroup;

    /**
     * An ID value which is reasonably likely to be unique relative to
     * other elements on the page. This ID should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    fieldId: string = 'guac-field-XXXXXXXXXXXXXXXX'.replace(/X/g, function getRandomCharacter() {
        return Math.floor(Math.random() * 36).toString(36);
    }) + '-' + new Date().getTime().toString(36);


    /**
     * Inject required services.
     */
    constructor(private formService: FormService,
                private log: LogService,
                private cdr: ChangeDetectorRef) {
        super();
    }

    /**
     * Updates the field content and passes changes to the field component.
     */
    async ngOnChanges(changes: SimpleChanges): Promise<void> {

        // Update field contents when field definition is changed
        if (changes['field']) {
            await this.insertFieldElement();
        }

        // Pass changes to the specific field component if it exists and implements
        // the ngOnChanges() lifecycle method
        this.fieldComponent?.instance.ngOnChanges?.(changes);
    }

    /**
     * Insert the component for the type of the field, now that the element
     * which holds the field content exists. Until then,
     * insertFieldElement() returns without inserting anything.
     */
    async ngAfterViewInit(): Promise<void> {
        this.fieldContent = this.fieldContentRef.nativeElement;

        // Initially insert field element
        await this.insertFieldElement();
    }

    /**
     * Returns the translation key for this field's label, of the form
     * NAMESPACE.FIELD_HEADER_NAME. If the field defines a
     * conditionalLabel and its condition is met, returns the alternate
     * key instead.
     *
     * @returns
     *     The translation key for this field's label.
     */
    getFieldHeader(): string {

        // If no field, or no name, then no header
        if (!this.field || !this.field.name)
            return '';

        const ns = canonicalize(this.namespace || 'MISSING_NAMESPACE');

        const conditionalLabel = this.field.conditionalLabel;
        if (conditionalLabel && conditionalLabel.labelKey && this.values) {

            let depValue: unknown = this.values.get(conditionalLabel.field)?.value;
            if (depValue == null)
                depValue = '';

            // equals and notEquals are mutually exclusive; equals wins if both are set
            const hasEquals    = 'equals'    in conditionalLabel;
            const hasNotEquals = 'notEquals' in conditionalLabel;
            let conditionMet: boolean;
            if (hasEquals)
                conditionMet = depValue === conditionalLabel.equals;
            else if (hasNotEquals)
                conditionMet = depValue !== conditionalLabel.notEquals;
            else {
                this.log.warn('conditionalLabel for field "' + this.field.name
                    + '" specifies neither "equals" nor "notEquals"; ignoring.');
                conditionMet = false;
            }

            if (conditionMet)
                return ns + '.' + conditionalLabel.labelKey;
        }

        return ns + '.FIELD_HEADER_' + canonicalize(this.field.name);

    }

    /**
     * Returns an object as would be provided to the ngClass directive
     * that defines the CSS classes that should be applied to this
     * field.
     *
     * @return
     *     The ngClass object defining the CSS classes for the current
     *     field.
     */
    getFieldClasses(): Record<string, boolean> {
        return this.formService.getClasses('labeled-field-', this.field, {
            empty: !this.control?.value
        });
    }

    /**
     * Returns whether the current field should be displayed.
     *
     * @returns
     *     true if the current field should be displayed, false
     *     otherwise.
     */
    isFieldVisible(): boolean {
        return this.fieldContent?.hasChildNodes() || false;
    }

    /**
     * Inserts the element corresponding to the current field into the DOM.
     */
    async insertFieldElement(): Promise<void> {

        if (!this.fieldContent) {
            return;
        }

        // Reset contents
        this.fieldContent.innerHTML = '';

        // Append field content
        if (this.field) {

            await this.formService.insertFieldElement(this.fieldContent, this.field.type, this,
                this.viewContainer, this.cdr)
                .then((fieldElement) => {

                    // Store reference to the created field component
                    this.fieldComponent = fieldElement;

                    // The component copied disabled, but ngOnChanges() is not
                    // called for the initial value: apply it to the control,
                    // which may still be disabled by a previous field
                    fieldElement.instance.ngOnChanges?.({
                        disabled: new SimpleChange(undefined, this.disabled, true)
                    });

                })
                .catch(() => {
                    // fieldCreationFailed
                    this.log.warn('Failed to retrieve field with type "' + this.field.type + '"');
                });
        }
    }
}
