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

// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
    config.set({
        basePath: '',
        frameworks: ['jasmine', '@angular-devkit/build-angular'],
        plugins: [
            require('karma-jasmine'),
            require('karma-firefox-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-coverage'),
            require('@angular-devkit/build-angular/plugins/karma')
        ],
        jasmineHtmlReporter: {
            suppressAll: true // removes the duplicated traces
        },
        coverageReporter: {
            dir: require('path').join(__dirname, '../../coverage/guacamole-frontend-lib'),
            subdir: '.',
            reporters: [
                {type: 'html'},
                {type: 'text-summary'}
            ]
        },
        reporters: ['progress', 'kjhtml'],

        // Run the tests once and exit
        singleRun: true,

        // Disable automatic test running on changed files
        autoWatch: false,

        // Use a headless firefox browser to run the tests
        browsers: ['FirefoxHeadless'],
        customLaunchers: {
            'FirefoxHeadless': {
                base: 'Firefox',
                flags: [
                    '--headless'
                ]
            }
        }
    });
};
