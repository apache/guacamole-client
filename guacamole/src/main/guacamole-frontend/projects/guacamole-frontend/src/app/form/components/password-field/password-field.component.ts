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
import { FormFieldBaseComponent } from '../form-field-base/form-field-base.component';

/**
 * Component to display password fields.
 */
@Component({
    selector: 'guac-password-field',
    templateUrl: './password-field.component.html',
    encapsulation: ViewEncapsulation.None
})
export class PasswordFieldComponent extends FormFieldBaseComponent implements OnChanges {

    /**
     * The ID value that should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    @Input() fieldId?: string;

    /**
     * The type to use for the input field. By default, the input field will
     * have the type 'password', and thus will be masked.
     *
     * @default 'password'
     */
    passwordInputType: string = 'password';

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
     * Returns a string which describes the action the next call to
     * togglePassword() will have.
     *
     * @return
     *     A string which describes the action the next call to
     *     togglePassword() will have.
     */
    getTogglePasswordHelpText(): string {

        // If password is hidden, togglePassword() will show the password
        if (this.passwordInputType === 'password')
            return 'FORM.HELP_SHOW_PASSWORD';

        // If password is shown, togglePassword() will hide the password
        return 'FORM.HELP_HIDE_PASSWORD';

    }

    /**
     * Toggles visibility of the field contents, if this field is a
     * password field. Initially, password contents are masked
     * (invisible).
     */
    togglePassword() {

        // If password is hidden, show the password
        if (this.passwordInputType === 'password')
            this.passwordInputType = 'text';

        // If password is shown, hide the password
        else
            this.passwordInputType = 'password';

    };

}
