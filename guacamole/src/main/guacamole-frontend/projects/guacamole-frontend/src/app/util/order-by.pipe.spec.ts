

import { TestBed } from '@angular/core/testing';
import { SortService } from '../list/services/sort.service';
import { OrderByPipe } from './order-by.pipe';

describe('OrderByPipe', () => {
    let pipe: OrderByPipe;
    let sortService: SortService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [SortService]
        });

        sortService = TestBed.inject(SortService);
        pipe = new OrderByPipe(sortService);
    });

    it('create an instance', () => {
        expect(pipe).toBeTruthy();
    });

    it('orders by property', () => {
        const result = pipe.transform([{name: 'b'}, {name: 'a'}], ['name']);
        expect(result).toEqual([{name: 'a'}, {name: 'b'}]);
    });
});
