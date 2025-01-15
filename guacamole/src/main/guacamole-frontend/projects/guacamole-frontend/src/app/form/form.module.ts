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

import { CommonModule } from '@angular/common';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslocoModule } from '@ngneat/transloco';
import { ElementModule } from 'guacamole-frontend-lib';
import { OrderByPipe } from '../util/order-by.pipe';
import { CheckboxFieldComponent } from './components/checkbox-field/checkbox-field.component';
import { DateFieldComponent } from './components/date-field/date-field.component';
import { EmailFieldComponent } from './components/email-field/email-field.component';
import { FormFieldComponent } from './components/form-field/form-field.component';
import { FormComponent } from './components/form/form.component';
import { GuacInputColorComponent } from './components/guac-input-color/guac-input-color.component';
import { LanguageFieldComponent } from './components/language-field/language-field.component';
import { NumberFieldComponent } from './components/number-field/number-field.component';
import { PasswordFieldComponent } from './components/password-field/password-field.component';
import { RedirectFieldComponent } from './components/redirect-field/redirect-field.component';
import { SelectFieldComponent } from './components/select-field/select-field.component';
import {
    TerminalColorSchemeFieldComponent
} from './components/terminal-color-scheme-field/terminal-color-scheme-field.component';
import { TextAreaFieldComponent } from './components/text-area-field/text-area-field.component';
import { TextFieldComponent } from './components/text-field/text-field.component';
import { TimeFieldComponent } from './components/time-field/time-field.component';
import { TimeZoneFieldComponent } from './components/time-zone-field/time-zone-field.component';
import { UsernameFieldComponent } from './components/username-field/username-field.component';
import { GuacLenientDateDirective } from './directives/guac-lenient-date.directive';
import { GuacLenientTimeDirective } from './directives/guac-lenient-time.directive';

/**
 * Module for displaying dynamic forms.
 */
@NgModule({
    declarations: [
        GuacLenientDateDirective,
        GuacLenientTimeDirective,
        TimeZoneFieldComponent,
        FormFieldComponent,
        FormComponent,
        CheckboxFieldComponent,
        DateFieldComponent,
        LanguageFieldComponent,
        NumberFieldComponent,
        PasswordFieldComponent,
        RedirectFieldComponent,
        SelectFieldComponent,
        TextFieldComponent,
        TextAreaFieldComponent,
        TimeFieldComponent,
        EmailFieldComponent,
        UsernameFieldComponent,
        TerminalColorSchemeFieldComponent,
        GuacInputColorComponent
    ],
    exports: [
        TimeZoneFieldComponent,
        FormFieldComponent,
        FormComponent,
        CheckboxFieldComponent,
        DateFieldComponent,
        GuacLenientDateDirective,
        LanguageFieldComponent,
        NumberFieldComponent,
        PasswordFieldComponent,
        RedirectFieldComponent,
        SelectFieldComponent,
        TextFieldComponent,
        TextAreaFieldComponent,
        TimeFieldComponent,
        EmailFieldComponent,
        UsernameFieldComponent,
        TerminalColorSchemeFieldComponent,
        GuacInputColorComponent
    ],
    imports: [
        CommonModule,
        ElementModule,
        ReactiveFormsModule,
        TranslocoModule,
        OrderByPipe
    ]
})
export class FormModule {
}
