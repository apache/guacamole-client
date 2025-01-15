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

import { DOCUMENT } from '@angular/common';
import { Component, Inject, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { FormFieldBaseComponent } from '../form-field-base/form-field-base.component';

/**
 * Component for the redirect field, which redirects the user to the provided
 * URL.
 */
@Component({
    selector: 'guac-redirect-field',
    templateUrl: './redirect-field.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RedirectFieldComponent extends FormFieldBaseComponent implements OnInit {

    /**
     * The ID value that should be used to associate
     * the relevant input element with the label provided by the
     * guacFormField component, if there is such an input element.
     */
    @Input() fieldId?: string;

    constructor(@Inject(DOCUMENT) private document: Document) {
        super();
    }

    /**
     * Redirect the user to the provided URL.
     */
    ngOnInit(): void {
        this.document.location.href = (this.field as any).redirectUrl;
    }

}
