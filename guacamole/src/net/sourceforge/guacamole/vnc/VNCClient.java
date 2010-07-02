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

import java.awt.image.WritableRaster;
import net.sourceforge.guacamole.instruction.framebuffer.PNGInstruction;
import net.sourceforge.guacamole.instruction.framebuffer.CursorInstruction;
import net.sourceforge.guacamole.instruction.framebuffer.CopyRectInstruction;
import net.sourceforge.guacamole.event.PointerEvent;
import net.sourceforge.guacamole.event.EventQueue;
import net.sourceforge.guacamole.event.KeyEvent;
import net.sourceforge.guacamole.event.EventHandler;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import javax.crypto.Cipher;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.BadPaddingException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;
import net.sourceforge.guacamole.Client;

import net.sourceforge.guacamole.instruction.ClipboardInstruction;
import net.sourceforge.guacamole.instruction.Instruction;
import net.sourceforge.guacamole.instruction.NameInstruction;
import net.sourceforge.guacamole.instruction.SizeInstruction;
import net.sourceforge.guacamole.instruction.framebuffer.PNGImage;
import net.sourceforge.guacamole.GuacamoleException;

public class VNCClient extends Client {

    private static final int SECURITY_TYPE_INVALID = 0;
    private static final int SECURITY_TYPE_NONE = 1;
    private static final int SECURITY_TYPE_VNC_AUTHENTICATION = 2;

    private static final int SECURITY_RESULT_OK = 0;
    private static final int SECURITY_RESULT_FAILED = 1;

    private static final int MESSAGE_SET_PIXEL_FORMAT = 0;
    private static final int MESSAGE_SET_ENCODINGS = 2;
    private static final int MESSAGE_FRAMEBUFFER_UPDATE_REQUEST = 3;
    private static final int MESSAGE_KEY_EVENT = 4;
    private static final int MESSAGE_POINTER_EVENT = 5;
    private static final int MESSAGE_CLIENT_CUT_TEXT = 6;

    private static final int MESSAGE_FRAMEBUFFER_UPDATE = 0;
    private static final int MESSAGE_SET_COLORMAP_ENTRIES = 1;
    private static final int MESSAGE_BELL = 2;
    private static final int MESSAGE_SERVER_CUT_TEXT = 3;

    private static final int HEXTILE_FLAG_RAW = 1;
    private static final int HEXTILE_FLAG_BACKGROUND_SPECIFIED = 2;
    private static final int HEXTILE_FLAG_FOREGROUND_SPECIFIED = 4;
    private static final int HEXTILE_FLAG_ANY_SUBRECTS = 8;
    private static final int HEXTILE_FLAG_SUBRECTS_COLORED = 16;

    private static final int ENCODING_RAW = 0;
    private static final int ENCODING_COPYRECT = 1;
    private static final int ENCODING_RRE = 2;
    private static final int ENCODING_HEXTILE = 5;
    private static final int ENCODING_ZRLE = 16;
    private static final int ENCODING_CURSOR = -239;

    private final Socket sock;
    private final DataInputStream input;
    private final DataOutputStream output;

    private int frameBufferWidth;
    private int frameBufferHeight;
    private String name;

    private boolean needRefresh = true;
    private VNCImageReader rawReader = null;

    private InputOutputStream toZlib;
    private DataInputStream fromZlib;

    public int getFrameBufferHeight() {
        return frameBufferHeight;
    }

    public int getFrameBufferWidth() {
        return frameBufferWidth;
    }

    public String getName() {
        return name;
    }

    public byte reverse(byte b) {
       
        int input = b & 0xFF;
        int output = 0;

        for (int i=0; i<8; i++) {

            output <<= 1;

            if ((input & 0x01) != 0)
                output |= 0x01;

            input >>= 1;

        }

        return (byte) output;
    }

