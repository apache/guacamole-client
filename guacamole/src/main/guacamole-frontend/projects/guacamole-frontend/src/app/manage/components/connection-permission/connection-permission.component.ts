import { Component, Input, ViewEncapsulation } from '@angular/core';
import { GroupListItem } from "../../../group-list/types/GroupListItem";
import { ConnectionListContext } from "../../types/ConnectionListContext";

/**
 * A component which displays a single connection entry and allows
 * manipulation of the connection permissions.
 */
@Component({
  selector: 'guac-connection-permission',
  templateUrl: './connection-permission.component.html',
  encapsulation: ViewEncapsulation.None
})
export class ConnectionPermissionComponent {

    /**
     * TODO
     */
    @Input({required: true}) context!: ConnectionListContext;

    /**
     * TODO
     */
    @Input({required: true}) item!: GroupListItem;
}
