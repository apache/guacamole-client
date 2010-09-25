package net.sourceforge.guacamole.net;

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

public class Base64 {

    private static String characters = 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";


    public static String toString(byte[] data) {
        
        StringBuffer buff = new StringBuffer();
        for (int i=0; i<data.length; i+=3) {

		int a = data[i];
		int b = 0; if (i+1 < data.length) b = data[i+1];
		int c = 0; if (i+2 < data.length) c = data[i+2];

		buff.append(characters.charAt(
			(a & 0xFC) >> 2
		));

		buff.append(characters.charAt(
			((a & 0x03) << 4) |
			((b & 0xF0) >> 4)
		));

		if (i+1 < data.length) 
			buff.append(characters.charAt(
				((b & 0x0F) << 2) |
				((c & 0xC0) >> 6)
			));
		else
			buff.append('=');

		if (i+2 < data.length) 
			buff.append(characters.charAt(
				(c & 0x3F)
			));
		else
			buff.append('=');

	}

        return buff.toString();
        
    }

}
