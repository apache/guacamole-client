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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { ListModule } from '../../../list/list.module';
import { Connection } from '../../../rest/types/Connection';
import { DirectoryPatch } from '../../../rest/types/DirectoryPatch';
import { DirectoryPatchOutcome } from '../../../rest/types/DirectoryPatchOutcome';
import { Error } from '../../../rest/types/Error';
import { ImportConnection } from '../../types/ImportConnection';
import { ParseError } from '../../types/ParseError';
import { ParseResult } from '../../types/ParseResult';
import { ConnectionImportErrorsComponent } from './connection-import-errors.component';

describe('ConnectionImportErrorsComponent', () => {

    let fixture: ComponentFixture<ConnectionImportErrorsComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [ConnectionImportErrorsComponent],
            imports: [
                ListModule,
                TranslocoTestingModule.forRoot({
                    langs: {
                        en: {
                            IMPORT: {
                                ERROR_INVALID_GROUP: 'No group matching "{{ GROUP }}" found',
                                ERROR_REMOVE: 'Cannot remove {{ NAME }}',
                                ERROR_ADD: 'Cannot add {{ NAME }}'
                            }
                        }
                    },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ]
        });
        fixture = TestBed.createComponent(ConnectionImportErrorsComponent);
    });

    /**
     * Runs change detection until all translations are applied and returns
     * the content of each row of the error table: the text of the cells for
     * row number, name, group and protocol, followed by each error message.
     *
     * @returns
     *     The trimmed texts of each row, one array per row.
     */
    const getRows = async (): Promise<string[][]> => {
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
        const text = (element: Element) => element.textContent!.trim();
        return Array.from(fixture.nativeElement.querySelectorAll('tbody tr') as NodeListOf<HTMLElement>)
            .map(row => [
                ...Array.from(row.querySelectorAll('td:not(.error-message)')).map(text),
                ...Array.from(row.querySelectorAll('td.error-message li')).map(text)
            ]);
    };

    it('should display errors that are added to the same parse result later', async () => {
        const connection = new ImportConnection({
            name: 'conn', protocol: 'ssh', group: 'ROOT/A/ROOT/B',
            parentIdentifier: undefined!, identifier: undefined!
        });
        const parseResult = new ParseResult({ connectionObjects: [connection], connectionCount: 1 });

        fixture.componentRef.setInput('parseResult', parseResult);
        expect(await getRows()).toEqual([]);

        // Like the import component after creating the groups of the file
        connection.errors.push(new ParseError({
            message: 'No group matching: ROOT/A/ROOT/B',
            key: 'IMPORT.ERROR_INVALID_GROUP',
            variables: { GROUP: 'ROOT/A/ROOT/B' }
        }));
        parseResult.errors[0] = connection.errors;
        parseResult.hasErrors = true;

        expect(await getRows()).toEqual([
            ['1', 'conn', 'ROOT/A/ROOT/B', 'ssh', 'No group matching "ROOT/A/ROOT/B" found']
        ]);
    });

    it('should display the API errors of each connection of the sent patches', async () => {
        const parseResult = new ParseResult({
            connectionCount: 2,
            groupPaths: { 1: 'ROOT/Existing', 2: 'ROOT' }
        });
        const patches = [
            new DirectoryPatch<Connection>({ op: DirectoryPatch.Operation.REMOVE, path: '/10' }),
            new DirectoryPatch<Connection>({
                op: DirectoryPatch.Operation.ADD, path: '/',
                value: new Connection({ name: 'replaced', protocol: 'ssh' })
            }),
            new DirectoryPatch<Connection>({
                op: DirectoryPatch.Operation.ADD, path: '/',
                value: new Connection({ name: 'created', protocol: 'vnc' })
            })
        ];
        const patchFailure = new Error({
            patches: [
                new DirectoryPatchOutcome({ error: { key: 'IMPORT.ERROR_REMOVE', variables: { NAME: 'replaced' } } }),
                new DirectoryPatchOutcome({ error: { key: 'IMPORT.ERROR_ADD', variables: { NAME: 'replaced' } } }),
                new DirectoryPatchOutcome()
            ]
        });

        fixture.componentRef.setInput('parseResult', parseResult);
        fixture.componentRef.setInput('patches', patches);
        fixture.componentRef.setInput('patchFailure', patchFailure);

        expect(await getRows()).toEqual([
            ['1', 'replaced', 'ROOT/Existing', 'ssh', 'Cannot remove replaced', 'Cannot add replaced'],
            ['2', 'created', 'ROOT', 'vnc']
        ]);
    });

});
