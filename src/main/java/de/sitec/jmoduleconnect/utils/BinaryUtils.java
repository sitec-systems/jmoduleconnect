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
package de.sitec.jmoduleconnect.utils;

/**
 * Utils to handle data in binary format.
 * @author sitec systems GmbH
 * @since 2.0
 */
public final class BinaryUtils
{
    /**
     * Standard mask for binary to interpret as unsigned
     */
    private final static int MASK = 0xFF;  
    
    /**
     * Constants for the length in byte of Java primitve datatypes.
     * @since 1.0
     */
    public final static byte BYTE_BIT_LENGTH = 8;
    public final static byte SHORT_BYTE_LENGTH = 2;
    public final static byte INT_BYTE_LENGTH = 4;
    public final static byte FLOAT_BYTE_LENGTH = 4;
    public final static byte LONG_BYTE_LENGTH = 8;
    
    private final static byte[] BIT = new byte[]{(byte)0x01, (byte)0x02
            , (byte)0x04, (byte)0x08, (byte)0x010, (byte)0x20, (byte)0x40
            , (byte)0x80};
    
    private BinaryUtils(){}
    
    /**
     * Converts a <code>byte</code> to a unsigned <code>short</code> value
     * between 0 - 255.
     * @param value the <code>byte</code> value
     * @return the value as unsigned
     * @since 1.0
     */
    public static short toUnsigned(final byte value)
    {
        return (short)(value & MASK);
    }
    
    /**
     * Converts a <code>short</code> to a unsigned <code>int</code> value
     * between 0 - 65535.
     * @param value the <code>short</code> value
     * @return the value as unsigned
     * @since 1.0
     */
    public static int toUnsigned(final short value)
    {
        return value & 0xFFFF;
    }
    
    /**
     * Converts a <code>int</code> to a unsigned <code>long</code> value
     * between 0 - 4294967295.
     * @param value the <code>int</code> value
     * @return the value as unsigned
     * @since 1.0
     */
    public static long toUnsigned(final int value)
    {
        return (long)(value & 0x00000000FFFFFFFFL);
    }
    
