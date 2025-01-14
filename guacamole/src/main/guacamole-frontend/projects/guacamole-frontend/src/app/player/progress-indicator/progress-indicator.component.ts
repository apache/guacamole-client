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

/*
 * NOTE: This session recording player implementation is based on the Session
 * Recording Player for Glyptodon Enterprise which is available at
 * https://github.com/glyptodon/glyptodon-enterprise-player under the
 * following license:
 *
 * Copyright (C) 2019 Glyptodon, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import { Component, Input, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';

/**
 * Component which displays an indicator showing the current progress of an
 * arbitrary operation.
 */
@Component({
    selector     : 'guac-player-progress-indicator',
    templateUrl  : './progress-indicator.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ProgressIndicatorComponent implements OnChanges {

    /**
     * A value between 0 and 1 inclusive which indicates current progress,
     * where 0 represents no progress and 1 represents finished.
     */
    @Input() progress?: number;

    /**
     * The current progress of the operation as a percentage. This value is
     * automatically updated as this.progress changes.
     */
    percentage = 0;

    /**
     * The CSS transform which should be applied to the bar portion of the
     * progress indicator. This value is automatically updated as
     * this.progress changes.
     */
    barTransform?: string = undefined;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['progress']) {
            // Keep percentage and bar transform up-to-date with changes to
            // progress value
            const progress = this.progress || 0;
            this.percentage = Math.floor(progress * 100);
            this.barTransform = 'rotate(' + (360 * progress - 45) + 'deg)';
        }
    }
}
