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

import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AboutExtensionButtonComponent } from "./about-extension-button.component";
import { FormControl } from "@angular/forms";
import {
    FormField
} from "guacamole-frontend-ext-lib/lib/form/FormField";
import { RouterLink } from "@angular/router";
import { PlanetChooserFieldComponent } from "./planet-chooser-field.component";

/**
 * The home page of the extension.
 */
@Component({
    selector: 'guac-home-page',
    imports: [CommonModule, AboutExtensionButtonComponent, PlanetChooserFieldComponent, RouterLink, PlanetChooserFieldComponent],
    template: `
        <p>
            guacamole-frontend-extension-example works!
        </p> 
        <h2>Planet Chooser Field</h2>
        <p>
            <app-planet-chooser-field
                    [disabled]="false"
                    [control]="control"
                    namespace="extension-example"
                    [focused]="false"
                    [field]="field"
            />
            Your selected planet is: {{control.value}}
        </p>
        <a routerLink="/">Home</a>
        <guac-about-extension-button/>
    `,
    encapsulation: ViewEncapsulation.None
})
export class HomePageComponent {
    control = new FormControl<string | null>('');
    field = {} as FormField;
}