    // Generates VNC key from string
    private byte[] generateVNCAuthKey(String password) throws VNCException {

        try {
            // Get password bytes
            byte[] passwordBytes = password.getBytes("iso-8859-1");
            if (passwordBytes.length > 8)
                throw new VNCException("Password must be 8 characters (bytes) or less.");

            // Reverse bit order of all bytes in array
            for (int i=0; i<passwordBytes.length; i++)
                passwordBytes[i] = reverse(passwordBytes[i]);

            // Get null-padded byte array
            byte[] key = new byte[8];
            System.arraycopy(passwordBytes, 0, key, 0, passwordBytes.length);

            return key;
        }
        catch (UnsupportedEncodingException e) {
            throw new VNCException("ISO-8859-1 not supported by Java. Cannot generate VNC authentication key.", e);
        }

    }

    // password = null for no authentication
    public VNCClient(String host, int port, String password, int colorBits, int outputBPP, boolean swapRedAndBlue)
            throws VNCException {

        try {
            // Connect
            sock = new Socket();
            sock.connect(
                    new InetSocketAddress(
                        host,
                        port
                    )
            );

            input = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
            output = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));

            // Read protocol version
            byte[] protocolVersionBytes = new byte[12];
            input.readFully(protocolVersionBytes);
            String protocolVersion = new String(protocolVersionBytes, "iso-8859-1");

            // Write own version (3.3 = simpler)
            if (protocolVersion.startsWith("RFB ")) {
                log("Received RFB version... Sending own...");
                output.writeBytes("RFB 003.003\n");
                output.flush();
            }
            else
                throw new VNCException("Server did not send RFB version handshake. Likely not a VNC/RFB service.");

            // Read security type message
            log("Reading security type...");
            int securityType = input.readInt();
            switch (securityType) {

                case SECURITY_TYPE_INVALID:

                    // Read failure reason string
                    int length = input.readInt();
                    byte[] reason = new byte[length];
                    input.readFully(reason);
                    String reasonString = new String(reason, "iso-8859-1");

                    throw new VNCException("Connection failed during security negotiation. VNC server says: \"" + reasonString + "\".");

                case SECURITY_TYPE_NONE:
                    // Security type NONE: do nothing.
                    if (password != null)
                        throw new VNCException("Password specified, but VNC server security type is NONE (no password).");
                    break;

                case SECURITY_TYPE_VNC_AUTHENTICATION:

                    if (password == null)
                        throw new VNCException("Password not specified, but VNC server security type is VNC Authentication (password required).");

                    // Read challenge
                    log("Reading challenge...");
                    byte[] challenge = new byte[16];
                    input.readFully(challenge);

                    // Encrypt and write challenge
                    try {

                        log("Initializing cipher...");

                        // Construct 8 byte key
                        byte[] key = generateVNCAuthKey(password);

                        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding"); // VNC Auth uses DES
                        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
                        cipher.init(
                                Cipher.ENCRYPT_MODE,
                                keyFactory.generateSecret(new DESKeySpec(key))
                        );


                        log("Sending encrypted challenge...");

                        byte[] encrypted = cipher.doFinal(challenge);
                        output.write(encrypted);
                        output.flush();

                        log("Reading security result...");
                        int result = input.readInt();
                        switch (result) {
                            case SECURITY_RESULT_OK:
                                log("Success");
                                break;
                            case SECURITY_RESULT_FAILED:
                                throw new VNCException("VNC authentication failed.");
                        }

                    }
                    catch (NoSuchAlgorithmException e) {
                        throw new VNCException("DES not supported by Java. Cannot use VNC authentication.", e);
                    }
                    catch (NoSuchPaddingException e) {
                        throw new VNCException("Wrong DES padding. Cannot use VNC authentication.", e);
                    }
                    catch (BadPaddingException e) {
                        throw new VNCException("Bad DES padding. Cannot use VNC authentication.", e);
                    }
                    catch (IllegalBlockSizeException e) {
                        throw new VNCException("Illegal DES block size. Cannot use VNC authentication.", e);
                    }
                    catch (InvalidKeyException e) {
                        throw new VNCException("Invalid key. Cannot use VNC authentication.", e);
                    }
                    catch (InvalidKeySpecException e) {
                        throw new VNCException("Invalid key spec. Cannot use VNC authentication.", e);
                    }

                    break;

                default:
                    throw new VNCException("Server did not return a valid security type. Server returned:  " + securityType);
            }

