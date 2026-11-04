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

/* global Guacamole, VideoFrame, expect, pending */

describe('Guacamole.Display', function() {

    /**
     * Width of the test image, in pixels. Deliberately not a multiple of the
     * 64-pixel granularity used for resizing layer canvases.
     *
     * @type {!number}
     */
    var IMAGE_WIDTH = 37;

    /**
     * Height of the test image, in pixels. Deliberately not a multiple of the
     * 64-pixel granularity used for resizing layer canvases.
     *
     * @type {!number}
     */
    var IMAGE_HEIGHT = 21;

    /**
     * Returns a new canvas of IMAGE_WIDTH x IMAGE_HEIGHT pixels filled with
     * opaque red.
     *
     * @returns {!HTMLCanvasElement}
     *     A new canvas filled with opaque red.
     */
    var createTestCanvas = function createTestCanvas() {
        var canvas = document.createElement('canvas');
        canvas.width = IMAGE_WIDTH;
        canvas.height = IMAGE_HEIGHT;

        var context = canvas.getContext('2d');
        context.fillStyle = '#FF0000';
        context.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        return canvas;
    };

    /**
     * Returns the base64-encoded contents of a PNG image of IMAGE_WIDTH x
     * IMAGE_HEIGHT pixels filled with opaque red, as would be sent within
     * the "blob" instructions of an image stream.
     *
     * @returns {!string}
     *     The base64-encoded PNG image data.
     */
    var createTestPNG = function createTestPNG() {
        var url = createTestCanvas().toDataURL('image/png');
        return url.substring(url.indexOf(',') + 1);
    };

    /**
     * Returns the RGBA components of the pixel at the given coordinates
     * within the given layer.
     *
     * @param {!Guacamole.Layer} layer
     *     The layer to read from.
     *
     * @param {!number} x
     *     The X coordinate of the pixel.
     *
     * @param {!number} y
     *     The Y coordinate of the pixel.
     *
     * @returns {!number[]}
     *     The red, green, blue, and alpha components of the pixel.
     */
    var getPixel = function getPixel(layer, x, y) {
        var data = layer.getCanvas().getContext('2d').getImageData(x, y, 1, 1).data;
        return [ data[0], data[1], data[2], data[3] ];
    };

    /**
     * Sends the given base64-encoded image data along a new
     * Guacamole.InputStream which is drawn to the given layer using
     * Guacamole.Display.drawStream(), in the same manner as the "img"
     * instruction.
     *
     * @param {!Guacamole.Display} display
     *     The display to draw with.
     *
     * @param {!Guacamole.Layer} layer
     *     The layer to draw to.
     *
     * @param {!string} data
     *     The base64-encoded image data to send.
     */
    var drawStream = function drawStream(display, layer, data) {
        var stream = new Guacamole.InputStream(null, 0);
        display.drawStream(layer, 0, 0, stream, 'image/png');
        stream.onblob(data);
        stream.onend();
    };

    describe('drawImage', function() {

        it('should grow autosized buffers to fit a VideoFrame', function() {

            if (!window.VideoFrame)
                pending('VideoFrame is not supported by this browser');

            var display = new Guacamole.Display();
            var buffer = display.createBuffer();

            var frame = new VideoFrame(createTestCanvas(), { 'timestamp' : 0 });
            buffer.drawImage(3, 5, frame);
            frame.close();

            expect(buffer.width).toBe(IMAGE_WIDTH + 3);
            expect(buffer.height).toBe(IMAGE_HEIGHT + 5);
            expect(getPixel(buffer, IMAGE_WIDTH + 2, IMAGE_HEIGHT + 4)).toEqual([ 255, 0, 0, 255 ]);

        });

        it('should grow autosized buffers to fit a canvas', function() {

            var display = new Guacamole.Display();
            var buffer = display.createBuffer();

            buffer.drawImage(0, 0, createTestCanvas());

            expect(buffer.width).toBe(IMAGE_WIDTH);
            expect(buffer.height).toBe(IMAGE_HEIGHT);
            expect(getPixel(buffer, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1)).toEqual([ 255, 0, 0, 255 ]);

        });

    });

    describe('drawStream', function() {

        it('should grow autosized buffers to fit the received image', function(done) {

            var display = new Guacamole.Display();
            var buffer = display.createBuffer();

            drawStream(display, buffer, createTestPNG());

            display.flush(function frameComplete() {
                expect(buffer.width).toBe(IMAGE_WIDTH);
                expect(buffer.height).toBe(IMAGE_HEIGHT);
                expect(getPixel(buffer, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1)).toEqual([ 255, 0, 0, 255 ]);
                done();
            });

        });

        it('should allow images drawn to buffers to be copied to visible layers', function(done) {

            var display = new Guacamole.Display();
            var buffer = display.createBuffer();
            var layer = display.getDefaultLayer();

            display.resize(layer, 64, 64);
            drawStream(display, buffer, createTestPNG());
            display.copy(buffer, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, layer, 10, 10);

            display.flush(function frameComplete() {
                expect(getPixel(layer, 10, 10)).toEqual([ 255, 0, 0, 255 ]);
                expect(getPixel(layer, 10 + IMAGE_WIDTH - 1, 10 + IMAGE_HEIGHT - 1)).toEqual([ 255, 0, 0, 255 ]);
                expect(getPixel(layer, 10 + IMAGE_WIDTH, 10)).toEqual([ 0, 0, 0, 0 ]);
                done();
            });

        });

    });

});
