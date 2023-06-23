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

import { TestBed } from '@angular/core/testing';
import { GuacImageService } from "./guac-image.service";

describe('GuacImageService', () => {
    let service: GuacImageService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(GuacImageService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should return that all image types are supported', async () => {
        const supported = await service.getSupportedMimetypes();
        expect(supported).toEqual(['image/jpeg', 'image/png', 'image/webp']);
    });
});
