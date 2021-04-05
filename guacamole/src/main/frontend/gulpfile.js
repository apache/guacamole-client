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

const angularFilesort = require('gulp-angular-filesort');
const cleanCss = require('gulp-clean-css');
const concat = require('gulp-concat');
const del = require('del');
const gulp = require('gulp');
const ngHtml2Js = require("gulp-ng-html2js");
const sourcemaps = require('gulp-sourcemaps');
const webpack = require('webpack-stream');

// Clean build files
gulp.task('clean', (callback) => del([
    'dist',
    'generated'
], callback));

// Build monolithic, minified CSS source
gulp.task('build-css',
    () => gulp.src([
            'node_modules/@simonwep/pickr/dist/themes/monolith.min.css',
            'src/app/**/*.css'
        ])
        .pipe(sourcemaps.init())
        .pipe(concat('guacamole.min.css'))
        .pipe(cleanCss())
        .pipe(sourcemaps.write('./'))
        .pipe(gulp.dest('dist'))
);

// Pre-cache AngularJS templates
gulp.task('build-template-js',
    () => gulp.src('src/app/**/*.html')
        .pipe(ngHtml2Js({
            moduleName: 'templates-main',
            prefix: 'app/'
        }))
        .pipe(concat('templates.js'))
        .pipe(gulp.dest('generated'))
);

// Build monolithic combined JavaScript source containing all pre-cached
// templates and all AngularJS module declarations in the proper order
gulp.task('build-combined-js',
    () => gulp.src([
            'src/app/**/*.js',
            'generated/templates.js'
        ])
        .pipe(angularFilesort())
        .pipe(sourcemaps.init())
        .pipe(concat('guacamole.js'))
        .pipe(sourcemaps.write('./'))
        .pipe(gulp.dest('generated'))
);

// Process monolithic JavaScript source through WebPack to produce a bundle
// that contains all required dependencies
gulp.task('build-webpack-bundle',
    () => gulp.src('generated/guacamole.js')
        .pipe(webpack(require('./webpack.config.js')))
        .pipe(gulp.dest('dist'))
);

// Build all JavaScript for the entire application
gulp.task('build-js',  gulp.series(
    'build-template-js',
    'build-combined-js',
    'build-webpack-bundle'
));

// Copy plain, static contents of application
gulp.task('copy-static',
    () => gulp.src([
        'src/relocateParameters.js',
        'src/index.html',
        'src/fonts/**/*',
        'src/images/**/*',
        'src/layouts/**/*',
        'src/translations/**/*'
    ], { base: './src' })
    .pipe(gulp.dest('dist'))
);

gulp.task('default', gulp.series(
    'clean',
    gulp.parallel('build-css', 'build-js', 'copy-static')
));

