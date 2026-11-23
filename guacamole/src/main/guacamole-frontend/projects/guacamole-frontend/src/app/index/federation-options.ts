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

import type { NgNFOptions } from '@angular-architects/native-federation';

/**
 * Returns the options for initializing Native Federation. The URL of the
 * remoteEntry.json of the application carries the build identifier from
 * index.html, so that a browser does not reuse a cached copy from a previous
 * build after Guacamole has been updated. The remoteEntry.json lists the
 * shared bundles of the application, and only its file name carries no hash.
 *
 * @param document
 *     The document whose <meta name="build"> element contains the build
 *     identifier.
 *
 * @returns
 *     The options to pass to initFederation().
 */
export function getFederationOptions(document: Document): NgNFOptions {

    const buildMeta = document.head.querySelector<HTMLMetaElement>('meta[name="build"]');

    // Append ?cacheTag=<build> only when the build identifier is known
    const hostRemoteEntry: { url: string, cacheTag?: string } = { url: './remoteEntry.json' };
    if (buildMeta?.content)
        hostRemoteEntry.cacheTag = encodeURIComponent(buildMeta.content);

    return { hostRemoteEntry };

}
