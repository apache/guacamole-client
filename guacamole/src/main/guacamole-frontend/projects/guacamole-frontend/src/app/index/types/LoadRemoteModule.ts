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

import type { NativeFederationResult } from '@angular-architects/native-federation';
import { InjectionToken } from '@angular/core';

/**
 * Loads the module exposed under the given name by the given remote, as
 * returned by initFederation() of Native Federation.
 */
export type LoadRemoteModule = NativeFederationResult['loadRemoteModule'];

/**
 * Injection token for the function which loads modules exposed by the
 * extensions listed in the Native Federation manifest. If the extensions
 * could not be initialized, the application starts without them, and the
 * function rejects every call.
 */
export const LOAD_REMOTE_MODULE = new InjectionToken<LoadRemoteModule>('LOAD_REMOTE_MODULE');
