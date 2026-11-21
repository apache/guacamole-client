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

import { Component, Input, ViewEncapsulation, ChangeDetectionStrategy } from '@angular/core';
import {
    FormField,
    FormFieldComponentData
} from "guacamole-frontend-ext-lib";
import { FormControl, ReactiveFormsModule } from "@angular/forms";

export const PLANET_CHOOSER_FIELD_TYPE = 'planet-chooser';

@Component({
    selector: 'app-planet-chooser-field',
    imports: [ReactiveFormsModule],
    template: `
        @if (control) {
            <select [formControl]="control">
                @for (planet of planets; track planet) {
                    <option [value]="planet">{{planet}}</option>
                }
            </select>
        }
    `,
    changeDetection: ChangeDetectionStrategy.Eager,
    encapsulation: ViewEncapsulation.None
})
export class PlanetChooserFieldComponent implements FormFieldComponentData {

    @Input({required: true}) disabled!: boolean;
    @Input({required: true}) field!: FormField;
    @Input({required: true}) focused!: boolean;
    @Input({required: true}) namespace!: string | undefined;
    @Input() control?: FormControl<string | null>;

    readonly planets = [
        'Mercury',
        'Venus',
        'Earth',
        'Mars',
        'Jupiter',
        'Saturn',
        'Uranus',
        'Neptune'
    ];

}
