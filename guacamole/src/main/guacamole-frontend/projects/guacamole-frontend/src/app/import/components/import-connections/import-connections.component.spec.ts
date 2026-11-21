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

import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ListModule } from '../../../list/list.module';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';
import { CacheInterceptor } from '../../../rest/interceptor/cache.interceptor';
import { ErrorHandlingInterceptor } from '../../../rest/interceptor/error-handling.interceptor';
import { Connection } from '../../../rest/types/Connection';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { DirectoryPatch } from '../../../rest/types/DirectoryPatch';
import { ConnectionImportErrorsComponent } from '../connection-import-errors/connection-import-errors.component';
import { ImportConnectionsComponent } from './import-connections.component';

/**
 * The base URL of the REST API of the data source under test.
 */
const API = 'api/session/data/postgresql';

/**
 * The connection group tree of the data source: ROOT contains only the group
 * "Existing" with the identifier "1".
 */
const TREE = {
    identifier: 'ROOT',
    name: 'ROOT',
    childConnectionGroups: [{ identifier: '1', name: 'Existing' }]
};

/**
 * The protocols of the data source; only SSH.
 */
const PROTOCOLS = { ssh: { name: 'ssh', connectionForms: [], sharingProfileForms: [] } };

describe('ImportConnectionsComponent', () => {

    let fixture: ComponentFixture<ImportConnectionsComponent>;
    let httpMock: HttpTestingController;
    let showStatus: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        showStatus = vi.fn();
        TestBed.configureTestingModule({
            declarations: [ImportConnectionsComponent, ConnectionImportErrorsComponent],
            // Ignores the user menu and the upload directives of the page
            schemas: [NO_ERRORS_SCHEMA],
            imports: [
                CommonModule,
                FormsModule,
                ListModule,
                TranslocoTestingModule.forRoot({
                    langs: {
                        en: {
                            IMPORT: {
                                ERROR_INVALID_GROUP: 'No group matching "{{ GROUP }}" found',
                                ERROR_NAME_TAKEN: 'Name {{ NAME }} is taken'
                            }
                        }
                    },
                    translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
                })
            ],
            providers: [
                provideRouter([]),
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { paramMap: convertToParamMap({ dataSource: 'postgresql' }) } }
                },
                { provide: HTTP_INTERCEPTORS, useClass: CacheInterceptor, multi: true },
                { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlingInterceptor, multi: true },
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
                { provide: AuthenticationService, useValue: {} },
                { provide: GuacNotificationService, useValue: { showStatus } }
            ]
        });
        httpMock = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(ImportConnectionsComponent);
        fixture.componentRef.setInput('dataSource', 'postgresql');
        fixture.detectChanges();
    });

    afterEach(() => {
        httpMock.verify();
        vi.restoreAllMocks();
    });

    /**
     * Uploads the given connections as JSON file and starts the import, as
     * the user would on the import page.
     *
     * @param connections
     *     The connections of the import file.
     *
     * @param createMissingGroups
     *     Whether the user checks the option to create missing groups.
     */
    const importFile = async (connections: object[], createMissingGroups: boolean): Promise<void> => {
        const component = fixture.componentInstance;
        const element: HTMLElement = fixture.nativeElement;

        if (createMissingGroups) {
            element.querySelector<HTMLInputElement>('#create-missing-groups')!.click();
            fixture.detectChanges();
        }

        component.handleFiles([new File([JSON.stringify(connections)], 'import.json',
            { type: 'application/json' })] as unknown as FileList);
        await vi.waitFor(() => expect(component.dataReady).toBe(true));
        fixture.detectChanges();

        element.querySelector<HTMLButtonElement>('button.import')!.click();

        // The parser needs the protocols and the connection group tree
        (await expectRequest('GET', `${API}/schema/protocols`)).flush(PROTOCOLS);
        (await expectRequest('GET', `${API}/connectionGroups/ROOT/tree`)).flush(TREE);
    };

    /**
     * Waits until the component has sent exactly one request with the given
     * method and URL.
     *
     * @param method
     *     The HTTP method of the request.
     *
     * @param url
     *     The URL of the request.
     *
     * @returns
     *     The pending request.
     */
    const expectRequest = (method: string, url: string): Promise<TestRequest> =>
        vi.waitFor(() => httpMock.expectOne(request => request.method === method && request.url === url,
            `${method} ${url}`));

    /**
     * Expects the request to create a connection group with the given name
     * below the given parent, and answers it with the given identifier.
     *
     * @param name
     *     The expected name of the new group.
     *
     * @param parentIdentifier
     *     The expected identifier of the parent group.
     *
     * @param identifier
     *     The identifier which the REST API assigns to the new group.
     */
    const expectGroupCreated = async (name: string, parentIdentifier: string, identifier: string): Promise<void> => {
        const request = await expectRequest('POST', `${API}/connectionGroups`);
        const group: ConnectionGroup = request.request.body;
        expect({ name: group.name, parentIdentifier: group.parentIdentifier, type: group.type })
            .toEqual({ name, parentIdentifier, type: ConnectionGroup.Type.ORGANIZATIONAL });
        request.flush({ ...group, identifier });
    };

    /**
     * Expects the PATCH request for the connections and returns its patches
     * as operation, path, name and parent identifier.
     *
     * @returns
     *     The pending request and a summary of its patches.
     */
    const expectConnectionsPatched = async (): Promise<{ request: TestRequest, patches: object[] }> => {
        const request = await expectRequest('PATCH', `${API}/connections`);
        const patches = (request.request.body as DirectoryPatch<Connection>[]).map(patch => ({
            op: patch.op,
            path: patch.path,
            name: patch.value?.name,
            parentIdentifier: patch.value?.parentIdentifier
        }));
        return { request, patches };
    };

    /**
     * Waits until the page no longer shows the loading indicator and all
     * translations are applied, then returns the content of each row of the
     * error table: the texts of row number, name, group and protocol,
     * followed by each error message.
     *
     * @returns
     *     The trimmed texts of each row, one array per row.
     */
    const getErrorRows = async (): Promise<string[][]> => {
        await vi.waitFor(() => expect(fixture.componentInstance.isLoading()).toBe(false));
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
        const text = (element: Element) => element.textContent!.trim();
        return Array.from(fixture.nativeElement.querySelectorAll('connection-import-errors tbody tr') as NodeListOf<HTMLElement>)
            .map(row => [
                ...Array.from(row.querySelectorAll('td:not(.error-message)')).map(text),
                ...Array.from(row.querySelectorAll('td.error-message li')).map(text)
            ]);
    };

    it('should create missing groups parent first and import the connections into them', async () => {
        await importFile([
            { name: 'conn1', protocol: 'ssh', group: 'ROOT/New/Sub' },
            { name: 'conn2', protocol: 'ssh', group: 'New' },
            { name: 'conn3', protocol: 'ssh', group: 'Existing' }
        ], true);

        // One request at a time, each child below the identifier of its new parent
        await expectGroupCreated('New', 'ROOT', '5');
        await expectGroupCreated('Sub', '5', '6');

        const { request, patches } = await expectConnectionsPatched();
        expect(patches).toEqual([
            { op: DirectoryPatch.Operation.ADD, path: '/', name: 'conn1', parentIdentifier: '6' },
            { op: DirectoryPatch.Operation.ADD, path: '/', name: 'conn2', parentIdentifier: '5' },
            { op: DirectoryPatch.Operation.ADD, path: '/', name: 'conn3', parentIdentifier: '1' }
        ]);
        request.flush({ patches: [{ identifier: '20' }, { identifier: '21' }, { identifier: '22' }] });

        // No users or user groups to create or grant
        (await expectRequest('GET', `${API}/users`)).flush({});
        (await expectRequest('GET', `${API}/userGroups`)).flush({});
        (await expectRequest('PATCH', `${API}/users`)).flush(null);
        (await expectRequest('PATCH', `${API}/userGroups`)).flush(null);

        await vi.waitFor(() => expect(showStatus).toHaveBeenCalledWith(expect.objectContaining({
            className: 'success',
            text: { key: 'IMPORT.INFO_CONNECTIONS_IMPORTED_SUCCESS', variables: { NUMBER: 3 } }
        })));
        expect(await getErrorRows()).toEqual([]);
    });

    it('should not import any connection if a group path cannot be resolved after creating the groups', async () => {
        await importFile([
            { name: 'conn1', protocol: 'ssh', group: 'ROOT/Existing' },
            { name: 'conn2', protocol: 'ssh', group: 'ROOT/A/ROOT/B' }
        ], true);

        // The segments named "ROOT" are skipped, the path itself stays unknown
        await expectGroupCreated('A', 'ROOT', '7');
        await expectGroupCreated('B', '7', '8');

        expect(await getErrorRows()).toEqual([
            ['1', 'conn1', 'ROOT/Existing', 'ssh'],
            ['2', 'conn2', 'ROOT/A/ROOT/B', 'ssh', 'No group matching "ROOT/A/ROOT/B" found']
        ]);
        httpMock.expectNone(request => request.method === 'PATCH');
        expect(showStatus).not.toHaveBeenCalled();
    });

    it('should neither create groups nor import connections into a missing group without the option', async () => {
        await importFile([{ name: 'conn', protocol: 'ssh', group: 'ROOT/Missing' }], false);

        expect(await getErrorRows()).toEqual([
            ['1', 'conn', 'ROOT/Missing', 'ssh', 'No group matching "ROOT/Missing" found']
        ]);
        httpMock.expectNone(request => request.method !== 'GET');
    });

    it('should display an error and import nothing if a group cannot be created', async () => {
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        await importFile([{ name: 'conn', protocol: 'ssh', group: 'ROOT/New' }], true);

        const translatableMessage = { key: 'APP.TEXT_UNTRANSLATED', variables: { MESSAGE: 'Permission denied.' } };
        (await expectRequest('POST', `${API}/connectionGroups`)).flush(
            { message: 'Permission denied.', translatableMessage, type: 'PERMISSION_DENIED' },
            { status: 403, statusText: 'Forbidden' });

        await vi.waitFor(() => expect(showStatus).toHaveBeenCalledWith(expect.objectContaining({
            className: 'error',
            title: 'IMPORT.DIALOG_HEADER_ERROR',
            text: translatableMessage
        })));
        expect(consoleError).toHaveBeenCalledWith('Connection import failed:', expect.anything());
        expect(fixture.componentInstance.fileName).toBeNull();
        expect(await getErrorRows()).toEqual([]);
        httpMock.expectNone(request => request.method === 'PATCH');
    });

    it('should display the errors of a failed import for each connection of the file', async () => {
        await importFile([
            { name: 'conn1', protocol: 'ssh', group: 'Existing' },
            { name: 'conn2', protocol: 'ssh' }
        ], true);

        const { request } = await expectConnectionsPatched();
        request.flush({
            type: 'BAD_REQUEST',
            message: 'Import failed.',
            patches: [{}, { error: { key: 'IMPORT.ERROR_NAME_TAKEN', variables: { NAME: 'conn2' } } }]
        }, { status: 400, statusText: 'Bad Request' });

        expect(await getErrorRows()).toEqual([
            ['1', 'conn1', 'ROOT/Existing', 'ssh'],
            ['2', 'conn2', 'ROOT', 'ssh', 'Name conn2 is taken']
        ]);
        expect(fixture.componentInstance.fileName).toBe('import.json');
        expect(showStatus).not.toHaveBeenCalled();
    });

});
