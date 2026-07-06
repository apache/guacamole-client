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

/* global Guacamole, expect */

describe("Guacamole.Display", function DisplaySpec() {

    /**
     * The display under test.
     *
     * @type {!Guacamole.Display}
     */
    var display;

    beforeEach(function() {
        display = new Guacamole.Display();
    });

    it("should re-crop the default layer when monitor size changes", function() {

        display.resize(display.getDefaultLayer(), 5120, 1598);
        display.flush();

        expect(display.getWidth()).toBe(5120);
        expect(display.getHeight()).toBe(1598);

        display.setMonitorSize(2560, 1422);
        display.flush();

        expect(display.getDefaultLayer().width).toBe(2560);
        expect(display.getDefaultLayer().height).toBe(1422);
        expect(display.getWidth()).toBe(2560);
        expect(display.getHeight()).toBe(1422);

    });

    it("should constrain later default layer resizes to current monitor size", function() {

        display.setMonitorSize(2560, 1422);
        display.resize(display.getDefaultLayer(), 5120, 1598);
        display.flush();

        expect(display.getDefaultLayer().width).toBe(2560);
        expect(display.getDefaultLayer().height).toBe(1422);
        expect(display.getWidth()).toBe(2560);
        expect(display.getHeight()).toBe(1422);

    });

});
