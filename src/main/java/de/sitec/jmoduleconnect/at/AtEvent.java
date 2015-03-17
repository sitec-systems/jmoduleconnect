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
package de.sitec.jmoduleconnect.at;

import java.util.EventObject;

/**
 * This class contains the data of an event from {@link AtListener}.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class AtEvent extends EventObject
{
    private final String data;

    /* package */ AtEvent(final Object source, final String data)
    {
        super(source);
        this.data = data;
    }

    /**
     * The received AT data from module.
     * @return The received AT data from module
     * @since 1.0
     */
    public String getData()
    {
        return data;
    }

    @Override
    public String toString()
    {
        return getData();
    }
    
    
}
