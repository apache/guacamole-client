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

import { Routes } from '@angular/router';
import { HomePageComponent } from "./components/home-page.component";

export const routes: Routes = [
    {
        path: '',
        component: HomePageComponent,
    },

    {
        path: 'about',
        loadComponent: () => import('./components/about-extension-page.component')
            .then(m => m.AboutExtensionPageComponent),
        title: 'About The Extension'
    },

    // fallback route when the url is not found
    {path: '**', redirectTo: '', pathMatch: 'full'}
];
