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
package de.sitec.jmoduleconnect.at;

/**
 * Thrown if the response of an AT command is <code>ERROR</code>.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class AtCommandFailedException extends Exception
{
    /**
     * Constructs an <code>AtCommandFailedException</code> with no
     * detail message.
     * @since 1.0
     */
    public AtCommandFailedException(){};

    /**
     * Constructs an <code>AtCommandFailedException</code> with the
     * specified detail message.
     * @param s The detail message
     * @since 1.0
     */
    public AtCommandFailedException(final String s)
    {
        super(s);
    }
    
    /**
     * Constructs an <code>AtCommandFailedException</code> with the
     * specified detail message and takes an other <code>Throwable</code>.
     * @param s The detail message
     * @param cause The other throwable
     * @since 1.0
     */
    public AtCommandFailedException(final String s, final Throwable cause)
    {
        super(s, cause);
    }
}
