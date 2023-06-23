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
 * Public API Surface of guacamole-frontend-lib
 */

import './lib/types/Guacamole'

// Client lib
export * from './lib/client/client.module';
export * from './lib/client/components/viewport.component';
export * from './lib/client/services/guac-audio.service';
export * from './lib/client/services/guac-image.service';
export * from './lib/client/services/guac-video.service';

// Element
export * from './lib/element/element.module';
export * from './lib/element/directives/guac-click.directive';
export * from './lib/element/directives/guac-focus.directive';
export * from './lib/element/directives/guac-resize.directive';
export * from './lib/element/directives/guac-scroll.directive';
export * from './lib/element/directives/guac-upload.directive';
export * from './lib/element/types/ScrollState';

// Events
export * from './lib/events/types/GuacEvent';
export * from './lib/events/types/GuacEventArguments';
export * from './lib/events/services/guac-event.service';

// OSK
export * from './lib/osk/osk.module';
export * from './lib/osk/components/osk/osk.component';

// Touch
export * from './lib/touch/touch.module';
export * from './lib/touch/directives/guac-touch-pinch.directive';
export * from './lib/touch/directives/guac-touch-drag.directive';