            // Send ClientInit message
            log("Requesting shared desktop...");
            output.writeByte(1); // Allow shared desktops
            output.flush();

            // Read ServerInit message
            log("Reading ServerInit...");
            frameBufferWidth = input.readUnsignedShort();
            frameBufferHeight = input.readUnsignedShort();

            // Pixel format
            int bpp = input.read();
            int depth = input.read();
            boolean bigEndian = input.readBoolean();
            boolean trueColor = input.readBoolean();
            int redMax = input.readUnsignedShort();
            int greenMax = input.readUnsignedShort();
            int blueMax = input.readUnsignedShort();
            int redShift = input.read();
            int greenShift = input.read();
            int blueShift = input.read();
            byte[] padding = new byte[3];
            input.readFully(padding);

            int nameLength = input.readInt();
            byte[] nameBytes = new byte[nameLength];
            input.readFully(nameBytes);
            name = new String(nameBytes);

            // Initialization success!
            log("** Success **");
            log("frameBufferWidth=" + frameBufferWidth);
            log("frameBufferHeight=" + frameBufferHeight);
            log("name=" + name);

            instructions.addLast(new NameInstruction(name));
            instructions.addLast(new SizeInstruction(frameBufferWidth, frameBufferHeight));

            // Set pixel format
            VNCFullColorImageReader fullColorReader;
            if (colorBits == 8)
                fullColorReader = new VNCFullColorImageReader(bigEndian, 3, 3, 2, 8, swapRedAndBlue);
            else if (colorBits == 16)
                fullColorReader = new VNCFullColorImageReader(bigEndian, 5, 6, 5, outputBPP, swapRedAndBlue);
            else if (colorBits == 24)
                fullColorReader = new VNCFullColorImageReader(bigEndian, 8, 8, 8, outputBPP, swapRedAndBlue);
            else
                throw new VNCException("Color depth " + colorBits + " not supported. Only color depths of 8, 16, or 24 are allowed.");

            setPixelFormat(
                    fullColorReader.getBitsPerPixel(),
                    fullColorReader.getDepth(),
                    fullColorReader.isBigEndian(),
                    true, // True color
                    fullColorReader.getRedMax(),
                    fullColorReader.getGreenMax(),
                    fullColorReader.getBlueMax(),
                    fullColorReader.getRedShift(),
                    fullColorReader.getGreenShift(),
                    fullColorReader.getBlueShift()
            );
            rawReader = fullColorReader;

            // Supported encodings.
            sendEncodings(ENCODING_COPYRECT, ENCODING_ZRLE, ENCODING_HEXTILE, ENCODING_RRE, ENCODING_RAW, ENCODING_CURSOR);

