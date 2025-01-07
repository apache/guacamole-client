

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

import { Field } from '../../rest/types/Field';
import { Form } from '../../rest/types/Form';
import { TranslatableMessage } from '../../rest/types/TranslatableMessage';
import { NotificationAction } from './NotificationAction';
import { NotificationCountdown } from './NotificationCountdown';
import { NotificationProgress } from './NotificationProgress';

/**
 * Provides the Notification class.
 */
export class Notification {

    /**
     * The CSS class to associate with the notification, if any.
     */
    className?: string;

    /**
     * The title of the notification.
     */
    title?: string;

    /**
     * The body text of the notification.
     */
    text?: TranslatableMessage;

    /**
     * The translation namespace of the translation strings that will
     * be generated for all fields within the notification. This namespace
     * is absolutely required if form fields will be included in the
     * notification.
     */
    formNamespace?: string;

    /**
     * Optional form content to display. This may be a form, an array of
     * forms, or a simple array of fields.
     */
    forms?: Form[] | Form | Field[] | Field;

    /**
     * The object which will receive all field values. Each field value
     * will be assigned to the property of this object having the same
     * name.
     */
    formModel?: Record<string, string>;

    /**
     * The function to invoke when the form is submitted, if form fields
     * are present within the notification.
     */
    formSubmitCallback?: Function;

    /**
     * An array of all actions available to the user in response to this
     * notification.
     */
    actions: NotificationAction[];

    /**
     * The current progress state of the ongoing action associated with this
     * notification.
     */
    progress?: NotificationProgress;

    /**
     * The countdown and corresponding default action which applies to
     * this notification, if any.
     */
    countdown?: NotificationCountdown;

    /**
     * Creates a new Notification, initializing the properties of that
     * Notification with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Notification.
     */
    constructor(template: Partial<Notification> = {}) {
        this.className = template.className;
        this.title = template.title;
        this.text = template.text;
        this.formNamespace = template.formNamespace;
        this.forms = template.forms;
        this.formModel = template.formModel;
        this.formSubmitCallback = template.formSubmitCallback;
        this.actions = template.actions || [];
        this.progress = template.progress;
        this.countdown = template.countdown;
    }

}
