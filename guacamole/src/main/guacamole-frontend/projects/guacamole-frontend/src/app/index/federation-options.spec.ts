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

import { getFederationOptions } from './federation-options';

describe('getFederationOptions', () => {

    /**
     * Adds a <meta name="build"> element with the given content to the
     * document.
     *
     * @param content
     *     The build identifier to store in the meta element.
     */
    function setBuildIdentifier(content: string): void {
        const meta = document.createElement('meta');
        meta.name = 'build';
        meta.content = content;
        document.head.appendChild(meta);
    }

    afterEach(() => {
        document.head.querySelectorAll('meta[name="build"]').forEach(meta => meta.remove());
    });

    it('tags the remote entry of the application with the build identifier', () => {
        setBuildIdentifier('20261121030132');
        expect(getFederationOptions(document)).toEqual({
            hostRemoteEntry: { url: './remoteEntry.json', cacheTag: '20261121030132' }
        });
    });

    it('encodes the build identifier for use within a URL', () => {
        setBuildIdentifier('#{guacamole.build.identifier}');
        expect(getFederationOptions(document)).toEqual({
            hostRemoteEntry: { url: './remoteEntry.json', cacheTag: '%23%7Bguacamole.build.identifier%7D' }
        });
    });

    it('leaves the remote entry untagged without a build identifier', () => {
        expect(getFederationOptions(document).hostRemoteEntry).toStrictEqual({ url: './remoteEntry.json' });
    });

    it('leaves the remote entry untagged if the build identifier is empty', () => {
        setBuildIdentifier('');
        expect(getFederationOptions(document).hostRemoteEntry).toStrictEqual({ url: './remoteEntry.json' });
    });

});
