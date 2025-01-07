

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

import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuthenticationInterceptor } from './auth/interceptor/authentication.interceptor';
import { ClientModule } from './client/client.module';
import { FormModule } from './form/form.module';
import { HomeModule } from './home/home.module';
import { ImportModule } from './import/import.module';
import { DefaultHeadersInterceptor } from './index/config/default-headers.interceptor';
import { ExtensionLoaderService } from './index/services/extension-loader.service';
import { ListModule } from './list/list.module';
import { LocaleModule } from './locale/locale.module';
import { LoginModule } from './login/login.module';
import { ManageModule } from './manage/manage.module';
import { NavigationModule } from './navigation/navigation.module';
import { NotificationModule } from './notification/notification.module';
import { ErrorHandlingInterceptor } from './rest/interceptor/error-handling.interceptor';
import { SettingsModule } from './settings/settings.module';

@NgModule({
    declarations: [
        AppComponent,
    ],
    imports     : [
        BrowserModule,
        CommonModule,
        LocaleModule,
        AppRoutingModule,
        HttpClientModule,
        NavigationModule,
        NotificationModule,
        FormModule,
        LoginModule,
        ListModule,
        SettingsModule,
        ManageModule,
        HomeModule,
        ClientModule,
        ImportModule
    ],
    providers   : [
        // Uses the extension loader service to load the extension and set the router config
        // before the app is initialized.
        {
            provide   : APP_INITIALIZER,
            useFactory: (extensionLoaderService: ExtensionLoaderService) =>
                () => extensionLoaderService.loadExtensionAndSetRouterConfig(),
            deps      : [ExtensionLoaderService],
            multi     : true,
        },

        { provide: HTTP_INTERCEPTORS, useClass: DefaultHeadersInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: AuthenticationInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlingInterceptor, multi: true },
    ],
    bootstrap   : [AppComponent]
})
export class AppModule {
}
