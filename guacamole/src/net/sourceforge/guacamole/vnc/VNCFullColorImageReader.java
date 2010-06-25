package net.sourceforge.guacamole.vnc;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
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

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.DataInputStream;
import java.io.IOException;


public class VNCFullColorImageReader extends VNCImageReader {

    private int bpp;
    private int depth;

    private int redBits;
    private int greenBits;
    private int blueBits;

    private int redMax;
    private int greenMax;
    private int blueMax;

    private int redShift;
    private int greenShift;
    private int blueShift;

    private boolean readAsIndexed;

    public boolean isBigEndian() {
        return true;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public int getDepth() {
        return depth;
    }

    public int getRedMax() {
        return redMax;
    }

    public int getGreenMax() {
        return greenMax;
    }

    public int getBlueMax() {
        return blueMax;
    }

    public int getRedShift() {
        return redShift;
    }

    public int getGreenShift() {
        return greenShift;
    }

    public int getBlueShift() {
        return blueShift;
    }

    // Set up BGR reader
    public VNCFullColorImageReader(int redBits, int greenBits, int blueBits, int outputBPP) throws VNCException {
        
        depth = redBits + greenBits + blueBits;

        if (depth > 0 && depth <= 8)
            bpp = 8;
        else if (depth <= 16)
            bpp = 16;
        else if (depth <= 32)
            bpp = 32;
        else
            throw new VNCException("Illegal bit depth for VNC images: " + bpp);

        this.redBits = redBits;
        this.greenBits = greenBits;
        this.blueBits = blueBits;

        redMax   = (1 << redBits)   - 1;
        greenMax = (1 << greenBits) - 1;
        blueMax  = (1 << blueBits)  - 1;

        redShift   = greenBits + blueBits;
        greenShift = blueBits;
        blueShift  = 0;

        if (outputBPP == 8)
            this.readAsIndexed = true;
        else if (outputBPP == 24)
            this.readAsIndexed = false;
        else
            throw new VNCException("Only 8-bit or 24-bit output is supported.");

    }

    @Override
    public int readCPixel(DataInputStream input) throws IOException {

        if (redBits != 8 || greenBits != 8 || blueBits != 8)
            return readPixel(input);

        int red   = input.read();
        int green = input.read();
        int blue  = input.read();

        int color = (red << 16) | (green << 8) | blue;
        return color;
    }

    @Override
    public int readPixel(DataInputStream input) throws IOException {
        int value;
        switch (bpp) {
            case 8:
                value = input.read();
                break;
            case 16:
                value = input.readShort();
                break;
            case 32:
                value = input.readInt();
                break;
            default:
                throw new IOException("Invalid BPP.");
        }

        int red   = (value >> redShift)   & redMax;
        int green = (value >> greenShift) & greenMax;
        int blue  = (value >> blueShift)  & blueMax;

        red   <<= 8 - redBits;
        green <<= 8 - greenBits;
        blue  <<= 8 - blueBits;

        int color = (red << 16) | (green << 8) | blue;
        return color;
    }

    @Override
    public BufferedImage generateBlankImage(int width, int height) {
        if (readAsIndexed)
            return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, COLOR_MODEL);
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    // Load color model
    public static final IndexColorModel COLOR_MODEL;
    static {
        // Construct color model
        byte[] colorShade = {0, (byte) 51, (byte) 104, (byte) 153, (byte) 204, (byte) 255};
        byte[] greyShade = new byte[39];
        for (int i=1; i<40; i++)
            greyShade[i-1] = (byte) (6*i);

        byte[] red = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];
        byte[] green = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];
        byte[] blue = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];
        byte[] alpha = new byte[colorShade.length*colorShade.length*colorShade.length+greyShade.length+1];

        int color = 0;
        for (int r=0; r<colorShade.length; r++) {
            for (int g=0; g<colorShade.length; g++) {
                for (int b=0; b<colorShade.length; b++) {
                    red[color] = colorShade[r];
                    green[color] = colorShade[g];
                    blue[color] = colorShade[b];
                    alpha[color] = (byte) 255;
                    color++;
                }
            }
        }

        for (int grey=0; grey<greyShade.length; grey++) {
            red[color] = greyShade[grey];
            green[color] = greyShade[grey];
            blue[color] = greyShade[grey];
            alpha[color] = (byte) 255;
            color++;
        }

        red[color] = 0;
        green[color] = 0;
        blue[color] = 0;
        alpha[color] = (byte) 0;

        COLOR_MODEL = new IndexColorModel(8, 256, red, green, blue, alpha);
    }

}

