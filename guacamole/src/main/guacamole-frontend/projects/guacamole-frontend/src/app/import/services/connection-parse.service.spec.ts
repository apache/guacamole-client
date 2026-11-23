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
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { ConnectionGroupService } from '../../rest/service/connection-group.service';
import { SchemaService } from '../../rest/service/schema.service';
import { ConnectionGroup } from '../../rest/types/ConnectionGroup';
import { Field } from '../../rest/types/Field';
import { Protocol } from '../../rest/types/Protocol';
import { ConnectionImportConfig } from '../types/ConnectionImportConfig';
import { ImportConnection } from '../types/ImportConnection';
import { ConnectionCSVService } from './connection-csv.service';
import { ConnectionParseService } from './connection-parse.service';

/**
 * The connection group tree of the data source: ROOT contains the group
 * "Existing" (identifier "1") with the connection "existing-conn" and the
 * child group "Child" (identifier "2").
 */
const TREE = {
    identifier: 'ROOT',
    name: 'ROOT',
    childConnectionGroups: [{
        identifier: '1',
        name: 'Existing',
        childConnections: [{ identifier: '10', name: 'existing-conn', protocol: 'ssh' }],
        childConnectionGroups: [{ identifier: '2', name: 'Child' }]
    }]
} as ConnectionGroup;

/**
 * The protocols of the data source; only SSH with a hostname parameter.
 */
const PROTOCOLS = {
    ssh: new Protocol({
        name: 'ssh',
        connectionForms: [{ name: 'network', fields: [new Field({ name: 'hostname' })] }]
    })
};

describe('ConnectionParseService', () => {

    let service: ConnectionParseService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                ConnectionParseService,
                ConnectionCSVService,
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { paramMap: convertToParamMap({ dataSource: 'postgresql' }) } }
                },
                {
                    provide: SchemaService,
                    useValue: {
                        getProtocols: () => of(PROTOCOLS),
                        getConnectionAttributes: () => of([])
                    }
                },
                { provide: ConnectionGroupService, useValue: { getConnectionGroupTree: () => of(TREE) } }
            ]
        });
        service = TestBed.inject(ConnectionParseService);
    });

    /**
     * Returns the translation keys of all errors of the given connection.
     *
     * @param connection
     *     The parsed connection.
     *
     * @returns
     *     The translation keys of the errors of the connection.
     */
    const errorKeys = (connection: ImportConnection): string[] =>
        connection.errors.map(error => error.key);

    it('should reject a group that does not exist if groups are not to be created', async () => {
        const result = await service.parseJSON(new ConnectionImportConfig(),
            JSON.stringify([{ name: 'conn', protocol: 'ssh', group: 'ROOT/Missing' }]));

        expect(result.hasErrors).toBe(true);
        expect(errorKeys(result.connectionObjects[0])).toEqual(['IMPORT.ERROR_INVALID_GROUP']);
    });

    it('should accept a group that does not exist if groups are to be created', async () => {
        const result = await service.parseJSON(new ConnectionImportConfig({ createMissingGroups: true }),
            JSON.stringify([{ name: 'conn', protocol: 'ssh', group: 'Missing/Sub/' }]));

        expect(result.hasErrors).toBe(false);
        expect(result.connectionCount).toBe(1);
        expect(result.connectionObjects.length).toBe(1);
        expect(result.connectionObjects[0].group).toBe('ROOT/Missing/Sub');
        expect(result.connectionObjects[0].parentIdentifier).toBeUndefined();
        expect(result.connectionObjects[0].importMode).toBe(ImportConnection.ImportMode.CREATE);
    });

    it('should collapse empty segments of a group path before looking up the group', async () => {
        const result = await service.parseCSV(new ConnectionImportConfig(),
            'name,protocol,group\nconn,ssh,/Existing//Child/\n');

        expect(result.hasErrors).toBe(false);
        expect(result.connectionObjects[0].group).toBe('ROOT/Existing/Child');
        expect(result.connectionObjects[0].parentIdentifier).toBe('2');
    });

    it('should derive the group path from a parent identifier', async () => {
        const result = await service.parseYAML(new ConnectionImportConfig(),
            '- name: conn\n  protocol: ssh\n  parentIdentifier: "2"\n');

        expect(result.hasErrors).toBe(false);
        expect(result.connectionObjects[0].group).toBe('ROOT/Existing/Child');
        expect(result.connectionObjects[0].parentIdentifier).toBe('2');
        expect(result.groupPaths).toEqual({ 0: 'ROOT/Existing/Child' });
    });

    it('should keep one entry per connection but count the REMOVE patch of a replaced connection', async () => {
        const result = await service.parseJSON(new ConnectionImportConfig({
                existingConnectionMode: ConnectionImportConfig.ExistingConnectionMode.REPLACE,
                existingPermissionMode: ConnectionImportConfig.ExistingPermissionMode.REPLACE
            }),
            JSON.stringify([
                { name: 'existing-conn', protocol: 'ssh', group: 'ROOT/Existing', users: ['alice'] },
                { name: 'new-conn', protocol: 'ssh', users: ['alice'] }
            ]));

        expect(result.hasErrors).toBe(false);
        expect(result.connectionCount).toBe(2);
        expect(result.connectionObjects.map(connection => connection.importMode)).toEqual([
            ImportConnection.ImportMode.REPLACE, ImportConnection.ImportMode.CREATE]);
        expect(result.connectionObjects[0].identifier).toBe('10');

        // The replaced connection results in a REMOVE and an ADD patch
        expect(result.users).toEqual({ alice: [1, 2] });
        expect(result.groupPaths).toEqual({ 1: 'ROOT/Existing', 2: 'ROOT' });
    });

});
