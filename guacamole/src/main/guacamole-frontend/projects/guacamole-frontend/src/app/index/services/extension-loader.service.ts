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

import { HttpClient } from '@angular/common/http';
import { Inject, Injectable, Injector } from '@angular/core';
import { Route, Router, Routes } from '@angular/router';
import { BootstrapExtensionFunction } from 'guacamole-frontend-ext-lib';
import _ from 'lodash';
import { lastValueFrom } from 'rxjs';
import { appRoutes, authGuard, fallbackRoute, titleResolver } from '../../app-routing.module';
import { LogService } from '../../util/log.service';
import { DEFAULT_BOOTSTRAP_FUNCTION_NAME, FederationRouteConfig } from '../types/FederationRouteConfig';
import { LOAD_REMOTE_MODULE, LoadRemoteModule } from '../types/LoadRemoteModule';

/**
 * A service which dynamically loads all extensions defined in the
 * native federation manifest, bootstraps them and adds their routes
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
                private injector: Injector,
                private http: HttpClient,
                private log: LogService,
                @Inject(LOAD_REMOTE_MODULE) private loadRemoteModule: LoadRemoteModule) {
    }

    /**
     * Loads the native federation configuration of the installed extensions,
     * bootstraps each extension and replaces the router configuration with
     * the routes of the main application and all extensions. Does nothing if
     * no extensions are installed. If an extension cannot be loaded, the
     * error is logged and the application keeps its own routes.
     *
     * This function runs as an app initializer, such that the routes of all
     * extensions are known before the first navigation.
     *
     * @returns
     *     A promise which resolves once all extensions have been bootstrapped
     *     and the routes have been set.
     */
    async loadExtensionAndSetRouterConfig(): Promise<void> {

        const routeConfig = await lastValueFrom(
            this.http.get<FederationRouteConfig>('native-federation-config.json')
        );

        // Config is empty if no extensions are installed
        if (_.isEmpty(routeConfig))
            return;

        // Keep the routes of the main application if any extension cannot be
        // loaded, such that the application still starts
        try {
            this.router.resetConfig(await this.buildRoutes(routeConfig));
        }
        catch (error) {
            this.log.error('Extensions could not be loaded:', error);
        }

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
    async buildRoutes(config: FederationRouteConfig): Promise<Routes> {
        const extensionRoutes: Routes = [];
        const keys = Object.keys(config);

        for (const key of keys) {
            const entry = config[key];
            const bootstrapFunctionName = entry.bootstrapFunctionName || DEFAULT_BOOTSTRAP_FUNCTION_NAME;
            const module = await this.loadRemoteModule<Record<string, BootstrapExtensionFunction>>(
                key, bootstrapFunctionName
            );

            // Bootstrap the extension
            const bootstrapFunction: BootstrapExtensionFunction = module[bootstrapFunctionName];
            const moduleRoutes = bootstrapFunction(this.injector);

            // Add the routes of the extension to the routes of the main
            // application, requiring a valid session like those routes
            const extensionRoute: Route = {
                path: entry.routePath,
                children: moduleRoutes,
                canActivate: [authGuard]
            };

            // Use the title of the extension if set, otherwise fall back to the
            // title of the main application.
            if (entry.pageTitle)
                extensionRoute.title = entry.pageTitle;
            else {
                extensionRoute.data = { titleKey: 'APP.NAME' };
                extensionRoute.title = titleResolver;
            }


            extensionRoutes.push(extensionRoute);
        }

        return [...appRoutes, ...extensionRoutes, fallbackRoute];
    }

}
