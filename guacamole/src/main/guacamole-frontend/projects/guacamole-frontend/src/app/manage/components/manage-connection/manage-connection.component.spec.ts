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

import { Component, EventEmitter, Input, NO_ERRORS_SCHEMA, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { firstValueFrom, of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { GuacPagerComponent } from '../../../list/components/guac-pager/guac-pager.component';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { ConnectionService } from '../../../rest/service/connection.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { Connection } from '../../../rest/types/Connection';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { Protocol } from '../../../rest/types/Protocol';
import { OrderByPipe } from '../../../util/order-by.pipe';
import { ManageConnectionComponent } from './manage-connection.component';

/**
 * Stand-in for the guac-form component which exposes the inputs the template
 * of ManageConnectionComponent binds.
 */
@Component({
    selector: 'guac-form',
    template: '',
    standalone: false
})
class GuacFormStubComponent {

    /**
     * The translation namespace passed by the template.
     */
    @Input() namespace?: string;

    /**
     * The form definitions passed by the template.
     */
    @Input() content?: Form[];

    /**
     * The form group passed by the template.
     */
    @Input() modelFormGroup?: FormGroup;

    /**
     * Whether only model attributes are passed by the template.
     */
    @Input() modelOnly = false;
}

/**
 * Stand-in for the location chooser which exposes its two-way binding of the
 * parent identifier, so that a test can choose a group as the user would.
 */
@Component({
    selector: 'guac-location-chooser',
    template: '',
    standalone: false
})
class LocationChooserStubComponent {

    /**
     * The data source passed by the template.
     */
    @Input() dataSource?: string;

    /**
     * The root group passed by the template.
     */
    @Input() rootGroup?: ConnectionGroup;

    /**
     * The identifier of the parent group passed by the template.
     */
    @Input() value?: string;

    /**
     * Emits the identifier of a chosen group.
     */
    @Output() valueChange = new EventEmitter<string | undefined>();
}

/**
 * Attribute definitions with a checkbox and a text field.
 */
const ATTRIBUTES: Form[] = [new Form({
    name: 'connection-attributes',
    fields: [
        new Field({ name: 'flag', type: Field.Type.BOOLEAN, options: ['true'] }),
        new Field({ name: 'note', type: Field.Type.TEXT })
    ]
})];

/**
 * The connection editor opened by {@link openEditor}, with the service of
 * which the editor saves the connection.
 */
interface OpenedEditor {

    /**
     * The fixture of the editor component.
     */
    fixture: ComponentFixture<ManageConnectionComponent>;

    /**
     * The form which the editor shows for the attributes of the connection.
     */
    attributeForm: GuacFormStubComponent;

    /**
     * The service which saves the connection.
     */
    connectionService: {
        saveConnection: ReturnType<typeof vi.fn>;
    };
}

/**
 * Opens the editor for the SSH connection "1" as an administrator, or for a
 * new connection.
 *
 * @param identifier
 *     The identifier of the connection to edit, or undefined to create a new
 *     connection.
 *
 * @param storedAttributes
 *     The attributes stored for the existing connection. A value of null
 *     stands for an attribute which is defined but not set, as JDBC returns
 *     it.
 *
 * @returns
 *     The editor once all requested data has been loaded.
 */
const openEditor = async (identifier: string | undefined,
                          storedAttributes: Record<string, string | null> = {}): Promise<OpenedEditor> => {
    const connectionService = {
        getConnection: () => of(new Connection({
            identifier: '1',
            name: 'ssh-target',
            protocol: 'ssh',
            attributes: { ...storedAttributes } as Record<string, string>
        })),
        getConnectionHistory: () => of([]),
        getConnectionParameters: () => of({}),
        saveConnection: vi.fn(() => of(undefined))
    };

    TestBed.configureTestingModule({
        declarations: [ManageConnectionComponent, GuacFormStubComponent, LocationChooserStubComponent, GuacPagerComponent],
        // Ignores guac-user-menu and management-buttons, which this spec does not exercise
        schemas: [NO_ERRORS_SCHEMA],
        imports: [
            FormsModule,
            OrderByPipe,
            // The editor waits for the translated format of history dates
            TranslocoTestingModule.forRoot({
                langs: { en: {} },
                translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
            })
        ],
        providers: [
            { provide: Router, useValue: {} },
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { queryParamMap: convertToParamMap({}) } }
            },
            {
                provide: AuthenticationService,
                useValue: { getCurrentUsername: () => 'guacadmin' }
            },
            { provide: ConnectionService, useValue: connectionService },
            {
                provide: ConnectionGroupService,
                useValue: {
                    getConnectionGroupTree: () => of(new ConnectionGroup({
                        identifier: ConnectionGroup.ROOT_IDENTIFIER,
                        name: 'ROOT',
                        activeConnections: 0
                    }))
                }
            },
            {
                provide: PermissionService,
                useValue: {
                    getEffectivePermissions: () => of(new PermissionSet({
                        systemPermissions: [PermissionSet.SystemPermissionType.ADMINISTER]
                    }))
                }
            },
            { provide: RequestService, useValue: { DIE: vi.fn() } },
            {
                provide: SchemaService,
                useValue: {
                    getConnectionAttributes: () => of(ATTRIBUTES),
                    getProtocols: () => of({
                        ssh: new Protocol({ name: 'ssh' }),
                        vnc: new Protocol({ name: 'vnc' })
                    })
                }
            }
        ]
    });
    const fixture = TestBed.createComponent(ManageConnectionComponent);
    fixture.componentRef.setInput('dataSource', 'postgresql');
    if (identifier)
        fixture.componentRef.setInput('id', identifier);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return {
        fixture,
        attributeForm: fixture.debugElement.queryAll(By.directive(GuacFormStubComponent))[0]?.componentInstance,
        connectionService
    };
};

describe('ManageConnectionComponent', () => {

    describe('attributes', () => {

        it('should provide a control for each attribute while creating a new connection', async () => {
            const { attributeForm } = await openEditor(undefined);

            expect(attributeForm.namespace).toBe('CONNECTION_ATTRIBUTES');
            expect(attributeForm.modelOnly).toBe(false);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag', 'note']);
        });

        it('should provide controls only for the attributes of an existing connection, even those without a value', async () => {
            const { attributeForm } = await openEditor('1', { flag: null });

            expect(attributeForm.modelOnly).toBe(true);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag']);
        });

        it('should save only the attributes of an existing connection', async () => {
            const { fixture, attributeForm, connectionService } = await openEditor('1', { flag: null });

            attributeForm.modelFormGroup?.get('flag')?.setValue('true');
            await firstValueFrom(fixture.componentInstance.saveConnection());

            expect(connectionService.saveConnection).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                attributes: { flag: 'true' }
            }));
        });

    });

    describe('location', () => {

        it('should save the connection within the group chosen in the location chooser', async () => {
            const { fixture, connectionService } = await openEditor('1');

            const chooser: LocationChooserStubComponent = fixture.debugElement.query(By.directive(LocationChooserStubComponent)).componentInstance;
            chooser.valueChange.emit('7');
            fixture.detectChanges();
            await firstValueFrom(fixture.componentInstance.saveConnection());

            expect(connectionService.saveConnection).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                parentIdentifier: '7'
            }));
        });

    });

});
