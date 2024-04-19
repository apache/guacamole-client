import { Pipe, PipeTransform } from '@angular/core';
import { SortService } from '../list/services/sort.service';

/**
 * Pipe that sorts a collection by the given predicates.
 */
@Pipe({
    name: 'orderBy',
    standalone: true
})
export class OrderByPipe implements PipeTransform {

    /**
     * Inject required service.
     */
    constructor(private sortService: SortService) {
    }

    /**
     * @see SortService.orderByPredicate
     */
    transform<T>(collection: T[] | null | undefined, ...predicates: string[]): T[] {

        return this.sortService.orderByPredicate(collection, predicates);

    }

}
