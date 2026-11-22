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
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { ManageConnectionGroupComponent } from './manage-connection-group.component';

/**
 * Stand-in for the guac-form component which exposes the inputs the template
 * of ManageConnectionGroupComponent binds.
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
    name: 'connection-group-attributes',
    fields: [
        new Field({ name: 'flag', type: Field.Type.BOOLEAN, options: ['true'] }),
        new Field({ name: 'note', type: Field.Type.TEXT })
    ]
})];

/**
 * The connection group editor opened by {@link openEditor}, with the service
 * of which the editor saves the connection group.
 */
interface OpenedEditor {

    /**
     * The fixture of the editor component.
     */
    fixture: ComponentFixture<ManageConnectionGroupComponent>;

    /**
     * The form which the editor shows for the attributes of the connection
     * group.
     */
    attributeForm: GuacFormStubComponent;

    /**
     * The service which saves the connection group.
     */
    connectionGroupService: {
        saveConnectionGroup: ReturnType<typeof vi.fn>;
    };
}

/**
 * Opens the editor for the connection group "1" as an administrator, or for
 * a new connection group.
 *
 * @param identifier
 *     The identifier of the connection group to edit, or undefined to create
 *     a new connection group.
 *
 * @param storedAttributes
 *     The attributes stored for the existing connection group. A value of
 *     null stands for an attribute which is defined but not set, as JDBC
 *     returns it.
 *
 * @returns
 *     The editor once all requested data has been loaded.
 */
const openEditor = (identifier: string | undefined,
                    storedAttributes: Record<string, string | null> = {}): OpenedEditor => {
    const connectionGroupService = {
        getConnectionGroup: () => of(new ConnectionGroup({
            identifier: '1',
            name: 'group',
            type: ConnectionGroup.Type.ORGANIZATIONAL,
            activeConnections: 0,
            attributes: { ...storedAttributes } as Record<string, string>
        })),
        getConnectionGroupTree: () => of(new ConnectionGroup({
            identifier: ConnectionGroup.ROOT_IDENTIFIER,
            name: 'ROOT',
            type: ConnectionGroup.Type.ORGANIZATIONAL,
            activeConnections: 0
        })),
        saveConnectionGroup: vi.fn(() => of(undefined))
    };

    TestBed.configureTestingModule({
        declarations: [ManageConnectionGroupComponent, GuacFormStubComponent, LocationChooserStubComponent],
        // Ignores guac-user-menu and management-buttons, which this spec does not exercise
        schemas: [NO_ERRORS_SCHEMA],
        imports: [
            FormsModule,
            TranslocoTestingModule.forRoot({})
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
            { provide: ConnectionGroupService, useValue: connectionGroupService },
            {
                provide: PermissionService,
                useValue: {
                    getEffectivePermissions: () => of(new PermissionSet({
                        systemPermissions: [PermissionSet.SystemPermissionType.ADMINISTER]
                    }))
                }
            },
            { provide: RequestService, useValue: { DIE: vi.fn() } },
            { provide: SchemaService, useValue: { getConnectionGroupAttributes: () => of(ATTRIBUTES) } }
        ]
    });
    const fixture = TestBed.createComponent(ManageConnectionGroupComponent);
    fixture.componentRef.setInput('dataSource', 'postgresql');
    if (identifier)
        fixture.componentRef.setInput('id', identifier);
    fixture.detectChanges();
    fixture.detectChanges();
    return {
        fixture,
        attributeForm: fixture.debugElement.query(By.directive(GuacFormStubComponent)).componentInstance,
        connectionGroupService
    };
};

describe('ManageConnectionGroupComponent', () => {

    describe('attributes', () => {

        it('should provide a control for each attribute while creating a new connection group', () => {
            const { attributeForm } = openEditor(undefined);

            expect(attributeForm.modelOnly).toBe(false);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag', 'note']);
        });

        it('should provide controls only for the attributes of an existing connection group, even those without a value', () => {
            const { attributeForm } = openEditor('1', { flag: null });

            expect(attributeForm.modelOnly).toBe(true);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag']);
        });

        it('should save only the attributes of an existing connection group', async () => {
            const { fixture, attributeForm, connectionGroupService } = openEditor('1', { flag: null });

            attributeForm.modelFormGroup?.get('flag')?.setValue('true');
            await firstValueFrom(fixture.componentInstance.saveConnectionGroup());

            expect(connectionGroupService.saveConnectionGroup).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                attributes: { flag: 'true' }
            }));
        });

    });

    describe('location', () => {

        it('should save the connection group within the group chosen in the location chooser', async () => {
            const { fixture, connectionGroupService } = await openEditor('1');

            const chooser: LocationChooserStubComponent = fixture.debugElement.query(By.directive(LocationChooserStubComponent)).componentInstance;
            chooser.valueChange.emit('7');
            fixture.detectChanges();
            await firstValueFrom(fixture.componentInstance.saveConnectionGroup());

            expect(connectionGroupService.saveConnectionGroup).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                parentIdentifier: '7'
            }));
        });

    });

});
