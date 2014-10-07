/**
 * This file is part of jModuleConnect.
 * 
 * jModuleConnect is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation, either version 3 of the License, or (at your option) 
 * any later version.
 * 
 * jModuleConnect is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more 
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with jModuleConnect. If not, see <http://www.gnu.org/licenses/>.
 */
package de.sitec.jmoduleconnect.file;

import de.sitec.jmoduleconnect.utils.BinaryUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class represents an OBEX frame. The OBEX standard can be found at 
 * http://www.irda.org/.
 * @author sitec systems GmbH
 * @since 1.0
 */
/* package */ class Obex
{
    private final Code obexCode;
    private final Map<ObexHeader.Code, ObexHeader> headers;
    private final byte flags;
    
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    private static final byte HEADER_LENGTH = 3;
    private static final byte REQUEST_CONNECT_LENGTH = 7;
    private static final byte REQUEST_SETPATH_LENGTH = 5;
    private static final byte OBEX_VERSION_10 = (byte)0x10;
    private static final short MAX_PACKET_LENGTH = (short)0xFFFF;
    
    /**
     * Defines ids of OBEX frames.
     * @since 1.0
     */
    public enum Code
    {
        REQUEST_CONNECT(Type.REQUEST, (byte)0x80),
        REQUEST_DISCONNECT(Type.REQUEST, (byte)0x81),
        REQUEST_PUT(Type.REQUEST, (byte)0x02),
        REQUEST_GET(Type.REQUEST, (byte)0x03),
        REQUEST_SETPATH(Type.REQUEST, (byte)0x85),
        REQUEST_SETPATH2(Type.REQUEST, (byte)0x86),
        REQUEST_SESSION(Type.REQUEST, (byte)0x87),
        REQUEST_ABORT(Type.REQUEST, (byte)0xFF),   
        REQUEST_FINAL(Type.REQUEST, (byte)0x80),
        REQUEST_PUT_FINAL(Type.REQUEST, (byte)0x82),
        REQUEST_GET_FINAL(Type.REQUEST, (byte)0x83),
        
        FLAG_SETPATH_CREATE(Type.FLAG, (byte)0x00),
        FLAG_SETPATH_NOCREATE(Type.FLAG, (byte)0x02),
        FLAG_SETPATH_PARENT_FOLDER(Type.FLAG, (byte)0x03),
        
        RESPONSE_SUCCESS(Type.RESPONSE, (byte)0xA0),
//        RESPONSE_SUCCESS(Type.RESPONSE, (byte)0x20),
        RESPONSE_CONTINUE(Type.RESPONSE, (byte)0x90),
//        RESPONSE_CONTINUE(Type.RESPONSE, (byte)0x10),
        RESPONSE_CREATED(Type.RESPONSE, (byte)0x21),
        RESPONSE_BADREQUEST(Type.RESPONSE, (byte)0x40),
        RESPONSE_FINAL(Type.RESPONSE, (byte)0x80),
        RESPONSE_DATABASE_FULL(Type.RESPONSE, (byte)0xE0),
        RESPONSE_FORBIDDEN(Type.RESPONSE, (byte)0xC3);
        
        private final Type type;
        private final byte code;
        
        private Code(final Type type, final byte code)
        {
            this.type = type;
            this.code = code;
        }

        /**
         * Gets the id of the frame.
         * @return The if of the frame
         * @since 1.0
         */
        public byte getCode()
        {
            return code;
        }

        /**
         * Gets the {@link Type} of the frame.
         * @return The {@link Type} of the frame
         * @since 1.0
         */
        public Type getType()
        {
            return type;
        }
        
        /**
         * Gets the <code>Code</code> for the input values.
         * @param type The {@link Type} of the frame
         * @param value The id as binary
         * @return The <code>Code</code> for the input values
         * @since 1.0
         * @throws IllegalArgumentException If no <code>Code</code> available for 
         *         input values
         */
        public static Code getCode(final Type type, final byte value)
        {
            for(final Code currCode: values())
            {
                if(currCode.type == type && currCode.code == value)
                {
                    return currCode;
                }
            }
            
            throw new IllegalArgumentException("Parameter value is invald: " 
                    + BinaryUtils.toHexString(value));
        }
    }
    
    /**
     * Defines frame types for OBEX.
     * @since 1.0
     */
    public enum Type
    {
        REQUEST((byte)0),
        FLAG((byte)1),
        RESPONSE((byte)2);
        
        private final byte type;
        
        private Type(final byte type)
        {
            this.type = type;
        }

        /**
         * Get the type of an obex frame.
         * @return The type of an obex frame
         * @since 1.0
         */
        public byte getType()
        {
            return type;
        }
    }
    
    public Obex(final Code obexCode, final Map<ObexHeader.Code, ObexHeader> data)
    {
        if(data == null)
        {
            throw new IllegalArgumentException("Parameter data cant be null");
        }
        this.obexCode = obexCode;
        this.headers = data;
        flags = 0;
    }
    
    public Obex(final Code obexCode, final ObexHeader data, final byte flags)
    {
        this.obexCode = obexCode;
        this.headers = new EnumMap<ObexHeader.Code, ObexHeader>(ObexHeader.Code.class);
        this.headers.put(data.getObexCode(), data);
        this.flags = flags;
    }

    public Obex(final Code obexCode, final ObexHeader data)
    {
        this(obexCode, data, (byte)0);
    }

    public Obex(final Code obexCode, final byte flags)
    {
        this.obexCode = obexCode;
        this.headers = null;
        this.flags = flags;
    }
    
    public Obex(final Code obexCode)
    {
        this(obexCode, (byte)0);
    }
    
    public Obex(final byte[] frame)
    {
//        System.out.println("Obex Frame: " + BinaryUtils.toHexString(frame));
        if(!validate(frame))
        {
            throw new IllegalArgumentException("The input byte array contains no valid OBEX frame");
        }
        
        obexCode = Code.getCode(Type.RESPONSE, frame[0]);
        
        if(frame.length > HEADER_LENGTH)
        {
            int pointer = 3;
            if(frame[3] == OBEX_VERSION_10)
            {
                flags = frame[4];
                pointer += 4;
            }
            else
            {
                flags = 0;
            }

            headers = new EnumMap<ObexHeader.Code, ObexHeader>(ObexHeader.Code.class);
//            data = new ArrayList<ObexHeader>();
            while(pointer < frame.length)
            {
                final ObexHeader header = ObexHeader.createObexHeader(frame, pointer);
//                System.out.println("Pointer: " + pointer);
//                System.out.println(BinaryUtils.toHexString(header.toByteArray()));
                pointer += header.getLength();
                headers.put(header.getObexCode(), header);
            }
        }
        else
        {
            flags = 0;
            headers = null;
        }
    }

    /**
     * Gets all header from the obex frame.
     * @return All header from the obex frame
     * @since 1.0
     */
    public Map<ObexHeader.Code, ObexHeader> getHeaders()
    {
        return headers;
    }
    
    /**
     * Gets the specific {@link ObexHeader} to the parameter <code>obexCode</code>.
     * @param obexCode The specific {@link Code}
     * @return The specific {@link ObexHeader}
     * @since 1.0
     */
    public ObexHeader getHeader(final ObexHeader.Code obexCode)
    {
        return headers.get(obexCode);
    }

    /**
     * Gets the {@link Code} of the OBEX frame.
     * @return The {@link Code} of the OBEX frame
     * @since 1.0
     */
    public Code getObexCode()
    {
        return obexCode;
    }
    
    /**
     * Validates an OBEX frame basend on the data length.
     * @param frame An <code>byte[]</code> that contains an OBEX frame
     * @return <code>true</code> - The OBEX frame is valid / <code>false</code>
     *         - The OBEX frame is invalid
     * @since 1.0
     */
    public static boolean validate(final byte[] frame)
    {
        final short length = BinaryUtils.byteArrToShort(frame, false, 1);
        
        return length == frame.length;
    }
    
    /**
     * Gets the OBEX frame as an <code>byte[]</code>.
     * @return The OBEX frame as an <code>byte[]</code>
     * @since 1.0
     */
    public byte[] toByteArray()
    {
        byte[] headersByteArr = new byte[0];
        if(headers != null)
        {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            for(final ObexHeader header: headers.values())
            {
                if(header != null)
                {
                    try
                    {
                        bos.write(header.toByteArray());
                    }
                    catch (final IOException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
            try
            {
                bos.close();
            }
            catch (final IOException ex)
            {
                ex.printStackTrace();
            }

            headersByteArr = bos.toByteArray();
        }
        
        int length = headersByteArr.length;
        
        switch(obexCode)
        {
            case REQUEST_CONNECT:
                length += REQUEST_CONNECT_LENGTH;
                break;
            case REQUEST_SETPATH:
                length += REQUEST_SETPATH_LENGTH;
                break;
            default:
                length += HEADER_LENGTH;
                break;
        }
        
        final byte[] result = new byte[length];
        result[0] = obexCode.code;
        
        BinaryUtils.putToByteArr(result, (short)length, 1, false);
        
        int position = 3;
        
        if(obexCode == Code.REQUEST_CONNECT)
        {
            result[3] = (byte)0x13; // Obex Version
            result[4] = (byte)0x00; // Flags
            position = 7;
        }
        else if(obexCode == Code.REQUEST_SETPATH)
        {
            result[3] = flags;
            result[4] = (byte)0x00;
            position = 5;
        }
        
        System.arraycopy(headersByteArr, 0, result, position, headersByteArr.length);
        
        return result;
    }
}
