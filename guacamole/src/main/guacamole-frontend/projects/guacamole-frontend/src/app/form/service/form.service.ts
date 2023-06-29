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
    ChangeDetectorRef,
    ComponentRef,
    Injectable,
    Renderer2,
    RendererFactory2,
    Type,
    ViewContainerRef
} from '@angular/core';
import { Form } from '../../rest/types/Form';
import { Field } from '../../rest/types/Field';
import { FormFieldComponent } from '../components/form-field/form-field.component';
import { TimeZoneFieldComponent } from '../components/time-zone-field/time-zone-field.component';
import { CheckboxFieldComponent } from '../components/checkbox-field/checkbox-field.component';
import { DateFieldComponent } from '../components/date-field/date-field.component';
import { LanguageFieldComponent } from '../components/language-field/language-field.component';
import { NumberFieldComponent } from '../components/number-field/number-field.component';
import { PasswordFieldComponent } from '../components/password-field/password-field.component';
import { RedirectFieldComponent } from '../components/redirect-field/redirect-field.component';
import { SelectFieldComponent } from '../components/select-field/select-field.component';
import { TextFieldComponent } from '../components/text-field/text-field.component';
import { TextAreaFieldComponent } from '../components/text-area-field/text-area-field.component';
import { TimeFieldComponent } from '../components/time-field/time-field.component';
import { EmailFieldComponent } from '../components/email-field/email-field.component';
import { UsernameFieldComponent } from '../components/username-field/username-field.component';
import {
    TerminalColorSchemeFieldComponent
} from '../components/terminal-color-scheme-field/terminal-color-scheme-field.component';
import { FormControl, FormGroup } from '@angular/forms';
import { isArray } from '../../util/is-array';
import { FieldTypeService, FormFieldComponentData } from 'guacamole-frontend-ext-lib';

/**
 * A service for maintaining form-related metadata and linking that data to
 * corresponding controllers and templates.
 */
@Injectable({
    providedIn: 'root'
})
export class FormService {

    /**
     * Reference to the view container that should be used to create the field component.
     */
    private viewContainer: ViewContainerRef | undefined;

    private cdr: ChangeDetectorRef | undefined;

    private renderer: Renderer2;

