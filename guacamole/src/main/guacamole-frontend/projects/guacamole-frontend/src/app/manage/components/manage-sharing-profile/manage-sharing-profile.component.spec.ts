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
import { of } from 'rxjs';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { FormService } from '../../../form/service/form.service';
import { ConnectionService } from '../../../rest/service/connection.service';
import { PermissionService } from '../../../rest/service/permission.service';
import { RequestService } from '../../../rest/service/request.service';
import { SchemaService } from '../../../rest/service/schema.service';
import { SharingProfileService } from '../../../rest/service/sharing-profile.service';
import { Connection } from '../../../rest/types/Connection';
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

describe('ManageSharingProfileComponent', () => {
    let fixture: ComponentFixture<ManageSharingProfileComponent>;

    beforeEach(() => {
        const sharingProfile = new SharingProfile({
            identifier: '1',
            name: 'view only',
            primaryConnectionIdentifier: '2'
        });
        const connection = new Connection({
            identifier: '2',
            name: 'ssh-target',
            protocol: 'ssh'
        });

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
                    useValue: { snapshot: { queryParamMap: convertToParamMap({}) } }
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
                        getSharingProfileAttributes: () => of([]),
                        getProtocols: () => of({ ssh: new Protocol({ name: 'ssh' }) })
                    }
                },
                {
                    provide: SharingProfileService,
                    useValue: {
                        getSharingProfile: () => of(sharingProfile),
                        getSharingProfileParameters: () => of({})
                    }
                },
                {
                    provide: FormService,
                    useValue: { getFormGroup: () => new FormGroup({}) }
                }
            ]
        });
        fixture = TestBed.createComponent(ManageSharingProfileComponent);
        fixture.componentRef.setInput('dataSource', 'postgresql');
        fixture.componentRef.setInput('id', '1');
        fixture.detectChanges();
        fixture.detectChanges();
    });

    it('should pass the sharing profile attributes namespace to guac-form without quotes', () => {
        const forms = fixture.debugElement.queryAll(By.directive(GuacFormStubComponent))
            .map(form => form.componentInstance as GuacFormStubComponent);

        expect(forms.length).toBeGreaterThan(0);
        expect(forms[0].namespace).toBe('SHARING_PROFILE_ATTRIBUTES');
    });
});
