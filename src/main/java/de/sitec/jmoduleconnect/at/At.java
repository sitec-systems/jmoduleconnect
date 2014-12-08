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

import de.sitec.jmoduleconnect.file.ModuleFileManager;
import de.sitec.jmoduleconnect.ProtocolParser;
import java.io.Closeable;
import java.io.IOException;

/**
 * An interface for the <b>AT</b> based communicaton with the connected device.
 * @author sitec systems GmbH
 * @since 1.0
 */
public interface At extends ProtocolParser, Closeable
{
    /**
     * Adds an {@link AtListener}.
     * @param atListener The {@link AtListener}
     * @since 1.0
     */
    void addAtListener(final AtListener atListener);
    
    /**
     * Close an non AT mode on device with <code>+++</code>. The class 
     * {@link ModuleFileManager} use this mehtod automatically.
     * @throws AtCommandFailedException The response from device contains 
     *         <code>ERROR</code>
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    void closeMode() throws AtCommandFailedException, IOException;
    
    /**
     * Removes an {@link AtListener}.
     * @param atListener The {@link AtListener}.
     * @since 1.0
     */
    void removeAtListener(final AtListener atListener);
    
    /**
     * Sends a AT command to a connected device.
     * @param atCommand The AT command. An <code>\r</code> is not necessary
     * @return The response to sent command
     * @throws AtCommandFailedException The response from device contains 
     *         <code>ERROR</code>
     * @throws IOException The communication to the device failed
     * @throws IllegalArgumentException The parameter atCommand is <code>null</code>
     *         or dont start with <code>AT</code>
     * @since 1.0
     */
    String send(final String atCommand) 
            throws AtCommandFailedException, IOException;
}
