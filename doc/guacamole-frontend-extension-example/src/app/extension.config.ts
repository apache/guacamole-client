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
    BootsrapExtensionFunction,
    FieldTypeService
} from "../../../../guacamole/src/main/guacamole-frontend/dist/guacamole-frontend-ext-lib";
import { inject, Injector, runInInjectionContext } from "@angular/core";
import { Routes } from "@angular/router";

import { routes } from "./app.routes";
import { PLANET_CHOOSER_FIELD_TYPE, PlanetChooserFieldComponent } from "./components/planet-chooser-field.component";


// noinspection JSUnusedGlobalSymbols
/**
 * Called by the frontend to initialize the extension.
 */
export const bootsrapExtension: BootsrapExtensionFunction = (injector: Injector): Routes => {

    runInInjectionContext(injector, () => {

        const fieldTypeService = inject(FieldTypeService);

        // Register a new field type
        fieldTypeService.registerFieldType(PLANET_CHOOSER_FIELD_TYPE, PlanetChooserFieldComponent);

    });

    // Return the routes of the extension which will be added to the frontend routes
    return routes;
}