import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@ngneat/transloco';
import { ElementModule } from 'guacamole-frontend-lib';
import { ListModule } from '../list/list.module';
import { NavigationModule } from '../navigation/navigation.module';
import {
    ConnectionImportErrorsComponent
} from './components/connection-import-errors/connection-import-errors.component';
import {
    ConnectionImportFileHelpComponent
} from './components/connection-import-file-help/connection-import-file-help.component';
import { ImportConnectionsComponent } from './components/import-connections/import-connections.component';

/**
 * The module for code supporting importing user-supplied files. Currently, only
 * connection import is supported.
 */
@NgModule({
    declarations: [
        ImportConnectionsComponent,
        ConnectionImportFileHelpComponent,
        ConnectionImportErrorsComponent
    ],
    imports     : [
        CommonModule,
        TranslocoModule,
        RouterLink,
        ElementModule,
        NavigationModule,
        FormsModule,
        ListModule
    ],
    exports     : [
        ImportConnectionsComponent,
        ConnectionImportFileHelpComponent
    ]
})
export class ImportModule {
}
