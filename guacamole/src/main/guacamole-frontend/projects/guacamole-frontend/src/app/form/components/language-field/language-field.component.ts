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
import { LanguageService } from '../../../rest/service/language.service';
import { FormFieldBaseComponent } from '../form-field-base/form-field-base.component';

/**
 * Component to display language fields. The language field type allows the
 * user to select a language from the set of languages supported by the
 * Guacamole web application.
 */
@Component({
    selector: 'guac-language-field',
    templateUrl: './language-field.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class LanguageFieldComponent extends FormFieldBaseComponent implements OnInit, OnChanges {

    /**
     * The ID value that should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    @Input() fieldId?: string;

    /**
     * A map of all available language keys to their human-readable
     * names.
     */
    languages?: Record<string, string> = undefined;

    constructor(private languageService: LanguageService) {
        super();
    }

    ngOnInit(): void {

        // Retrieve defined languages
        this.languageService.getLanguages().subscribe(languages => {
            this.languages = languages;
        });

        // Interpret undefined/null as empty string
        this.control?.valueChanges.subscribe(value => {
            if (!value && value !== '')
                this.control?.setValue('', { emitEvent: false });
        });
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

}
