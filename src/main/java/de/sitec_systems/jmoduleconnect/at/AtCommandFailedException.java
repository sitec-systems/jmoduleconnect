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
/*
 * Author: Mattes Standfuss
 * Copyright (c): sitec systems GmbH, 2015
 */
package de.sitec_systems.jmoduleconnect.at;

/**
 * Thrown if the response of an AT command is <code>ERROR</code>.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class AtCommandFailedException extends Exception
{
    private final Type type;
    private final short errorCode;
    
    /**
     * Indicates that the error code mode is off.
     * @since 1.4
     */
    private static final short ERROR_CODE_MODE_OFF = -32768;
    
    /**
     * Constructs an <code>AtCommandFailedException</code> with no
     * detail message.
     * @param type The type of the AT error
     * @since 1.4
     */
    public AtCommandFailedException(final Type type)
    {
        super();
        this.type = type;
        errorCode = ERROR_CODE_MODE_OFF;
    }

    /**
     * Constructs an <code>AtCommandFailedException</code> with the
     * specified detail message.
     * @param type The type of the AT error
     * @param message The detail message
     * @since 1.4
     */
    public AtCommandFailedException(final Type type, final String message)
    {
        super(message);
        this.type = type;
        errorCode = ERROR_CODE_MODE_OFF;
    }

    /**
     * Constructs an <code>AtCommandFailedException</code> and takes an other 
     * <code>Throwable</code>.
     * @param type The type of the AT error
     * @param cause The other throwable
     * @since 1.4
     */
    public AtCommandFailedException(final Type type, final Throwable cause)
    {
        super(cause);
        this.type = type;
        errorCode = ERROR_CODE_MODE_OFF;
    }
    
    /**
     * Constructs an <code>AtCommandFailedException</code> with the
     * specified detail message and takes an other <code>Throwable</code>.
     * @param type The type of the AT error
     * @param message  The detail message
     * @param cause The other throwable
     * @since 1.4
     */
    public AtCommandFailedException(final Type type, final String message
            , final Throwable cause)
    {
        super(message, cause);
        this.type = type;
        errorCode = ERROR_CODE_MODE_OFF;
    }

    /**
     * Constructs an <code>AtCommandFailedException</code> with no
     * detail message.
     * @param type The type of the AT error
     * @param errorCode The error code of the AT command
     * @since 1.4
     */
    public AtCommandFailedException(final Type type, final short errorCode)
    {
        this.type = type;
        this.errorCode = errorCode;
    }

    /**
     * Constructs an <code>AtCommandFailedException</code> with the
     * specified detail message.
     * @param type The type of the AT error
     * @param errorCode The error code of the AT command
     * @param message  The detail message
     * @since 1.4
     */
    public AtCommandFailedException(final Type type, final short errorCode
            , final String message)
    {
        super(message);
        this.type = type;
        this.errorCode = errorCode;
    }

    /**
     * Constructs an <code>AtCommandFailedException</code> with the
     * specified detail message and takes an other <code>Throwable</code>.
     * @param type The type of the AT error
     * @param errorCode The error code of the AT command
     * @param message  The detail message
     * @param cause The other throwable
     * @since 1.0
     */
    public AtCommandFailedException(final Type type, final short errorCode
            , final String message, final Throwable cause)
    {
        super(message, cause);
        this.type = type;
        this.errorCode = errorCode;
    }

    /**
     * Constructs an <code>AtCommandFailedException</code> and takes an other 
     * <code>Throwable</code>.
     * @param type The type of the AT error
     * @param errorCode The error code of the AT command
     * @param cause The other throwable
     * @since 1.4
     */
    public AtCommandFailedException(final Type type, final short errorCode
            , final Throwable cause)
    {
        super(cause);
        this.type = type;
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the type of the AT error.
     * @return The type of the AT error
     * @since 1.4
     */
    public Type getType()
    {
        return type;
    }

    /**
     * Gets the error code of an AT command. If error code mode off then delivers
     * {@link #ERROR_CODE_MODE_OFF}.
     * @return The error code of an AT command
     * @since 1.4
     * @see #ERROR_CODE_MODE_OFF
     */
    public short getErrorCode()
    {
        return errorCode;
    }
    
    /**
     * Enum for AT error types.
     * @since 1.4
     */
    public static enum Type
    {
        ERROR, CMS, CME;
    }
}
