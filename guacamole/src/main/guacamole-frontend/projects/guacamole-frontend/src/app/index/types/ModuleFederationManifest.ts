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

import { Manifest, RemoteConfig } from '@angular-architects/module-federation';

/**
 * TODO
 */
export const DEFAULT_BOOTSTRAP_FUNCTION_NAME = 'bootsrapExtension';

/**
 * TODO
 */
export interface ModuleFederationManifestConfig extends RemoteConfig {

    /**
     * The name of the exposed module which contains the bootstrap function.
     */
    entrypoint: string;

    /**
     * The name of the function that will be called by the frontend to bootstrap the extension.
     *
     * @default DEFAULT_BOOTSTRAP_FUNCTION_NAME
     */
    bootsrapFunctionName: string;

    /**
     * The title of the page to be displayed in the browser tab when the route is active.
     * If not set, the title of the main application will be used. Specific routes can
     * override this value by setting the title property on the route.
     */
    pageTitle?: string;

    /**
     * The path where the extension should be mounted.
     */
    routePath: string;
}

/**
 * TODO
 */
export type ModuleFederationManifest = Manifest<ModuleFederationManifestConfig>;
