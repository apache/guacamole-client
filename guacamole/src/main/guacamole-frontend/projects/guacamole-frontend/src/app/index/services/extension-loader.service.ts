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

import { getManifest, loadRemoteModule } from '@angular-architects/module-federation';
import { Injectable, Injector } from '@angular/core';
import { Route, Router, Routes } from '@angular/router';
import { BootsrapExtensionFunction } from 'guacamole-frontend-ext-lib';
import { appRoutes, fallbackRoute, titleResolver } from '../../app-routing.module';
import { ModuleFederationManifest } from '../types/ModuleFederationManifest';

/**
 * A service which dynamically loads all extensions defined in the
 * module federation manifest, bootstraps them and adds their routes
 * to the main application.
 */
@Injectable({
    providedIn: 'root'
})
export class ExtensionLoaderService {

    /**
     * Inject required services.
     */
    constructor(private router: Router,
                private injector: Injector) {
    }

    /**
     * TODO
     */
    async loadExtensionAndSetRouterConfig(): Promise<void> {
        const manifest = getManifest<ModuleFederationManifest>();
        this.buildRoutes(manifest)
            .then(routes => this.router.resetConfig(routes));
    }

    /**
     * Iterates over all extensions defined in the module federation manifest
     * to bootstrap them and add their routes to the main application.
     *
     * @param config
     *     The module federation manifest.
     *
     * @returns
     *     The routes of the main application and all extensions.
     */
    async buildRoutes(config: ModuleFederationManifest): Promise<Routes> {
        const extensionRoutes: Routes = [];
        const keys = Object.keys(config);

        for (const key of keys) {
            const entry = config[key];
            const module = await loadRemoteModule({
                type: 'manifest',
                remoteName: key,
                exposedModule: entry.entrypoint
            });

            // Bootstrap the extension
            const bootstrapFunction: BootsrapExtensionFunction = module[entry.bootsrapFunctionName];
            const moduleRoutes = bootstrapFunction(this.injector);

            // Add the routes of the extension to the routes of the main application
            const extensionRoute: Route = {
                path: entry.routePath,
                children: moduleRoutes
            };

            // Use the title of the extension if set, otherwise fall back to the
            // title of the main application.
            if (entry.pageTitle)
                extensionRoute.title = entry.pageTitle;
            else {
                extensionRoute.data = {title: 'APP.NAME'};
                extensionRoute.title = titleResolver;
            }


            extensionRoutes.push(extensionRoute);
        }

        return [...appRoutes, ...extensionRoutes, fallbackRoute];
    }

}
