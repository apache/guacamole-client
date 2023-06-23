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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { simulateMouseClick } from "../../../../test-utils/click-helper";
import { ElementModule } from '../../../element/element.module';
import { GuacEventService } from "../../../events/services/guac-event.service";
import { GuacEventArguments } from "../../../events/types/GuacEventArguments";
import { OskComponent } from './osk.component';
import { SimpleChange } from "@angular/core";
import {
    HttpClientTestingModule,
    HttpTestingController
} from "@angular/common/http/testing";
import { By } from "@angular/platform-browser";

describe('OskComponent', () => {
    let component: OskComponent;
    let fixture: ComponentFixture<OskComponent>;

    let httpTestingController: HttpTestingController;
    let guacEventService: GuacEventService<GuacEventArguments>;
    const layout = 'assets/de-de-qwertz.json';
    const layoutResponse = `{"language":"de_DE","type":"qwertz","width":23,"keys":{"0":[{"title":"0","requires":[]},{"title":"=","requires":["shift"]},{"title":"}","requires":["alt-gr"]}],"1":[{"title":"1","requires":[]},{"title":"!","requires":["shift"]}],"2":[{"title":"2","requires":[]},{"title":"\\"","requires":["shift"]},{"title":"²","requires":["alt-gr"]}],"3":[{"title":"3","requires":[]},{"title":"§","requires":["shift"]},{"title":"³","requires":["alt-gr"]}],"4":[{"title":"4","requires":[]},{"title":"$","requires":["shift"]}],"5":[{"title":"5","requires":[]},{"title":"%","requires":["shift"]}],"6":[{"title":"6","requires":[]},{"title":"&","requires":["shift"]}],"7":[{"title":"7","requires":[]},{"title":"/","requires":["shift"]},{"title":"{","requires":["alt-gr"]}],"8":[{"title":"8","requires":[]},{"title":"(","requires":["shift"]},{"title":"[","requires":["alt-gr"]}],"9":[{"title":"9","requires":[]},{"title":")","requires":["shift"]},{"title":"]","requires":["alt-gr"]}],"Esc":65307,"F1":65470,"F2":65471,"F3":65472,"F4":65473,"F5":65474,"F6":65475,"F7":65476,"F8":65477,"F9":65478,"F10":65479,"F11":65480,"F12":65481,"Space":" ","Back":[{"title":"⟵","keysym":65288}],"Tab":[{"title":"Tab ↹","keysym":65289}],"Enter":[{"title":"↵","keysym":65293}],"Home":[{"title":"Pos 1","keysym":65360}],"PgUp":[{"title":"Bild ↑","keysym":65365}],"PgDn":[{"title":"Bild ↓","keysym":65366}],"End":[{"title":"Ende","keysym":65367}],"Ins":[{"title":"Einfg","keysym":65379}],"Del":[{"title":"Entf","keysym":65535}],"Left":[{"title":"←","keysym":65361}],"Up":[{"title":"↑","keysym":65362}],"Right":[{"title":"→","keysym":65363}],"Down":[{"title":"↓","keysym":65364}],"Menu":[{"title":"Menu","keysym":65383}],"LShift":[{"title":"Shift","modifier":"shift","keysym":65505}],"RShift":[{"title":"Shift","modifier":"shift","keysym":65506}],"LCtrl":[{"title":"Strg","modifier":"control","keysym":65507}],"RCtrl":[{"title":"Strg","modifier":"control","keysym":65508}],"Caps":[{"title":"Caps","modifier":"caps","keysym":65509}],"LAlt":[{"title":"Alt","modifier":"alt","keysym":65513}],"AltGr":[{"title":"AltGr","modifier":"alt-gr","keysym":65027}],"Meta":[{"title":"Meta","modifier":"meta","keysym":65511}],"^":[{"title":"^","requires":[]},{"title":"°","requires":["shift"]}],"ß":[{"title":"ß","requires":[]},{"title":"?","requires":["shift"]},{"title":"\\\\","requires":["alt-gr"]}],"´":[{"title":"´","requires":[]},{"title":"\`","requires":["shift"]}],"+":[{"title":"+","requires":[]},{"title":"*","requires":["shift"]},{"title":"~","requires":["alt-gr"]}],"#":[{"title":"#","requires":[]},{"title":"'","requires":["shift"]}],"<":[{"title":"<","requires":[]},{"title":">","requires":["shift"]},{"title":"|","requires":["alt-gr"]}],",":[{"title":",","requires":[]},{"title":";","requires":["shift"]}],".":[{"title":".","requires":[]},{"title":":","requires":["shift"]}],"-":[{"title":"-","requires":[]},{"title":"_","requires":["shift"]}],"q":[{"title":"q","requires":[]},{"title":"Q","requires":["caps"]},{"title":"Q","requires":["shift"]},{"title":"q","requires":["caps","shift"]},{"title":"@","requires":["alt-gr"]}],"w":[{"title":"w","requires":[]},{"title":"W","requires":["caps"]},{"title":"W","requires":["shift"]},{"title":"w","requires":["caps","shift"]}],"e":[{"title":"e","requires":[]},{"title":"E","requires":["caps"]},{"title":"E","requires":["shift"]},{"title":"e","requires":["caps","shift"]},{"title":"€","requires":["alt-gr"]}],"r":[{"title":"r","requires":[]},{"title":"R","requires":["caps"]},{"title":"R","requires":["shift"]},{"title":"r","requires":["caps","shift"]}],"t":[{"title":"t","requires":[]},{"title":"T","requires":["caps"]},{"title":"T","requires":["shift"]},{"title":"t","requires":["caps","shift"]}],"z":[{"title":"z","requires":[]},{"title":"Z","requires":["caps"]},{"title":"Z","requires":["shift"]},{"title":"z","requires":["caps","shift"]}],"u":[{"title":"u","requires":[]},{"title":"U","requires":["caps"]},{"title":"U","requires":["shift"]},{"title":"u","requires":["caps","shift"]}],"i":[{"title":"i","requires":[]},{"title":"I","requires":["caps"]},{"title":"I","requires":["shift"]},{"title":"i","requires":["caps","shift"]}],"o":[{"title":"o","requires":[]},{"title":"O","requires":["caps"]},{"title":"O","requires":["shift"]},{"title":"o","requires":["caps","shift"]}],"p":[{"title":"p","requires":[]},{"title":"P","requires":["caps"]},{"title":"P","requires":["shift"]},{"title":"p","requires":["caps","shift"]}],"ü":[{"title":"ü","requires":[]},{"title":"Ü","requires":["caps"]},{"title":"Ü","requires":["shift"]},{"title":"ü","requires":["caps","shift"]}],"a":[{"title":"a","requires":[]},{"title":"A","requires":["caps"]},{"title":"A","requires":["shift"]},{"title":"a","requires":["caps","shift"]}],"s":[{"title":"s","requires":[]},{"title":"S","requires":["caps"]},{"title":"S","requires":["shift"]},{"title":"s","requires":["caps","shift"]}],"d":[{"title":"d","requires":[]},{"title":"D","requires":["caps"]},{"title":"D","requires":["shift"]},{"title":"d","requires":["caps","shift"]}],"f":[{"title":"f","requires":[]},{"title":"F","requires":["caps"]},{"title":"F","requires":["shift"]},{"title":"f","requires":["caps","shift"]}],"g":[{"title":"g","requires":[]},{"title":"G","requires":["caps"]},{"title":"G","requires":["shift"]},{"title":"g","requires":["caps","shift"]}],"h":[{"title":"h","requires":[]},{"title":"H","requires":["caps"]},{"title":"H","requires":["shift"]},{"title":"h","requires":["caps","shift"]}],"j":[{"title":"j","requires":[]},{"title":"J","requires":["caps"]},{"title":"J","requires":["shift"]},{"title":"j","requires":["caps","shift"]}],"k":[{"title":"k","requires":[]},{"title":"K","requires":["caps"]},{"title":"K","requires":["shift"]},{"title":"k","requires":["caps","shift"]}],"l":[{"title":"l","requires":[]},{"title":"L","requires":["caps"]},{"title":"L","requires":["shift"]},{"title":"l","requires":["caps","shift"]}],"ö":[{"title":"ö","requires":[]},{"title":"Ö","requires":["caps"]},{"title":"Ö","requires":["shift"]},{"title":"ö","requires":["caps","shift"]}],"ä":[{"title":"ä","requires":[]},{"title":"Ä","requires":["caps"]},{"title":"Ä","requires":["shift"]},{"title":"ä","requires":["caps","shift"]}],"y":[{"title":"y","requires":[]},{"title":"Y","requires":["caps"]},{"title":"Y","requires":["shift"]},{"title":"y","requires":["caps","shift"]}],"x":[{"title":"x","requires":[]},{"title":"X","requires":["caps"]},{"title":"X","requires":["shift"]},{"title":"x","requires":["caps","shift"]}],"c":[{"title":"c","requires":[]},{"title":"C","requires":["caps"]},{"title":"C","requires":["shift"]},{"title":"c","requires":["caps","shift"]}],"v":[{"title":"v","requires":[]},{"title":"V","requires":["caps"]},{"title":"V","requires":["shift"]},{"title":"v","requires":["caps","shift"]}],"b":[{"title":"b","requires":[]},{"title":"B","requires":["caps"]},{"title":"B","requires":["shift"]},{"title":"b","requires":["caps","shift"]}],"n":[{"title":"n","requires":[]},{"title":"N","requires":["caps"]},{"title":"N","requires":["shift"]},{"title":"n","requires":["caps","shift"]}],"m":[{"title":"m","requires":[]},{"title":"M","requires":["caps"]},{"title":"M","requires":["shift"]},{"title":"m","requires":["caps","shift"]},{"title":"µ","requires":["alt-gr"]}]},"layout":[["Esc",0.7,"F1","F2","F3","F4",0.7,"F5","F6","F7","F8",0.7,"F9","F10","F11","F12"],[0.1],{"main":{"alpha":[["^","1","2","3","4","5","6","7","8","9","0","ß","´","Back"],["Tab","q","w","e","r","t","z","u","i","o","p","ü","+",1,0.6],["Caps","a","s","d","f","g","h","j","k","l","ö","ä","#","Enter"],["LShift","<","y","x","c","v","b","n","m",",",".","-","RShift"],["LCtrl","Meta","LAlt","Space","AltGr","Menu","RCtrl"]],"movement":[["Ins","Home","PgUp"],["Del","End","PgDn"],[1],["Up"],["Left","Down","Right"]]}}],"keyWidths":{"Back":2,"Tab":1.5,"\\\\":1.5,"Caps":1.75,"Enter":1.25,"LShift":2,"RShift":2.1,"LCtrl":1.6,"Meta":1.6,"LAlt":1.6,"Space":6.1,"AltGr":1.6,"Menu":1.6,"RCtrl":1.6,"Ins":1.6,"Home":1.6,"PgUp":1.6,"Del":1.6,"End":1.6,"PgDn":1.6}}`;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [OskComponent],
            imports: [HttpClientTestingModule, ElementModule],
        })
            .compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        guacEventService = TestBed.inject(GuacEventService<GuacEventArguments>);

        fixture = TestBed.createComponent(OskComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('loads layout and displays osk', async () => {
        component.layout = layout;
        fixture.detectChanges();
        component.ngOnChanges({layout: new SimpleChange(undefined, layout, true)});

        const req = httpTestingController.expectOne(layout);
        expect(req.request.method).toEqual('GET');
        req.flush(JSON.parse(layoutResponse));

        expect(fixture.debugElement.query(By.css('.guac-keyboard'))).toBeTruthy();
    });

    it('outputs emit events', () => {
        spyOn(guacEventService, 'broadcast');

        component.layout = layout;
        fixture.detectChanges();
        component.ngOnChanges({layout: new SimpleChange(undefined, layout, true)});

        const req = httpTestingController.expectOne(layout);
        expect(req.request.method).toEqual('GET');
        req.flush(JSON.parse(layoutResponse));

        // click div with class 'guac-keyboard-cap' and text 'a'
        const divs = fixture.debugElement.queryAll(By.css('.guac-keyboard-cap'));
        const divA = divs.find((div) => div.nativeElement.textContent === 'a')?.nativeElement;

        simulateMouseClick(divA);

        expect(guacEventService.broadcast).toHaveBeenCalledWith('guacSyntheticKeydown', {keysym: 97});
        expect(guacEventService.broadcast).toHaveBeenCalledWith('guacSyntheticKeyup', {keysym: 97});

    });

});
