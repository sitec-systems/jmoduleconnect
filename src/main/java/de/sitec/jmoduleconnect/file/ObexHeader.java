/**
 * jModuleConnect is an framework for communication and file management on modem 
 * modules.
 * 
 * This project was inspired by the project TC65SH 
 * by Christoph Vilsmeier: <http://www.vilsmeier-consulting.de/tc65sh.html>
 * 
 * Copyright (C) 2015 sitec systems GmbH <http://www.sitec-systems.de>
 * 
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
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thic class represents OBEX header. The OBEX standard can be found at 
 * http://www.irda.org/.
 * @author sitec systems GmbH
 * @since 1.0
 */
/* package */ class ObexHeader<T>
{
    protected final Code obexCode;
    private final T data;
    
    private static final Logger LOG = LoggerFactory.getLogger(ObexHeader.class);
    protected static final Charset NAME_CHARSET = Charset.forName("UTF_16BE");
    protected static final Charset BYTE_CHARSET = Charset.forName("ISO_8859_1");
    
    /**
     * Defines id for the OBEX header.
     * @since 1.0
     */
    public enum Code
    {
        COUNT((byte)0xC0),
        NAME((byte)0x01),
        TYPE ((byte)0x42),
        LENGTH((byte)0xC3),
        TIME((byte)0x44),
        DESCRIPTION((byte)0x05),
        TARGET((byte)0x46),
        HTTP((byte)0x47),
        BODY((byte)0x48),
        END_OF_BODY((byte)0x49),
        WHO((byte)0x4A),
        CONNECTION_ID((byte)0xCB),
        APP_PARAMETERS((byte)0x4C),
        AUTH_CHALLENGE((byte)0x4D),
        AUTH_RESPONSE((byte)0x4E),
        CREATOR_ID((byte)0xCF),
        WAN_UUID((byte)0x50),
        OBJECT_CLASS((byte)0x51),
        SESSION_PARAMETERS((byte)0x52),
        SESSION_SEQUENCE_NUMBER((byte)0x93);
        
        private final byte code;

        private Code(final byte code)
        {
            this.code = code;
        }
        
        /**
         * Gets the <code>Code</code> for the input value.
         * @param value The code as binary
         * @return The <code>Code</code> for the input value
         * @since 1.0
         */
        public static Code getCode(final byte value)
        {
//            System.out.println("Header: " + BinaryUtils.toHexString(value));
            for(final Code currCode: values())
            {
                if(currCode.code == value)
                {
                    return currCode;
                }
            }
            
            throw new IllegalArgumentException("Parameter value " 
                    + BinaryUtils.toHexString(value) + " is invald");
        }

        /**
         * Gets the id of the OBEX header.
         * @return The id of the OBEX header
         * @since 1.0
         */
        public byte getCode()
        {
            return code;
        }
    }
    
    /**
     * Creates an <code>ObexHeader</code> from input <code>byte[]</code>.
     * @param data The input <code>byte[]</code>
     * @param offset The start point of the OBEX header
     * @return The created <code>ObexHeader</code> or <code>null</code> for unknown
     *         OBEX header
     * @since 1.0
     */
    public static ObexHeader createObexHeader(final byte[] data, final int offset)
    {
        final Code code = Code.getCode(data[offset]);
        
        switch(code)
        {
            case WHO:
            case TARGET:
            case BODY: 
            case END_OF_BODY:
            case APP_PARAMETERS:
                final short byteLength = BinaryUtils.byteArrToShort(data, false, offset + 1);
//                System.out.println("Length: " + byteLength);
                final byte[] bytePayload = Arrays.copyOfRange(data, offset + 3, offset + byteLength);
//                System.out.println(BinaryUtils.toHexString(bytePayload));
                return new ObexHeader<byte[]>(code, bytePayload);
            case TYPE:
            case NAME:
                final short stringLength = BinaryUtils.byteArrToShort(data, false, 1);
                final byte[] stringPayload = Arrays.copyOfRange(data, offset + 3, stringLength - 3);
                return new ObexHeader<String>(code, new String(stringPayload, NAME_CHARSET));
            case LENGTH:
            case CONNECTION_ID:
                final int intPayload = BinaryUtils.byteArrToInt(data, false, offset + 1);
                return new ObexHeader<Integer>(code, intPayload);
            case TIME:
                Date date = null;
                final short timeLength = BinaryUtils.byteArrToShort(data, false, 1);
                final byte[] timePayload = Arrays.copyOfRange(data, offset + 3, timeLength - 3);
                try
                {
                    date = Obex.DATE_FORMAT.parse(new String(timePayload, BYTE_CHARSET));
                }
                catch (final ParseException ex)
                {
                    LOG.error("Error at creating OBEX header: " + Code.TIME, ex);
                }
                return new ObexHeader<Date>(code, date);
        }
        
        return null;
    }

    public ObexHeader(final Code obexCode, final T data)
    {
        this.obexCode = obexCode;
        this.data = data;
    }
    
    /**
     * Gets the data of the OBEX header
     * @return The data of the OBEX header
     * @since 1.0
     */
    public T getData()
    {
        return data;
    }
    
    /**
     * Gets the length of the OBEX header.
     * @return The length of the OBEX header
     * @since 1.0
     */
    public int getLength()
    {
        int result = 0;
        switch(obexCode)
        {
            case CONNECTION_ID:
            case LENGTH:
                result = 5;
                break;
            case BODY: 
            case APP_PARAMETERS:
            case END_OF_BODY:
                final byte[] body = (byte[])data;
                result = body.length + 3;
                break;
            case NAME:
                final byte[] name = ((String)data).getBytes(NAME_CHARSET);
                result = 3 + name.length;
                break;
            case WHO:
            case TARGET:
                final byte[] target = (byte[])data;
                result = target.length + 3;
                break;
            case TIME:
                final byte[] time = Obex.DATE_FORMAT.format((Date)data).getBytes(BYTE_CHARSET);
                result = 3 + time.length;
                break;
             case TYPE:
                final byte[] type = ((String)data).getBytes(BYTE_CHARSET);
                result = 3 + type.length;
                break;
        }
        
        return result;
    }

    /**
     * Gets the {@link Code} of the OBEX header.
     * @return The {@link Code} of the OBEX header
     * @since 1.0
     */
    public Code getObexCode()
    {
        return obexCode;
    }
    
    /**
     * Gets the OBEX header as an <code>byte[]</code>.
     * @return The OBEX header as an <code>byte[]</code>
     * @since 1.0
     */
    public byte[] toByteArray()
    {
        byte[] result = null;
        switch(obexCode)
        {
            case APP_PARAMETERS:
                final byte[] param = (byte[])data;
                result = new byte[param.length + 3];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, (short)result.length, 1, false);
                System.arraycopy(param, 0, result, 3, param.length);
                break;
            case CONNECTION_ID:
                result = new byte[5];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, ((Integer)data).intValue(), 1, false);
                break;
            case BODY: 
            case END_OF_BODY:
                final byte[] body = (byte[])data;
                result = new byte[body.length + 3];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, (short)result.length, 1, false);
                System.arraycopy(body, 0, result, 3, body.length);
                break;
            case LENGTH:
                final int length = ((Integer)data).intValue();
                result = new byte[5];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, length, 1, false);
                break;
            case NAME:
                final byte[] name = ((String)data).getBytes(NAME_CHARSET);
                result = new byte[3 + name.length];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, (short)result.length, 1, false);
                System.arraycopy(name, 0, result, 3, name.length);
                break;
            case WHO:
            case TARGET:
                final byte[] target = (byte[])data;
                result = new byte[target.length + 3];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, (short)result.length, 1, false);
                System.arraycopy(target, 0, result, 3, target.length);
                break;
            case TIME:
                final byte[] time = Obex.DATE_FORMAT.format((Date)data).getBytes(BYTE_CHARSET);
                result = new byte[3 + time.length];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, (short)result.length, 1, false);
                System.arraycopy(time, 0, result, 3, time.length);
                break;
             case TYPE:
                final byte[] type = ((String)data).getBytes(BYTE_CHARSET);
                result = new byte[3 + type.length];
                result[0] = obexCode.code;
                BinaryUtils.putToByteArr(result, (short)result.length, 1, false);
                System.arraycopy(type, 0, result, 3, type.length);
                break;
        }
        
        return result;
    }
    
    /**
     * Gets the <code>String</code> representation of the data.
     * @return The <code>String</code> representation of the data or <code>null</code>
     *         if no data available
     * @since 1.4
     */
    private String getDataString()
    {
        String result = null;
        switch(obexCode)
        {
            case LENGTH:
            case CONNECTION_ID:
                result = ((Integer)data).toString();
                break;
            case WHO:
            case TARGET:
            case APP_PARAMETERS:
            case BODY: 
            case END_OF_BODY:
                result = BinaryUtils.toHexString((byte[])data);
                break;
            case TYPE:
            case NAME:
                result = (String)data;
                break;
            case TIME:
                result = ((Date)data).toString();
                break;
        }
        
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("ObexHeader{obexCode=");
        sb.append(obexCode);
        sb.append(", data=");
        sb.append(getDataString());
        sb.append('}');
        
        return sb.toString();
    }
}
