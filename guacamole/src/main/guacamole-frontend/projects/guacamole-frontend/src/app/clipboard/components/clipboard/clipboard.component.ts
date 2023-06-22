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

import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import {
    GuacFrontendEventArguments
} from '../../../events/types/GuacFrontendEventArguments';
import { ClipboardData } from '../../types/ClipboardData';
import { ClipboardService } from '../../services/clipboard.service';
import { Subject } from 'rxjs';

@Component({
    selector: 'guac-clipboard',
    templateUrl: './clipboard.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ClipboardComponent implements OnInit, AfterViewInit, OnDestroy {

    /**
     * The DOM element which will contain the clipboard contents within the
     * user interface provided by this directive. We populate the clipboard
     * editor via this DOM element rather than updating a model so that we
     * are prepared for future support of rich text contents.
     */
    @ViewChild('activeClipboardTextarea') element!: ElementRef<HTMLTextAreaElement>;

    destroyed = new Subject<void>();

    constructor(private readonly clipboardService: ClipboardService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>) {
    }

    /**
     * When isActive is set to true then the Clipboard data will be
     * displayed in the Clipboard Editor. When false, the Clipboard Editor
     * will not be displayed with Clipboard data.
     */
    isActive = false;

    /**
     * Updates clipboard editor to be active.
     */
    setActive() {
        this.isActive = true;
    }

    /**
     * Rereads the contents of the clipboard field, updating the
     * ClipboardData object on the scope as necessary. The type of data
     * stored within the ClipboardData object will be heuristically
     * determined from the HTML contents of the clipboard field.
     */
    updateClipboardData() {
        // Read contents of clipboard textarea
        this.clipboardService.setClipboard(new ClipboardData({
            type: 'text/plain',
            data: this.element.nativeElement.value
        }));
    }

    /**
     * Updates the contents of the clipboard editor to the given data.
     *
     * @param data
     *     The ClipboardData to display within the clipboard editor for
     *     editing.
     */
    updateClipboardEditor(data: ClipboardData) {
        // If the clipboard data is a string, render it as text
        if (typeof data.data === 'string') {
            this.element.nativeElement.value = data.data;
        }

        // Ignore other data types for now
    }

    ngOnInit(): void {
        // Update remote clipboard if local clipboard changes
        this.guacEventService.on('guacClipboard')
            .subscribe(({data}) => {
                this.updateClipboardEditor(data);
            });
    }

    ngAfterViewInit(): void {

        // Init clipboard editor with current clipboard contents
        this.clipboardService.getClipboard().then((data) => {
            this.updateClipboardEditor(data);
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }
}
