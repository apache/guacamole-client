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

const AngularTemplateCachePlugin = require('angular-templatecache-webpack5-plugin');
const CopyPlugin = require('copy-webpack-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const DependencyListPlugin = require('./plugins/dependency-list-plugin');
const HtmlPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TerserPlugin = require('terser-webpack-plugin');
const webpack = require('webpack');

module.exports = {

    bail: true,
    mode: 'production',
    stats: 'minimal',

    output: {
        path: __dirname + '/dist',
        filename: 'guacamole.[contenthash].js',
        hashFunction: 'xxhash64',
        clean: true
    },

    // Generate source maps
    devtool: 'source-map',

    // Entry point for the Guacamole webapp is the "index" AngularJS module
    entry: './src/app/index/indexModule.js',

    module: {
        rules: [

            // Automatically extract imported CSS for later reference within separate CSS file
            {
                test: /\.css$/i,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: 'css-loader',
                        options: {
                            import: false,
                            url: false
                        }
                    }
                ]
            },

            /*
             * Necessary to be able to use angular 1 with webpack as explained in https://github.com/webpack/webpack/issues/2049
             */
            {
                test: require.resolve('angular'),
                loader: 'exports-loader',
                options: {
                    type: 'commonjs',
                    exports: 'single window.angular'
                }
            }

        ]
    },
    optimization: {
        minimizer: [

            // Minify using Terser
            new TerserPlugin({
                terserOptions: {
                    ecma: 5,
                    compress: {
                        drop_console: false,
                        passes: 2
                    },
                    mangle: true,
                    format: {
                        comments: false
                    }
                },
                extractComments: false
            }),

            new CssMinimizerPlugin()

        ],
        splitChunks: {
            cacheGroups: {

                // Bundle CSS as one file
                styles: {
                    name: 'styles',
                    test: /\.css$/,
                    chunks: 'all',
                    enforce: true
                }

            }
        }
    },
    plugins: [

        new AngularTemplateCachePlugin({
            module: 'templates-main',
            outputFilename: 'templates.js',
            root: 'app/',
            source: 'src/app/**/*.html',
            standalone: true
        }),

        // Copy static files to dist/
        new CopyPlugin({
            patterns: [
                { from: 'app/**/*', context: 'src/' },
                { from: 'fonts/**/*', context: 'src/' },
                { from: 'images/**/*', context: 'src/' },
                { from: 'layouts/**/*', context: 'src/' },
                { from: 'translations/**/*', context: 'src/' },
                { from: 'verifyCachedVersion.js', context: 'src/' }
            ]
        }),

        // Copy core libraries for global inclusion
        new CopyPlugin({
            patterns: [
                { from: 'angular/angular.min.js', context: 'node_modules/' },
                { from: 'blob-polyfill/Blob.js', context: 'node_modules/' },
                { from: 'datalist-polyfill/datalist-polyfill.min.js', context: 'node_modules/' },
                { from: 'jquery/dist/jquery.min.js', context: 'node_modules/' },
                { from: 'lodash/lodash.min.js', context: 'node_modules/' }
            ]
        }),

        // Generate index.html from template
        new HtmlPlugin({
            inject: false,
            template: 'src/index.html'
        }),

        // Extract CSS from Webpack bundle as separate file
        new MiniCssExtractPlugin({
            filename: 'guacamole.[contenthash].css',
            chunkFilename: '[id].guacamole.[contenthash].css'
        }),

        // List all bundled node modules for sake of automatic LICENSE file
        // generation / sanity checks
        new DependencyListPlugin(),

        // Automatically require used modules
        new webpack.ProvidePlugin({
            jstz: 'jstz',
            Pickr: '@simonwep/pickr',
            saveAs: 'file-saver',
            'assert': 'assert',
            'process': 'process/browser'
        })

    ],
    resolve: {

        fallback: {
            'assert': 'assert/',
            'process': 'process/browser'
        },

        // Include Node modules and base source tree within search path for
        // import/resolve
        modules: [
            'src',
            'node_modules'
        ]

    }

};

