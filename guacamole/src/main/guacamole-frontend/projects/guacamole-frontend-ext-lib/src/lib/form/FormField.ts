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

/**
 * An interface for the object returned by REST API calls when representing the data
 * associated with a field or configuration parameter.
 */
export interface FormField {

    /**
     * The name which uniquely identifies this parameter.
     */
    name: string;

    /**
     * The type string defining which values this parameter may contain,
     * as well as what properties are applicable.
     */
    type: string;

    /**
     * All possible legal values for this parameter.
     */
    options?: string[];

    /**
     * A message which can be translated using the translation service,
     * consisting of a translation key and optional set of substitution
     * variables.
     */
    translatableMessage?: unknown;

    /**
     * The URL to which the user should be redirected when a field of type
     * REDIRECT is displayed.
     */
    redirectUrl?: string;
}
