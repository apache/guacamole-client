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
import { GuacImageService } from './guac-image.service';

/**
 * Test double for the Image constructor, which decodes every image as a 1x1
 * image. The DOM environment of the tests does not decode images and never
 * fires their load or error events.
 */
class DecodingImage {

    /**
     * The width of the decoded image, in pixels.
     */
    width = 0;

    /**
     * The height of the decoded image, in pixels.
     */
    height = 0;

    /**
     * Invoked once the image has been decoded.
     */
    onload: (() => void) | null = null;

    /**
     * Invoked if the image cannot be decoded. Never invoked by this double.
     */
    onerror: (() => void) | null = null;

    /**
     * Decodes the image asynchronously, like a browser, once its source is set.
     */
    set src(value: string) {
        setTimeout(() => {
            this.width = 1;
            this.height = 1;
            this.onload?.();
        });
    }

}

describe('GuacImageService', () => {
    let service: GuacImageService;

    beforeEach(() => {
        vi.stubGlobal('Image', DecodingImage);
        TestBed.configureTestingModule({});
        service = TestBed.inject(GuacImageService);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should return that all image types are supported', async () => {
        const supported = await service.getSupportedMimetypes();
        expect(supported).toEqual(['image/jpeg', 'image/png', 'image/webp']);
    });
});
