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

import { FormControl } from '@angular/forms';
import { FormField } from './FormField';

/**
 * An interface which defines the basic properties of a form field component.
 * Used by the form service to generate form field components.
 */
export interface FormFieldComponentData {

    /**
     * The translation namespace of the translation strings that will
     * be generated for this field. This namespace is absolutely
     * required. If this namespace is omitted, all generated
     * translation strings will be placed within the MISSING_NAMESPACE
     * namespace, as a warning.
     */
    namespace: string | undefined;

    /**
     * The field to display.
     */
    field: FormField;

    /**
     * The form control which contains this fields current value. When this
     * field changes, the property will be updated accordingly.
     */
    control?: FormControl<string | null>;

    /**
     * Whether this field should be rendered as disabled. By default,
     * form fields are enabled.
     */
    disabled: boolean;

    /**
     * Whether this field should be focused.
     */
    focused: boolean;

    /**
     * An ID value which is reasonably likely to be unique relative to
     * other elements on the page. This ID should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    fieldId?: string;

    /**
     * The managed client associated with this form field, if any.
     */
    client?: any;
}