    constructor(private fieldTypeService: FieldTypeService, rendererFactory: RendererFactory2) {
        this.registerFieldTypes();
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    /**
     * Registers the default field types with this field type service.
     */
    private registerFieldTypes() {

        /**
         * Text field type.
         */
        this.fieldTypeService.registerFieldType(Field.Type.TEXT, TextFieldComponent);

        /**
         * Email address field type.
         */
        this.fieldTypeService.registerFieldType(Field.Type.EMAIL, EmailFieldComponent);

        /**
         * Numeric field type.
         */
        this.fieldTypeService.registerFieldType(Field.Type.NUMERIC, NumberFieldComponent);

        /**
         * Boolean field type.
         */
        this.fieldTypeService.registerFieldType(Field.Type.BOOLEAN, CheckboxFieldComponent);

        /**
         * Username field type. Identical in principle to a text field, but may
         * have different semantics.
         */
        this.fieldTypeService.registerFieldType(Field.Type.USERNAME, UsernameFieldComponent);

        /**
         * Password field type. Similar to a text field, but the contents of
         * the field are masked.
         */
        this.fieldTypeService.registerFieldType(Field.Type.PASSWORD, PasswordFieldComponent);

        /**
         * Enumerated field type. The user is presented a finite list of values
         * to choose from.
         */
        this.fieldTypeService.registerFieldType(Field.Type.ENUM, SelectFieldComponent);

        /**
         * Multiline field type. The user may enter multiple lines of text.
         */
        this.fieldTypeService.registerFieldType(Field.Type.MULTILINE, TextAreaFieldComponent);

        /**
         * Field type which allows selection of languages. The languages
         * displayed are the set of languages supported by the Guacamole web
         * application. Legal values are valid language IDs, as dictated by
         * the filenames of Guacamole's available translations.
         */
        this.fieldTypeService.registerFieldType(Field.Type.LANGUAGE, LanguageFieldComponent);

        /**
         * Field type which allows selection of time zones.
         */
        this.fieldTypeService.registerFieldType(Field.Type.TIMEZONE, TimeZoneFieldComponent);

        /**
         * Field type which allows selection of individual dates.
         */
        this.fieldTypeService.registerFieldType(Field.Type.DATE, DateFieldComponent);

        /**
         * Field type which allows selection of times of day.
         */
        this.fieldTypeService.registerFieldType(Field.Type.TIME, TimeFieldComponent);

        /**
         * Field type which allows selection of color schemes accepted by the
         * Guacamole server terminal emulator and protocols which leverage it.
         */
        this.fieldTypeService.registerFieldType(Field.Type.TERMINAL_COLOR_SCHEME, TerminalColorSchemeFieldComponent);

        /**
         * Field type that supports redirecting the client browser to another
         * URL.
         */
        this.fieldTypeService.registerFieldType(Field.Type.REDIRECT, RedirectFieldComponent);
    }

    /**
     * Given form content and an arbitrary prefix, returns a corresponding
     * CSS class object as would be provided to the ngClass directive that
     * assigns a content-specific CSS class based on the prefix and
     * form/field name. Generated class names follow the lowercase with
     * dashes naming convention. For example, if the prefix is "field-" and
     * the provided content is a field with the name "Swap red/blue", the
     * object { 'field-swap-red-blue' : true } would be returned.
     *
     * @param prefix
     *     The arbitrary prefix to prepend to the name of the generated CSS
     *     class.
     *
     * @param content
     *     The form or field whose name should be used to produce the CSS
     *     class name.
     *
     * @param object
     *     The optional base ngClass object that should be used to provide
     *     additional name/value pairs within the returned object.
     *
     * @return
     *     The ngClass object based on the provided object and defining a
     *     CSS class name for the given content.
     */
    getClasses(prefix: string, content?: Form | Field, object: Record<string, boolean> = {}): Record<string, boolean> {

        // Perform no transformation if there is no content or
        // corresponding name
        if (!content || !content.name)
            return object;

        // Transform content name and prefix into lowercase-with-dashes
        // CSS class name
        const className = prefix + content.name.replace(/[^a-zA-Z0-9]+/g, '-').toLowerCase();

        // Add CSS class name to provided base object (without touching
        // base object)
        const classes = {...object};
        classes[className] = true;
        return classes;

    }

    /**
     * Creates a component for the field associated with the given name to the given
     * scope, producing a distinct and independent DOM Element which functions
     * as an instance of that field. The scope object provided must include at
     * least the following properties since they are copied to the created
     * component:
     *
     * namespace:
     *     A String which defines the unique namespace associated the
     *     translation strings used by the form using a field of this type.
     *
     * fieldId:
     *     A String value which is reasonably likely to be unique and may
     *     be used to associate the main element of the field with its
     *     label.
     *
     * field:
     *     The Field object that is being rendered, representing a field of
     *     this type.
     *
     * model:
     *     The current String value of the field, if any.
     *
     * disabled:
     *     A boolean value which is true if the field should be disabled.
     *     If false or undefined, the field should be enabled.
     *
     * focused:
     *     A boolean value which is true if the field should be focused.
     *
     *
     * @param fieldContainer
     *     The DOM Element to which the field component should be added as a child.
     *
     * @param fieldType
     *     The name of the field type defining the nature of the element to be
     *     created.
     *
     * @param scope
     *     The scope from which the properties of the field will be copied.
     *
     * @return
     *     A Promise which resolves to the compiled Element. If an error occurs
     *     while retrieving the field type, this Promise will be rejected.
     */
    insertFieldElement(fieldContainer: Element, fieldType: string, scope: FormFieldComponent): Promise<ComponentRef<any>> {

        // TODO: Add some comments

        return new Promise((resolve, reject) => {

            // Ensure field type is defined
            const componentToInject: Type<FormFieldComponentData> | null = this.fieldTypeService.getComponent(fieldType);
            if (!(componentToInject && this.viewContainer)) {
                reject();
                return;
            }

            const componentRef = this.insertFieldElementInternal(componentToInject, fieldContainer, scope);

            if (!componentRef) {
                reject();
                return;
            }

            this.cdr?.detectChanges();
            resolve(componentRef);

        });
    }

    /**
     * TODO: Document/rename
     * @param componentType
     * @param fieldContainer
     * @param scope
     * @private
     */
    private insertFieldElementInternal(componentType: Type<FormFieldComponentData>, fieldContainer: Element, scope: FormFieldComponent): ComponentRef<any> | null {

        if (!this.viewContainer) {
            return null;
        }

        // Clear the container first
        this.viewContainer.clear();

        // Create a component using the factory and add it to the container
        const componentRef = this.viewContainer.createComponent(componentType);

        // Copy properties from scope to the component instance
        componentRef.instance.namespace = scope.namespace;
        componentRef.instance.field = scope.field;
        componentRef.instance.control = scope.control;
        componentRef.instance.disabled = scope.disabled;
        componentRef.instance.focused = scope.focused;
        componentRef.instance.fieldId = scope.fieldId;

        // Get the native element of the created component
        const nativeElement = componentRef.location.nativeElement;

        // Insert the created component after the target element
        this.renderer.appendChild(fieldContainer, nativeElement)

        return componentRef;
    }

    setViewContainer(viewContainer: ViewContainerRef) {
        this.viewContainer = viewContainer;
    }

    setChangeDetectorRef(cdr: ChangeDetectorRef) {
        this.cdr = cdr;
    }

    /**
     * Creates a FormGroup object based on the given forms.
     * The FormGroup object will contain a FormControl for each field in each form.
     * The name of each FormControl will be the name of the corresponding field.
     *
     * @param forms
     *    The forms to be included in the form group.
     */
    getFormGroup(forms: Form[]): FormGroup {

        const formGroup = new FormGroup({});

        for (let i = 0; i < forms.length; i++) {
            const form = forms[i];

            for (const field of form.fields) {
                formGroup.addControl(field.name, new FormControl(''));
            }

        }

        return formGroup;

    }

    /**
     * Produce set of forms from any given content.
     *
     * @param content
     *     The content to be converted to an array of forms.
     */
    asFormArray(content?: Form[] | Form | Field[] | Field | null): Form[] {

        // If no content provided, there are no forms
        if (!content) {
            return [];
        }

        // Ensure content is an array
        if (!isArray(content))
            content = [content] as Form[] | Field[];

        // If content is an array of fields, convert to an array of forms
        if (this.isFieldArray(content)) {
            content = [{
                fields: content
            }];
        }

        // Content is now an array of forms
        return content;

    }

    /**
     * Determines whether the given object is an array of fields.
     *
     * @param obj
     *     The object to test.
     *
     * @returns
     *     true if the given object appears to be an array of
     *     fields, false otherwise.
     */
    isFieldArray(obj: Form[] | Field[]): obj is Field[] {
        return !!obj.length && !this.isForm(obj[0]);
    }

    /**
     * Determines whether the given object is a form, under the
     * assumption that the object is either a form or a field.
     *
     * @param obj
     *     The object to test.
     *
     * @returns
     *     true if the given object appears to be a form, false
     *     otherwise.
     */
    isForm(obj: Form | Field): obj is Form {
        return 'name' in obj && 'fields' in obj;
    }


}
