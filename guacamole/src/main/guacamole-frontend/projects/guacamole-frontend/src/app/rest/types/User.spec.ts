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

import { User } from './User';

describe('User', () => {

    // The JDBC authentication stores the hash of the empty password for
    // "password": "", and a random hash if the password is missing
    it('should not send a password to the REST API if none is given', () => {
        expect(JSON.parse(JSON.stringify(new User({ username: 'alice' }))))
            .toEqual({ username: 'alice', attributes: {} });
    });

});
