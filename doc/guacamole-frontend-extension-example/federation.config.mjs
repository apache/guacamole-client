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

import { withNativeFederation, shareAll } from '@angular-architects/native-federation/config';
import extLib from 'guacamole-frontend-ext-lib/package.json' with { type: 'json' };

export default withNativeFederation({

  // Must equal the namespace in the extension manifest, as Guacamole lists
  // each extension under its namespace in native-federation-manifest.json
  name: 'guacamole-angular-example',

  exposes: {
    'bootstrapExtension': './src/app/extension.config.ts',
    'AboutExtensionButtonComponent': './src/app/components/about-extension-button.component.ts'
  },

  shared: {
    ...shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto' }),

    // 'auto' would take the file: path from package.json as version range;
    // require the installed version instead, as the host does
    'guacamole-frontend-ext-lib': { singleton: true, strictVersion: true, requiredVersion: `~${extLib.version}` }
  },

  // Packages that should not be shared or are not needed at runtime
  skip: [
    'rxjs/ajax',
    'rxjs/fetch',
    'rxjs/testing',
    'rxjs/webSocket'
  ]

  // Please read our FAQ about sharing libs:
  // https://shorturl.at/jmzH0
  
});
