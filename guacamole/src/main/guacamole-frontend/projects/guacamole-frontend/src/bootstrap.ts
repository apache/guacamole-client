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

import { ViewEncapsulation, provideZoneChangeDetection } from '@angular/core';
import { platformBrowser } from '@angular/platform-browser';

import { AppModule } from './app/app.module';
import { LOAD_REMOTE_MODULE, LoadRemoteModule } from './app/index/types/LoadRemoteModule';

/**
 * Bootstraps the application once Native Federation has been initialized.
 *
 * @param loadRemoteModule
 *     The function which loads modules exposed by extensions. The application
 *     receives it through the LOAD_REMOTE_MODULE injection token.
 */
export function bootstrap(loadRemoteModule: LoadRemoteModule): void {
    platformBrowser().bootstrapModule(AppModule, {
        applicationProviders: [
            provideZoneChangeDetection(),
            { provide: LOAD_REMOTE_MODULE, useValue: loadRemoteModule }
        ],
        defaultEncapsulation: ViewEncapsulation.None
    })
        .catch(err => console.error(err));
}