    /**
     * Converts a <code>String</code> with ASCII hexdecimal values to a similar
     * <code>byte</code>.
     * @param str The <code>String</code> with the hexdecimal values
     * @return The converted <code>byte[]</code>.
     * @since 1.0
     */
    public static byte[] hexStrToByteArr(final String str) 
    {
        if(str.indexOf(" ") != -1)
        {
            throw new IllegalArgumentException("Parameter must have no whitespaces");
        }
        final int length = str.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) 
        {
            data[i / 2] = (byte)((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i+1), 16));
        }

        return data;
    }

    
    /**
     * Returns a unsigned hex <code>String</code> of the <code>byte</code> value.
     * @param value The <code>byte</code> value
     * @return the value as unsigned hex
     * @since 1.0
     */
    public static String toHexString(final byte value)
    {
        String result = Integer.toHexString(toUnsigned(value)).toUpperCase();
        
        if(result.length() == 1)
        { 
            result = "0x0" + result;
        }
        else
        {
            result = "0x" + result;
        }      
        
        return result;
    }
    
    /**
     * Returns a <code>String</code> with unsigned hex representation of the input
     * <code>byte[]</code>.
     * @param array The <code>byte[]</code>
     * @return The <code>byte[]</code> as unsigned hex <code>String</code>
     * @since 1.0
     */
    public static String toHexString(final byte[] array)
    {
        nullCheck(array, "array");
        
        final StringBuffer sb = new StringBuffer();
        for(int i=0; i<array.length; i++)
        {
            sb.append("[").append(toHexString(array[i])).append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Converts a <code>byte[2]</code> to <code>short</code>
     * @param buffer The <code>byte[]</code> that contains the <code>short</code>
     *               value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last Postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>short</code> value
     * @since 1.0
     */
    public static short byteArrToShort(final byte[] buffer, final boolean lsbIsFirst)
    {
        lengthCheck(buffer, SHORT_BYTE_LENGTH);
        
        return byteArrToShort(buffer, lsbIsFirst, 0);
    }
    
    /**
     * Converts a part of <code>byte[]</code> to <code>short</code>.
     * @param buffer The <code>byte[]</code> that contains the <code>short</code>
     *        value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last Postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @param offset The position at <code>byte[]</code> from their the next
     *        2 <code>byte</code> used for converting
     * @return The converted <code>short</code> value
     * @since 1.0
     */
    public static short byteArrToShort(final byte[] buffer, final boolean lsbIsFirst
            , final int offset)
    {
        nullCheck(buffer, "buffer");
        offsetCheck(buffer, offset);
        
        short value;
        if(lsbIsFirst)
        {
            value = (short)(MASK & buffer[offset]);
            value |= ((MASK & buffer[offset + 1]) << 8);
        }
        else
        {
            value = (short)((MASK & buffer[offset]) << 8);
            value |= (MASK & buffer[offset + 1]);
        }
        
        return value;
    }

    /**
     * Converts a <code>byte[4]</code> to <code>int</code>
     * @param buffer The <code>byte[]</code> that contains the <code>int</code>
     *        value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last Postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>int</code> value
     * @since 1.0
     */
    public static int byteArrToInt(final byte[] buffer, final boolean lsbIsFirst)
    {
        lengthCheck(buffer, INT_BYTE_LENGTH);

        return byteArrToInt(buffer, lsbIsFirst, 0);
    }
    
    /**
     * Converts a part of <code>byte[]</code> to <code>int</code>.
     * @param buffer The <code>byte[]</code> that contains the <code>int</code>
     *               value as binary
     * @param lsbIsFirst If the low Byte at first Postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @param offset The position at <code>byte[]</code> from their the next
     *        4 <code>byte</code> used for converting
     * @return The converted <code>int</code> value
     * @since 1.0
     */
    public static int byteArrToInt(final byte[] buffer, final boolean lsbIsFirst
            , final int offset)
    {
        nullCheck(buffer, "buffer");
        offsetCheck(buffer, offset);

        int value;
        if(lsbIsFirst)
        {
            value  = (MASK & buffer[offset]);
            value |= (MASK & buffer[offset + 1]) << 8;
            value |= (MASK & buffer[offset + 2]) << 16;
            value |= (MASK & buffer[offset + 3]) << 24;
        }
        else
        {
            value  = (MASK & buffer[offset]) << 24;
            value |= (MASK & buffer[offset + 1]) << 16;
            value |= (MASK & buffer[offset + 2]) << 8;
            value |= (MASK & buffer[offset + 3]);
        }

        return value;
    }

    /**
     * Converts a <code>byte[8]</code> to <code>long</code>
     * @param buffer The <code>byte[]</code> that contains the <code>long</code>
     *               value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>long</code> value
     * @since 1.0
     */
    public static long byteArrToLong(final byte[] buffer, final boolean lsbIsFirst)
    {
        lengthCheck(buffer, LONG_BYTE_LENGTH);
        
        return byteArrToLong(buffer, lsbIsFirst, 0);
    }
    
    /**
     * Converts a part of <code>byte[]</code> to <code>long</code>.
     * @param buffer The <code>byte[]</code> that contains the <code>long</code>
     *        value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last Postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @param offset The position at <code>byte[]</code> from their the next
     *        8 <code>byte</code> used for converting
     * @return The converted <code>long</code> value
     * @since 1.0
     */
    public static long byteArrToLong(final byte[] buffer, final boolean lsbIsFirst
            , final int offset)
    {
        nullCheck(buffer, "buffer");
        offsetCheck(buffer, offset);
        
        long value;
        int first;
        int second;
        
        if(lsbIsFirst)
        {
            first = byteArrToInt(buffer, lsbIsFirst, offset + 4);
            second = byteArrToInt(buffer, lsbIsFirst, offset);
            
            value = ((first & 0xFFFFFFFFL) << 32) | (second & 0xFFFFFFFFL);
        }
        else
        {
            first = byteArrToInt(buffer, lsbIsFirst, offset);
            second = byteArrToInt(buffer, lsbIsFirst, offset + 4);
            
            value = ((first & 0xFFFFFFFFL) << 32) | (second & 0xFFFFFFFFL);
        }

        return value;
    }

     /**
     * Converts a <code>byte[4]</code> to <code>float</code>
     * @param buffer The <code>byte[]</code> that contains the <code>float</code>
     *        value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last Postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>float</code> value
     * @since 1.0
     */
    public static float byteArrToFloat(final byte[] buffer, final boolean lsbIsFirst)
    {
        lengthCheck(buffer, FLOAT_BYTE_LENGTH);
        
        return Float.intBitsToFloat(byteArrToInt(buffer, lsbIsFirst, 0));
    }
    
    
    /**
     * Converts a part of <code>byte[]</code> to <code>float</code>.
     * @param buffer The <code>byte[]</code> that contains the <code>float</code>
     *        value as binary
     * @param lsbIsFirst If the low Byte at first postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last Postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @param offset The position at <code>byte[]</code> from their the next
     *        4 <code>byte</code> used for converting
     * @return The converted <code>float</code> value
     * @since 1.0
     */
    public static float byteArrToFloat(final byte[] buffer, final boolean lsbIsFirst
            , final int offset)
    {    
        return Float.intBitsToFloat(byteArrToInt(buffer, lsbIsFirst, offset));
    }

    /**
     * Converts a <code>float</code> to <code>byte[4]</code>.
     * @param value The <code>float</code> value that must be converted
     * @param lsbIsFirst If the low Byte at first Postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>byte[4]</code>
     * @since 1.0
     */
    public static byte[] toByteArr(final float value, final boolean lsbIsFirst)
    {  
        return toByteArr(Float.floatToIntBits(value), false);
    }
    
    /**
     * Converts and puts a <code>float</code> to the input <code>byte[]</code>.
     * @param inArr The <code>byte[]</code> in that the method add the converted
     *        value
     * @param value The <code>float</code> value that must be converted
     * @param offset The offset after that the converted value will at in the 
     *        input array
     * @param lsbIsFirst If the low Byte at first Postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @since 1.0
     */
    public static void putToByteArr(final byte[] inArr, final float value
            , final int offset, final boolean lsbIsFirst)
    {
        putToByteArr(inArr, Float.floatToIntBits(value), offset, lsbIsFirst);
    }

    /**
     * Converts a <code>int</code> value to a <code>byte[4]</code>.
     * @param value The <code>int</code> value that must be converted
     * @param lsbIsFirst lsbIsFirst If the low Byte at first postion at
     *        <code>byte[]</code> then select <code>true</code> or if its at last 
     *        Postion in the <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>byte[4]</code>
     * @since 1.0
     */
    public static byte[] toByteArr(final int value, final boolean lsbIsFirst)
    {
        byte[] result = new byte[4];
        
        putToByteArr(result, value, 0, lsbIsFirst);

        return result;
    }
    
    /**
     * Converts and puts a <code>int</code> to the input <code>byte[]</code>.
     * @param inArr The <code>byte[]</code> in that the method add the converted
     *        value
     * @param value The <code>int</code> value that must be converted
     * @param offset The offset after that the converted value will at in the 
     *        input array
     * @param lsbIsFirst If the low Byte at first Postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @since 1.0
     */
    public static void putToByteArr(final byte[] inArr, final int value
            , final int offset, final boolean lsbIsFirst)
    {
        nullCheck(inArr, "inArr");
        minLengthCheck(inArr, (byte)4);
        
        if(lsbIsFirst)
        {
            inArr[offset] = (byte)value;
            inArr[offset + 1] = (byte)(value >>> 8);
            inArr[offset + 2] = (byte)(value >>> 16);
            inArr[offset + 3] = (byte)(value >>> 24);
        }
        else
        {
            inArr[offset] = (byte)(value >>> 24);
            inArr[offset + 1] = (byte)(value >>> 16);
            inArr[offset + 2] = (byte)(value >>> 8);
            inArr[offset + 3] = (byte)value;
        }
    }

    /**
     * Converts a <code>long</code> value to a <code>byte[8]</code>.
     * @param value The <code>int</code> value that must be converted
     * @param lsbIsFirst lsbIsFirst If the low Byte at first postion at 
     *        <code>byte[]</code> then select <code>true</code> or if its at last 
     *        postion in the <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>byte[8]</code>
     * @since 1.0
     */
    public static byte[] toByteArr(final long value, final boolean lsbIsFirst)
    {
        byte[] result = new byte[8];

        putToByteArr(result, value, 0, lsbIsFirst);

        return result;
    }
    
    /**
     * Converts and puts a <code>long</code> to the input <code>byte[]</code>.
     * @param inArr The <code>byte[]</code> in that the method add the converted
     *        value
     * @param value The <code>long</code> value that must be converted
     * @param offset The offset after that the converted value will at in the 
     *        input array
     * @param lsbIsFirst If the low Byte at first Postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @since 1.0
     */
    public static void putToByteArr(final byte[] inArr, final long value
            , final int offset, final boolean lsbIsFirst)
    {
        nullCheck(inArr, "inArr");
        minLengthCheck(inArr, (byte)8);
        
        if(lsbIsFirst)
        {
            inArr[offset] = (byte)value;
            inArr[offset + 1] = (byte)(value >>> 8);
            inArr[offset + 2] = (byte)(value >>> 16);
            inArr[offset + 3] = (byte)(value >>> 24);
            inArr[offset + 4] = (byte)(value >>> 32);
            inArr[offset + 5] = (byte)(value >>> 40);
            inArr[offset + 6] = (byte)(value >>> 48);
            inArr[offset + 7] = (byte)(value >>> 56);
        }
        else
        {
            inArr[offset] = (byte)(value >>> 56);
            inArr[offset + 1] = (byte)(value >>> 48);
            inArr[offset + 2] = (byte)(value >>> 40);
            inArr[offset + 3] = (byte)(value >>> 32);
            inArr[offset + 4] = (byte)(value >>> 24);
            inArr[offset + 5] = (byte)(value >>> 16);
            inArr[offset + 6] = (byte)(value >>> 8);
            inArr[offset + 7] = (byte)value;
        }
    }

    /**
     * Converts a <code>short</code> Value to a <code>byte[2]</code>.
     * @param value The <code>short</code> value that must be converted
     * @param lsbIsFirst lsbIsFirst If the low Byte at first postion at 
     *        <code>byte[]</code> then select <code>true</code> or if its at last 
     *        postion in the <code>byte[]</code> then select <code>false</code>
     * @return The converted <code>byte[2]</code>
     * @since 1.0
     */
    public static byte[] toByteArr(final short value, final boolean lsbIsFirst)
    {
        byte[] result = new byte[2];

        putToByteArr(result, value, 0, lsbIsFirst);

        return result;
    }
    
    /**
     * Converts and puts a <code>short</code> to the input <code>byte[]</code>.
     * @param inArr The <code>short</code> in that the method add the converted
     *        value
     * @param value The <code>float</code> value that must be converted
     * @param offset The offset after that the converted value will at in the 
     *        input array
     * @param lsbIsFirst If the low Byte at first Postion at <code>byte[]</code>
     *        then select <code>true</code> or if its at last postion in the
     *        <code>byte[]</code> then select <code>false</code>
     * @since 1.0
     */
    public static void putToByteArr(final byte[] inArr, final short value
            , final int offset, final boolean lsbIsFirst)
    {
        nullCheck(inArr, "inArr");
        minLengthCheck(inArr, (byte)2);
        
        if(lsbIsFirst)
        {
            inArr[offset] = (byte)value;
            inArr[offset + 1] = (byte)(value >>> 8);
        }
        else
        {
            inArr[offset] = (byte)(value >>> 8);
            inArr[offset + 1] = (byte)value;
        }
    }
    
    /**
     * Gets the bit state of a specific bit from a <code>byte</code> as <code>
     * boolean</code>.
     * @param value The <code>byte</code> value
     * @param bitNbr The specific bit in the <code>byte</code>, from 0 to 7
     * @return The bit state as <code>boolean</code>.
     * @since 1.0
     */
    public static boolean bitToBoolean(final byte value, final int bitNbr)
    {
        if(bitNbr < 0 || bitNbr > 7)
        {
            throw new IllegalArgumentException("Parameter bitNbr is out of range (0-7)");
        }
        
        return (value & BIT[bitNbr]) != 0;
    }
    
    /**
     * Gets the bit states from a <code>byte</code> as <code>boolean[]</code>.
     * @param value The <code>byte</code> value
     * @return The bit states as <code>boolean[]</code>
     * @since 1.0
     */
    public static boolean[] byteToBoolArr(final byte value)
    {
        final boolean[] result = new boolean[BYTE_BIT_LENGTH];
        
        for(int i=0; i<result.length; i++)
        {
            result[i] = bitToBoolean(value, i);
        }
        
        return result;
    }
    
    /**
     * Sets a specific bit in the input <code>byte</code>.
     * @param value The input <code>byte</code>
     * @param bitNbr The specific bit in the <code>byte</code>, from 0 to 7
     * @param state The new state of the specific bit
     * @return The input <code>byte</code> with changed bit
     * @since 1.0
     */
    public static byte booleanToBit(final byte value, final int bitNbr
            , final boolean state)
    {
        if(bitNbr < 0 || bitNbr > 7)
        {
            throw new IllegalArgumentException("Parameter bitNbr is out of range (0-7)");
        }
        
        final byte convertedBit = BIT[bitNbr];
        
        byte result = value;
        if(state)
        {
            result |= convertedBit;
        }
        else
        {
            result &= ~convertedBit;
        }
        return result;
    }
    
    /**
     * Converts a <code>boolean[]</code> to a <code>byte</code> and the bits get
     * the states from the <code>boolean[]</code>.
     * @param values The input <code>boolean[]</code>
     * @return The byte with setted bits
     * @since 1.0
     */
    public static byte boolArrToByte(final boolean[] values)
    {
        if(values == null || values.length != BYTE_BIT_LENGTH)
        {
            throw new IllegalArgumentException("Parameter values is invalid");
        }
        
        byte result = 0;
        
        for(int i=0; i<values.length; i++)
        {
            result = booleanToBit(result, i, values[i]);
        }
        
        return result;
    }
    
    private static void nullCheck(final byte[] buffer, final String parameter)
    {
        if(buffer == null)
        {
            throw new NullPointerException("Parameter " 
                    + parameter + " cant be null");
        }
    }
    
    private static void lengthCheck(final byte[] buffer, final byte length)
    {
        if (buffer.length != length)
        {
            throw new IllegalArgumentException("buffer length must be " 
                    + length + " bytes");
        }
    }
    
    private static void offsetCheck(final byte[] buffer, final int offset)
    {
        if(offset < 0 || offset > buffer.length)
        {
            throw new ArrayIndexOutOfBoundsException("Parameter if out of bound");
        }
    }
    
    private static void minLengthCheck(final byte[] buffer, final byte length)
    {
        if (buffer.length < length)
        {
            throw new IllegalArgumentException("Input array length must be min " 
                    + length + " bytes");
        }
    }
}
