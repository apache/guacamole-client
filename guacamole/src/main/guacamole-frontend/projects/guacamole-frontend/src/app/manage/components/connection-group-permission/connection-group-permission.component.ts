import { Component, Input, ViewEncapsulation } from '@angular/core';
import { ConnectionListContext } from "../../types/ConnectionListContext";
import { GroupListItem } from "../../../group-list/types/GroupListItem";

/**
 * A component which displays a single connection group and allows
 * manipulation of the connection group permissions.
 */
@Component({
    selector: 'guac-connection-group-permission',
    templateUrl: './connection-group-permission.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ConnectionGroupPermissionComponent {

    /**
     * TODO
     */
    @Input({required: true}) context!: ConnectionListContext;

    /**
     * TODO
     */
    @Input({required: true}) item!: GroupListItem;

}
