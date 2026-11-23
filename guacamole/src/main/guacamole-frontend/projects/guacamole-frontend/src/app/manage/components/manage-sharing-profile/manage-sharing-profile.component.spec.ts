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


import { Component, Input, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { firstValueFrom, of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ConnectionService } from '../../../rest/service/connection.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { SharingProfileService } from '../../../rest/service/sharing-profile.service';
import { Connection } from '../../../rest/types/Connection';
import { Field } from '../../../rest/types/Field';
import { Form } from '../../../rest/types/Form';
import { PermissionSet } from '../../../rest/types/PermissionSet';
import { Protocol } from '../../../rest/types/Protocol';
import { SharingProfile } from '../../../rest/types/SharingProfile';
import { ManageSharingProfileComponent } from './manage-sharing-profile.component';

/**
 * Stand-in for the guac-form component which exposes the inputs the template
 * of ManageSharingProfileComponent binds.
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
 * Attribute definitions with a checkbox and a text field, for the specs
 * which exercise the attributes of a sharing profile.
 */
const ATTRIBUTES: Form[] = [new Form({
    name: 'sharing-profile-attributes',
    fields: [
        new Field({ name: 'flag', type: Field.Type.BOOLEAN, options: ['true'] }),
        new Field({ name: 'note', type: Field.Type.TEXT })
    ]
})];

/**
 * The sharing profile editor opened by {@link openEditor}, with the service
 * of which the editor saves the sharing profile.
 */
interface OpenedEditor {

    /**
     * The fixture of the editor component.
     */
    fixture: ComponentFixture<ManageSharingProfileComponent>;

    /**
     * The form which the editor shows for the attributes of the sharing
     * profile.
     */
    attributeForm: GuacFormStubComponent;

    /**
     * The service which saves the sharing profile.
     */
    sharingProfileService: {
        saveSharingProfile: ReturnType<typeof vi.fn>;
    };
}

/**
 * Opens the editor for the sharing profile "1" of the connection "2" as an
 * administrator, or for a new sharing profile of that connection.
 *
 * @param identifier
 *     The identifier of the sharing profile to edit, or undefined to create
 *     a new sharing profile.
 *
 * @param attributes
 *     The attribute definitions which the data source offers for sharing
 *     profiles.
 *
 * @param storedAttributes
 *     The attributes stored for the existing sharing profile. A value of null
 *     stands for an attribute which is defined but not set, as JDBC returns
 *     it.
 *
 * @returns
 *     The editor once all requested data has been loaded.
 */
const openEditor = (identifier: string | undefined, attributes: Form[] = [],
                    storedAttributes: Record<string, string | null> = {}): OpenedEditor => {
    const sharingProfile = new SharingProfile({
        identifier: '1',
        name: 'view only',
        primaryConnectionIdentifier: '2',
        attributes: { ...storedAttributes } as Record<string, string>
    });
    const connection = new Connection({
        identifier: '2',
        name: 'ssh-target',
        protocol: 'ssh'
    });
    const sharingProfileService = {
        getSharingProfile: () => of(sharingProfile),
        getSharingProfileParameters: () => of({}),
        saveSharingProfile: vi.fn(() => of(undefined))
    };

    TestBed.configureTestingModule({
        declarations: [ManageSharingProfileComponent, GuacFormStubComponent],
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
                useValue: { snapshot: { queryParamMap: convertToParamMap(identifier ? {} : { parent: '2' }) } }
            },
            {
                provide: AuthenticationService,
                useValue: { getCurrentUsername: () => 'guacadmin' }
            },
            {
                provide: ConnectionService,
                useValue: { getConnection: () => of(connection) }
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
                    getSharingProfileAttributes: () => of(attributes),
                    getProtocols: () => of({ ssh: new Protocol({ name: 'ssh' }) })
                }
            },
            { provide: SharingProfileService, useValue: sharingProfileService }
        ]
    });
    const fixture = TestBed.createComponent(ManageSharingProfileComponent);
    fixture.componentRef.setInput('dataSource', 'postgresql');
    if (identifier)
        fixture.componentRef.setInput('id', identifier);
    fixture.detectChanges();
    fixture.detectChanges();
    return {
        fixture,
        attributeForm: fixture.debugElement.queryAll(By.directive(GuacFormStubComponent))[0]?.componentInstance,
        sharingProfileService
    };
};

describe('ManageSharingProfileComponent', () => {

    it('should pass the sharing profile attributes namespace to guac-form without quotes', () => {
        const { fixture } = openEditor('1');
        const forms = fixture.debugElement.queryAll(By.directive(GuacFormStubComponent))
            .map(form => form.componentInstance as GuacFormStubComponent);

        expect(forms.length).toBeGreaterThan(0);
        expect(forms[0].namespace).toBe('SHARING_PROFILE_ATTRIBUTES');
    });

    describe('attributes', () => {

        it('should provide a control for each attribute while creating a new sharing profile', () => {
            const { attributeForm } = openEditor(undefined, ATTRIBUTES);

            expect(attributeForm.modelOnly).toBe(false);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag', 'note']);
        });

        it('should provide controls only for the attributes of an existing sharing profile, even those without a value', () => {
            const { attributeForm } = openEditor('1', ATTRIBUTES, { flag: null });

            expect(attributeForm.modelOnly).toBe(true);
            expect(Object.keys(attributeForm.modelFormGroup?.controls ?? {})).toEqual(['flag']);
        });

        it('should save only the attributes of an existing sharing profile', async () => {
            const { fixture, attributeForm, sharingProfileService } = openEditor('1', ATTRIBUTES, { flag: null });

            attributeForm.modelFormGroup?.get('flag')?.setValue('true');
            await firstValueFrom(fixture.componentInstance.saveSharingProfile());

            expect(sharingProfileService.saveSharingProfile).toHaveBeenCalledWith('postgresql', expect.objectContaining({
                attributes: { flag: 'true' }
            }));
        });

    });

});