            // Set up ZLIB stream (for ZRLE encoding)
            toZlib = new InputOutputStream();
            fromZlib = new DataInputStream(new BufferedInputStream(new InflaterInputStream(toZlib)));
        }
        catch (SocketTimeoutException e) {
            throw new VNCException("VNC connection timed out.", e);
        }
        catch (UnknownHostException e) {
            throw new VNCException("Unknown host.", e);
        }
        catch (IOException e) {
            throw new VNCException("Could not write to / read from socket (network error).", e);
        }

    }


    // Handles all messages, blocking until at least one message is read if desired.
    private void handleMessages(boolean blocking) throws IOException, VNCException, GuacamoleException {

        synchronized (input) {

            // While we have no messages, and we are either set for blocking, or input is available
            boolean handledMessages = false;
            while ((!handledMessages && blocking) || input.available() != 0) {
                try {

                    int messageType = input.read();

                    if (messageType == -1)
                        throw new VNCException("Connection to VNC server closed (end of stream encountered).");


                    switch (messageType) {
                        case MESSAGE_FRAMEBUFFER_UPDATE:
                            handleFramebufferUpdate();
                            break;

                        case MESSAGE_SET_COLORMAP_ENTRIES:
                            handleSetColormapEntries();
                            break;

                        case MESSAGE_BELL:
                            handleBell();
                            break;

                        case MESSAGE_SERVER_CUT_TEXT:
                            handleServerCutText();
                            break;
                    }

                    handledMessages = true;
                }
                catch (IOException e) {
                    throw new VNCException("Could not read messages from socket (network error).", e);
                }
            }

        }

    }

    private void fillRect(BufferedImage image, int sx, int sy, int w, int h, int color) {

        color |= 0xFF000000;

        int ex = sx+w;
        int ey = sy+h;

        for (int y = sy; y < ey; y++) {
            for (int x = sx; x < ex; x++) {
                image.setRGB(x, y, color);
            }
        }

    }

    private void handleFramebufferUpdate() throws IOException, VNCException, GuacamoleException {

        input.read(); // Padding
        int numberOfRectangles = input.readUnsignedShort();

        // Construct rectangular updates for all rectangles
        for (int i=0; i<numberOfRectangles; i++) {

            // Read image metadata
            int dstX = input.readUnsignedShort();
            int dstY = input.readUnsignedShort();
            int width = input.readUnsignedShort();
            int height = input.readUnsignedShort();
            int type = input.readInt();

            if (type == ENCODING_RAW) {

                // Read update image
                if (width > 0 && height > 0) {
                    BufferedImage image = rawReader.readImage(input, width, height);

                    // Construct FramebufferUpdate
                    PNGInstruction update = new PNGInstruction(dstX, dstY, new PNGImage(image));
                    instructions.addLast(update);

                    // If full-screen refresh, reset refresh flag.
                    if (dstX == 0 && dstY == 0 && width == frameBufferWidth && height == frameBufferHeight)
                        needRefresh = false;
                }
            }
            else if (type == ENCODING_COPYRECT) {

                // Read CopyRect encoding
                int srcX = input.readUnsignedShort();
                int srcY = input.readUnsignedShort();

                // Construct FramebufferUpdate
                
                if (width > 0 && height > 0) {
                    CopyRectInstruction update = new CopyRectInstruction(dstX, dstY, width, height, srcX, srcY);
                    instructions.addLast(update);
                }
            }
            else if (type == ENCODING_RRE) {

                int numSubRects = input.readInt();
                int background = rawReader.readPixel(input);

                BufferedImage image = null; 

                if (width > 0 && height > 0) {
                    image = rawReader.generateBlankImage(width, height);
                }

                //instructions.addLast(new DrawRectInstruction(dstX, dstY, width, height, background));
                if (image != null) {
                    fillRect(image, 0, 0, width, height, background);
                }

                for (int j=0; j<numSubRects; j++) {

                    int color = rawReader.readPixel(input);
                    int offX = input.readUnsignedShort();
                    int offY = input.readUnsignedShort();
                    int rectW = input.readUnsignedShort();
                    int rectH = input.readUnsignedShort();

                    //instructions.addLast(new DrawRectInstruction(dstX+offX, dstY+offY, rectW, rectH, color));
                    if (image != null) {
                        if (rectW == 1 && rectH == 1)
                            image.setRGB(offX, offY, 0xFF000000 | color);
                        else {
                            fillRect(image, offX, offY, rectW, rectH, color);
                        }
                    }
                }

                // Send as png instruction (rects are too inefficient)
                if (image != null) {
                    PNGInstruction update = new PNGInstruction(dstX, dstY, new PNGImage(image));
                    instructions.addLast(update);

                    // If full-screen refresh, reset refresh flag.
                    if (dstX == 0 && dstY == 0 && width == frameBufferWidth && height == frameBufferHeight)
                        needRefresh = false;
                }
            }
            else if (type == ENCODING_HEXTILE) {


                if (width > 0 && height > 0) {
                    BufferedImage image = rawReader.generateBlankImage(width, height);
                    WritableRaster raster = image.getWritableTile(0, 0);

                    int backgroundColor = 0;
                    int foregroundColor = 0;

                    // For all 16x16 tiles in left-to-right, top-to-bottom order
                    for (int tileOffsetY = 0; tileOffsetY < height; tileOffsetY += 16) {

                        int tileHeight = Math.min(16, height - tileOffsetY);

                        for (int tileOffsetX = 0; tileOffsetX < width; tileOffsetX += 16) {

                            int tileWidth = Math.min(16, width - tileOffsetX);

                            int flags = input.read();

                            // If RAW flag is set, other flags are irrelevant.
                            if ((flags & HEXTILE_FLAG_RAW) != 0) {

                                // Read and draw raw tile
                                BufferedImage tile = rawReader.readImage(input, tileWidth, tileHeight);
                                raster.setRect(tileOffsetX, tileOffsetY, tile.getData());

                            }

                            // RAW = 0
                            else {

                                // If background specified, read pixel value
                                if ((flags & HEXTILE_FLAG_BACKGROUND_SPECIFIED) != 0)
                                    backgroundColor = rawReader.readPixel(input);

                                // Draw background
                                fillRect(image, tileOffsetX, tileOffsetY, tileWidth, tileHeight, backgroundColor);

                                // If foreground specified, read pixel value
                                if ((flags & HEXTILE_FLAG_FOREGROUND_SPECIFIED) != 0)
                                    foregroundColor = rawReader.readPixel(input);

                                // If subrects present, read subrects
                                if ((flags & HEXTILE_FLAG_ANY_SUBRECTS) != 0) {

                                    // Read number of subrects, determine whether they are colored
                                    int numSubRects = input.read();
                                    boolean colored = (flags & HEXTILE_FLAG_SUBRECTS_COLORED) != 0;

                                    int color = foregroundColor;
                                    for (int j=0; j<numSubRects; j++) {

                                        // Read color (if colored), otherwise foregroundColor is used.
                                        if (colored) color = rawReader.readPixel(input);

                                        // Read position
                                        int position = input.read();
                                        int x = position >> 4;
                                        int y = position & 0x0F;

                                        // Read dimensions
                                        int dimensions = input.read();
                                        int w = (dimensions >> 4) + 1;
                                        int h = (dimensions & 0x0F) + 1;

                                        fillRect(image, tileOffsetX+x, tileOffsetY+y, w, h, color);
                                    }

                                }

                            } // end if not raw

                        }
                    }

                    // Send as png instruction (rects are too inefficient)
                    PNGInstruction update = new PNGInstruction(dstX, dstY, new PNGImage(image));
                    instructions.addLast(update);

                    // If full-screen refresh, reset refresh flag.
                    if (dstX == 0 && dstY == 0 && width == frameBufferWidth && height == frameBufferHeight)
                        needRefresh = false;
                }

            }
            else if (type == ENCODING_ZRLE) {

                // Read ZLIB data
                int length = input.readInt();
                byte[] zlibData = new byte[length];
                input.readFully(zlibData);

                // Write data to ZLIB stream
                toZlib.write(zlibData);


                if (width > 0 && height > 0) {
                    BufferedImage image = rawReader.generateBlankImage(width, height);
                    WritableRaster raster = image.getWritableTile(0, 0);
                
                    // For all 64x64 tiles in left-to-right, top-to-bottom order
                    for (int tileOffsetY = 0; tileOffsetY < height; tileOffsetY += 64) {

                        int tileHeight = Math.min(64, height - tileOffsetY);

                        for (int tileOffsetX = 0; tileOffsetX < width; tileOffsetX += 64) {

                            int tileWidth = Math.min(64, width - tileOffsetX);

                            // Get subencoding type (RLE flag + palette size)
                            int subencodingType = fromZlib.read();

                            // If RAW, just read raw image
                            if (subencodingType == 0) {

                                // Read and draw raw tile
                                if (image != null) {
                                    BufferedImage tile = rawReader.readCImage(fromZlib, tileWidth, tileHeight);
                                    raster.setRect(tileOffsetX, tileOffsetY, tile.getData());
                                }

                            }

                            // If single color...
                            else if (subencodingType == 1) {

                                // Read color
                                int color = rawReader.readCPixel(fromZlib);

                                // Draw solid rectangle
                                if (image != null) {
                                    fillRect(image, tileOffsetX, tileOffsetY, tileWidth, tileHeight, color);
                                }

                            }

                            // Packed palette
                            else if (subencodingType >= 2 && subencodingType <= 16) {

                                int paletteSize = subencodingType;
                                int[] palette = new int[paletteSize];

                                // Read palette
                                for (int j=0; j<paletteSize; j++)
                                    palette[j] = rawReader.readCPixel(fromZlib);

                                // Calculate index size
                                int indexBits;
                                int mask;

                                if (paletteSize == 2) {
                                    indexBits = 1;
                                    mask = 0x80;
                                }
                                else if (paletteSize <= 4) {
                                    indexBits = 2;
                                    mask = 0xC0;
                                }
                                else {
                                    indexBits = 4;
                                    mask = 0xF0;
                                }

                                for (int y=0; y<tileHeight; y++) {

                                    // Packing only occurs per-row
                                    int bitsAvailable = 0;
                                    int buffer = 0;

                                    for (int x=0; x<tileWidth; x++) {

                                        // Buffer more bits if necessary
                                        if (bitsAvailable == 0) {
                                            buffer = fromZlib.read();
                                            bitsAvailable = 8;
                                        }

                                        // Read next pixel
                                        int index = (buffer & mask) >> (8 - indexBits);
                                        buffer <<= indexBits;
                                        bitsAvailable -= indexBits;

                                        // Write pixel to image
                                        image.setRGB(tileOffsetX+x, tileOffsetY+y, 0xFF000000 | palette[index]);
                                        
                                    }
                                }

                            }

                            // Plain RLE
                            else if (subencodingType == 128) {

                                int color = -1;
                                int runRemaining= 0;

                                for (int y=0; y<tileHeight; y++) {
                                    for (int x=0; x<tileWidth; x++) {

                                        if (runRemaining == 0) {
                                            
                                            // Read length and color
                                            color = rawReader.readCPixel(fromZlib);

                                            runRemaining = 1;

                                            int mod;
                                            do {
                                                mod = fromZlib.read();
                                                runRemaining += mod;
                                            } while (mod == 255);

                                        }


                                        // Write pixel to image
                                        image.setRGB(tileOffsetX+x, tileOffsetY+y, 0xFF000000 | color);
                                        runRemaining--;

                                    }
                                }

                            }

                            // Palette RLE
                            else if (subencodingType >= 130 && subencodingType <= 255) {

                                int paletteSize = subencodingType - 128;
                                int[] palette = new int[paletteSize];

                                // Read palette
                                for (int j=0; j<paletteSize; j++)
                                    palette[j] = rawReader.readCPixel(fromZlib);

                                int index = -1;
                                int runRemaining= 0;

                                for (int y=0; y<tileHeight; y++) {
                                    for (int x=0; x<tileWidth; x++) {

                                        if (runRemaining == 0) {

                                            // Read length and index
                                            index = fromZlib.read();

                                            runRemaining = 1;

                                            // Run is represented by index | 0x80
                                            // Otherwise, single pixel
                                            if ((index & 0x80) != 0) {

                                                index -= 128;

                                                int mod;
                                                do {
                                                    mod = fromZlib.read();
                                                    runRemaining += mod;
                                                } while (mod == 255);

                                            }


                                        }


                                        // Write pixel to image
                                        image.setRGB(tileOffsetX+x, tileOffsetY+y, 0xFF000000 | palette[index]);
                                        runRemaining--;

                                    }
                                }

                            }
                            else
                                throw new VNCException("Invalid ZRLE subencoding type: " + subencodingType);

                        }
                    }

                    // Send as png instruction (rects are too inefficient)
                    PNGInstruction update = new PNGInstruction(dstX, dstY, new PNGImage(image));
                    instructions.addLast(update);

                    // If full-screen refresh, reset refresh flag.
                    if (dstX == 0 && dstY == 0 && width == frameBufferWidth && height == frameBufferHeight)
                        needRefresh = false;

                }

            }
            else if (type == ENCODING_CURSOR) {

                // Construct FramebufferUpdate
                if (width > 0 && height > 0) {
                    BufferedImage image = rawReader.readImage(input, width, height);

                    // Read cursor mask
                    for (int y=0; y<height; y++) {

                        int readByte = input.read();
                        int bits_available = 8;
                        for (int x=0; x<width; x++) {

                            if (bits_available == 0) {
                                readByte = input.read();
                                bits_available = 8;
                            }

                            // Set transparent if masked
                            boolean bit = (readByte & 0x80) != 0;
                            if (!bit) image.setRGB(x, y, 0);

                            readByte <<= 1;
                            bits_available--;

                        }
                    }


                    // Construct FramebufferUpdate
                    CursorInstruction update = new CursorInstruction(dstX, dstY, new PNGImage(image));
                    instructions.addLast(update);
                }

            }
            else
                throw new IOException("Unsupported encoding: " + type);

        }

    }

    private void handleSetColormapEntries() throws IOException, VNCException {

        input.read(); // Padding
        int firstColor = input.readUnsignedShort();
        int numberOfColors = input.readUnsignedShort();

        // Read palette
        byte[] reds = new byte[numberOfColors];
        byte[] greens = new byte[numberOfColors];
        byte[] blues = new byte[numberOfColors];
        byte[] alphas = new byte[numberOfColors];

        for (int i=0; i<numberOfColors; i++) {
            int red = input.readUnsignedShort()   >> 8;
            int green = input.readUnsignedShort() >> 8;
            int blue = input.readUnsignedShort()  >> 8;

            reds[i] = (byte) red;
            greens[i] = (byte) green;
            blues[i] = (byte) blue;
            alphas[i] = (byte) 0xFF;
        }

        // Set reader
        rawReader = new VNCIndexedImageReader(reds, greens, blues);
    }

    private void handleBell() {
        // Do nothing, currently
        // This message has no data, so no need to dummy-read.
        log("BELL!");
    }

    private void handleServerCutText() throws IOException {

        byte[] padding = new byte[3];
        input.readFully(padding);

        int length = input.readInt();
        byte[] textBytes = new byte[length];
        input.readFully(textBytes);

        String clipboard = new String(textBytes, "UTF-8");
        instructions.addLast(new ClipboardInstruction(clipboard));
    }
        
    private void setPixelFormat(int bitsPerPixel, int depth,
            boolean bigEndian, boolean trueColor,
            int redMax, int greenMax, int blueMax,
            int redShift, int greenShift, int blueShift)
    throws IOException {

        synchronized (output) {
            output.writeByte(MESSAGE_SET_PIXEL_FORMAT);
            output.writeBytes("   "); // Padding
            output.writeByte(bitsPerPixel);
            output.writeByte(depth);
            output.writeBoolean(bigEndian);
            output.writeBoolean(trueColor);
            output.writeShort(redMax);
            output.writeShort(greenMax);
            output.writeShort(blueMax);
            output.writeByte(redShift);
            output.writeByte(greenShift);
            output.writeByte(blueShift);
            output.writeBytes("   "); // Padding
            output.flush();
        }
        
    }

    // Last is most recent message.
    private final Object instructionLock = new Object();
    private LinkedList<Instruction> instructions = new LinkedList<Instruction>();

    @Override
    public void setClipboard(String clipboard) throws GuacamoleException {
        try {
            sendClipboard(clipboard);
        }
        catch (IOException e) {
            throw new GuacamoleException("Could not send clipboard data to VNC server (network error).", e);
        }
    }

    @Override
    public void send(KeyEvent event) throws GuacamoleException {
        try {
            // Add to queue
            keyEvents.add(event);
        }
        catch (IOException e) {
            throw new GuacamoleException("Could not send keyboard event to VNC server (network error).", e);
        }
    }

    @Override
    public void send(PointerEvent event) throws GuacamoleException {
        try {
            // Add to queue
            pointerEvents.add(event);
        }
        catch (IOException e) {
            throw new GuacamoleException("Could not send pointer event to VNC server (network error).", e);
        }
    }

    private static final int EVENT_DEADLINE = 500;

    private EventQueue<KeyEvent> keyEvents = new EventQueue<KeyEvent>(new EventHandler<KeyEvent>() {

        public void handle(KeyEvent e) throws IOException {
            sendKeyEvent(e.getPressed(), e.getKeySym());
        }

    }, EVENT_DEADLINE);

    private EventQueue<PointerEvent> pointerEvents = new EventQueue<PointerEvent>(new EventHandler<PointerEvent>() {

        public void handle(PointerEvent e) throws IOException {
            sendPointerEvent(
                    e.isLeftButtonPressed(),
                    e.isMiddleButtonPressed(),
                    e.isRightButtonPressed(),
                    e.isUpButtonPressed(),
                    e.isDownButtonPressed(),
                    e.getX(),
                    e.getY()
            );
        }

    }, EVENT_DEADLINE);

    private void sendClipboard(String clipboard) throws IOException {

        synchronized (output) {
            output.writeByte(MESSAGE_CLIENT_CUT_TEXT);
            output.writeBytes("   "); // Padding

            byte[] encodedString = clipboard.getBytes("UTF-8");

            output.writeInt(encodedString.length);
            output.write(encodedString);
            output.flush();
        }

    }

    private void sendKeyEvent(boolean pressed, int keysym) throws IOException {

        synchronized (output) {
            output.writeByte(MESSAGE_KEY_EVENT);
            output.writeBoolean(pressed);
            output.writeBytes("  "); // Padding
            output.writeInt(keysym);
            output.flush();
        }

        //log("Sent key event.");
    }

    private void sendPointerEvent(boolean left, boolean middle, boolean right, boolean up, boolean down, int x, int y) throws IOException {

        int buttonMask = 0;
        if (left)   buttonMask |= 1;
        if (middle) buttonMask |= 2;
        if (right)  buttonMask |= 4;
        if (up)     buttonMask |= 8;
        if (down)   buttonMask |= 16;

        synchronized (output) {
            output.writeByte(MESSAGE_POINTER_EVENT);
            output.writeByte(buttonMask);
            output.writeShort(x);
            output.writeShort(y);
            output.flush();
        }

        //log("Sent pointer event.");
    }

    private void sendEncodings(int... encodings) throws IOException {

        synchronized (output) {
            output.writeByte(MESSAGE_SET_ENCODINGS);
            output.writeBytes(" "); // padding
            output.writeShort(encodings.length);
        
            for (int encoding : encodings)
                output.writeInt(encoding);
        }

    }

    @Override
    public Instruction nextInstruction(boolean blocking)
    throws GuacamoleException {

        synchronized (instructionLock) {
            if (instructions.size() == 0) {

                try {
                    // Send framebuffer update request
                    synchronized (output) {
                        output.writeByte(MESSAGE_FRAMEBUFFER_UPDATE_REQUEST);
                        output.writeBoolean(!needRefresh); // Incremental
                        output.writeShort(0); // x
                        output.writeShort(0); // y
                        output.writeShort(frameBufferWidth); // width
                        output.writeShort(frameBufferHeight); // height
                        output.flush();
                    }
                }
                catch (IOException e) {
                    throw new GuacamoleException("Could not send framebuffer update request to VNC server (network error).", e);
                }

                // Handle incoming messages, blocking until one message exists
                try {
                    handleMessages(blocking);
                }
                catch (IOException e) {
                    throw new GuacamoleException("Could not read messages from VNC server (network error).", e);
                }
                catch (VNCException e) {
                    throw new GuacamoleException(e);
                }

            }

            // If no messages, return null.
            if (instructions.size() == 0)
                return null;

            // Send messages, oldest first
            return instructions.removeFirst();
        }

    }

    private void log(String str) {
        System.err.println(str);
    }

    @Override
    public void disconnect() throws GuacamoleException {
        try {

            // Close event queues
            keyEvents.close();
            pointerEvents.close();

            // Close socket
            sock.close();
        }
        catch (IOException e) {
            throw new GuacamoleException("Network error while closing socket.", e);
        }
    }
}

