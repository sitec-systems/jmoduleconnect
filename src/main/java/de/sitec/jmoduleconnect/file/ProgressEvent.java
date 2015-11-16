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
package de.sitec.jmoduleconnect.file;

import java.util.EventObject;

/**
 * This class contains the data of an event from {@link ProgressListener}.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class ProgressEvent extends EventObject
{
    private final OperationType operationType;
    private final int progress;
    private final boolean done;

    /* package */ ProgressEvent(final Object source, final OperationType operationType
            ,  final int progress, final boolean done)
    {
        super(source);
        this.operationType = operationType;
        this.progress = progress;
        this.done = done;
    }

    /**
     * Gets the type of the current operation.
     * @return The type of the current operation
     * @since 1.0
     */
    public OperationType getOperationType()
    {
        return operationType;
    }

    /**
     * Gets the current progress of the operation. Value range is between 
     * <code>0</code> and <code>100</code>.
     * @return The current progress of the operation
     * @since 1.0
     */
    public int getProgress()
    {
        return progress;
    }

    /**
     * Gets the finished state of the operation.
     * @return If <code>true</code> the operation is done / If <code>false</code>
     *         and received from 
     *         {@link ProgressListener#progressDone(de.sitec.jmoduleconnect.file.ProgressEvent) }
     *         the operation has failed / If <code>false</code> and received from
     *         {@link ProgressListener#progressReceived(de.sitec.jmoduleconnect.file.ProgressEvent) }
     *         the operation is in progress
     * @since 1.0
     */
    public boolean isDone()
    {
        return done;
    }
}
