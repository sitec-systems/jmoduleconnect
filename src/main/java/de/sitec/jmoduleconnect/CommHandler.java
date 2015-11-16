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
package de.sitec.jmoduleconnect;

import java.io.Closeable;
import java.io.IOException;

/**
 * An interface for the primitive communication with the device.
 * @author sitec systems GmbH
 * @since 1.0
 */
public interface CommHandler extends Closeable
{
    /**
     * Adds an <code>ProtocolParser</code> to the <code>CommHandler</code>. This
     * is necessary for receiving and parsing data from device.
     * @param protocolParser The <code>ProtocolParser</code>
     * @since 1.0
     */
    void addProtocolParser(final ProtocolParser protocolParser);
    
    /**
     * Removes an <code>ProtocolParser</code> from the <code>CommHandler</code>.
     * @param protocolParser The <code>ProtocolParser</code>
     * @since 1.0
     */
    void removeProtocolParser(final ProtocolParser protocolParser);
    
    /**
     * Sends data to the connected device.
     * @param data The data
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    void send(final byte[] data) throws IOException;
}
