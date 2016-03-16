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
package de.sitec_systems.jmoduleconnect;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface for interpretation and parsing of protocols.
 * @author sitec systems GmbH
 * @since 1.0
 */
public interface ProtocolParser
{
    /**
     * Returns <code>true</code> if the input can interpreted by this parser.
     * @param is The {@link InputStream}
     * @return <code>true</code> if the input can interpreted by this parser
     * @throws IOException An exception at reading from {@link InputStream}
     * @since 1.0
     */
    boolean isProtocol(final InputStream is) throws IOException;
    
    /**
     * Reads and parses an protocol from {@link InputStream}.
     * @param is The {@link InputStream}
     * @throws IOException An exception at reading from {@link InputStream}
     * @since 1.0
     */
    void parse(final InputStream is) throws IOException;
}
