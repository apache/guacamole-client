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
import { Field } from '../../rest/types/Field';
import { Form } from '../../rest/types/Form';

import { FormService } from './form.service';

/**
 * Two forms with three fields.
 */
const FORMS: Form[] = [
    new Form({
        name: 'first',
        fields: [
            new Field({ name: 'flag', type: Field.Type.BOOLEAN, options: ['true'] }),
            new Field({ name: 'note', type: Field.Type.TEXT })
        ]
    }),
    new Form({
        name: 'second',
        fields: [new Field({ name: 'empty', type: Field.Type.TEXT })]
    })
];

describe('FormService', () => {
    let service: FormService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(FormService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getFormGroup', () => {

        it('should provide an empty control for each field of the given forms', () => {
            expect(service.getFormGroup(FORMS).getRawValue()).toEqual({ flag: '', note: '', empty: '' });
        });

        it('should provide controls only for the fields which are properties of the given model', () => {
            const formGroup = service.getFormGroup(FORMS, { flag: 'true', empty: null, other: 'x' });

            expect(Object.keys(formGroup.controls)).toEqual(['flag', 'empty']);
        });

        it('should provide no control if the given model has no properties', () => {
            expect(Object.keys(service.getFormGroup(FORMS, {}).controls)).toEqual([]);
        });

    });
});
