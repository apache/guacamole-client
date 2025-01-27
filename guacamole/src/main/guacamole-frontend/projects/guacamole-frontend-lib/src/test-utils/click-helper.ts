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

/**
 * TODO: Document
 * @param element
 * @param eventName
 * @param coordX
 * @param coordY
 */
const dispatchEvent = (element: any, eventName: string, coordX: any, coordY: any) => {
    element.dispatchEvent(new MouseEvent(eventName, {
        view      : window,
        bubbles   : true,
        cancelable: true,
        clientX   : coordX,
        clientY   : coordY,
        button    : 0
    }));
};

/**
 * TODO: Document
 * @param element
 */
export const simulateMouseClick = (element: any) => {
    const box = element.getBoundingClientRect();
    const coordX = box.left + (box.right - box.left) / 2;
    const coordY = box.top + (box.bottom - box.top) / 2;

    dispatchEvent(element, 'mousedown', coordX, coordY);
    dispatchEvent(element, 'mouseup', coordX, coordY);
    dispatchEvent(element, 'click', coordX, coordY);
};
