
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


// Keymap

var unshiftedKeySym = new Array();
unshiftedKeySym[8]   = 0xFF08; // backspace
unshiftedKeySym[9]   = 0xFF09; // tab
unshiftedKeySym[13]  = 0xFF0D; // enter
unshiftedKeySym[16]  = 0xFFE1; // shift
unshiftedKeySym[17]  = 0xFFE3; // ctrl
unshiftedKeySym[18]  = 0xFFE9; // alt
unshiftedKeySym[19]  = 0xFF13; // pause/break
unshiftedKeySym[20]  = 0xFFE5; // caps lock
unshiftedKeySym[27]  = 0xFF1B; // escape
unshiftedKeySym[33]  = 0xFF55; // page up
unshiftedKeySym[34]  = 0xFF56; // page down
unshiftedKeySym[35]  = 0xFF57; // end
unshiftedKeySym[36]  = 0xFF50; // home
unshiftedKeySym[37]  = 0xFF51; // left arrow
unshiftedKeySym[38]  = 0xFF52; // up arrow
unshiftedKeySym[39]  = 0xFF53; // right arrow
unshiftedKeySym[40]  = 0xFF54; // down arrow
unshiftedKeySym[45]  = 0xFF63; // insert
unshiftedKeySym[46]  = 0xFFFF; // delete
unshiftedKeySym[91]  = 0xFFEB; // left window key (super_l)
unshiftedKeySym[92]  = 0xFF67; // right window key (menu key?)
unshiftedKeySym[93]  = null; // select key
unshiftedKeySym[112] = 0xFFBE; // f1
unshiftedKeySym[113] = 0xFFBF; // f2
unshiftedKeySym[114] = 0xFFC0; // f3
unshiftedKeySym[115] = 0xFFC1; // f4
unshiftedKeySym[116] = 0xFFC2; // f5
unshiftedKeySym[117] = 0xFFC3; // f6
unshiftedKeySym[118] = 0xFFC4; // f7
unshiftedKeySym[119] = 0xFFC5; // f8
unshiftedKeySym[120] = 0xFFC6; // f9
unshiftedKeySym[121] = 0xFFC7; // f10
unshiftedKeySym[122] = 0xFFC8; // f11
unshiftedKeySym[123] = 0xFFC9; // f12
unshiftedKeySym[144] = 0xFF7F; // num lock
unshiftedKeySym[145] = 0xFF14; // scroll lock

// Shifted versions, IF DIFFERENT FROM UNSHIFTED!
// If any of these are null, the unshifted one will be used.
var shiftedKeySym  = new Array();
shiftedKeySym[18]  = 0xFFE7; // alt

// Constants for keysyms for special keys
var KEYSYM_CTRL = 65507;
var KEYSYM_ALT = 65513;
var KEYSYM_DELETE = 65535;
var KEYSYM_SHIFT = 65505;


