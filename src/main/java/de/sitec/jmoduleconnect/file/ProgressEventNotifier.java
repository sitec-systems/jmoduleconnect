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

import de.sitec.jmoduleconnect.utils.AbstractEventNotifier;
import javax.swing.event.EventListenerList;

/**
 * An notifier for {@link ProgressEvent}.
 * @author sitec systems GmbH
 * @since 1.0
 */
/* package */ class ProgressEventNotifier extends AbstractEventNotifier<ProgressEvent, ProgressListener>
{
    /**
     * Adds an {@link ProgressListener}.
     * @param progressListener The {@link ProgressListener}
     * @since 1.0
     */
    public void addProgressListener(final ProgressListener progressListener)
    {
        addEventListener(ProgressListener.class, progressListener);
    }

    /**
     * Removes an {@link ProgressListener}.
     * @param progressListener The {@link ProgressListener}
     * @since 1.0
     */
    public void removeProgressListener(final ProgressListener progressListener)
    {
        removeEventListener(ProgressListener.class, progressListener);
    }
    
    /**
     * Notifys all registred {@link ProgressListener} about a new {@link ProgressEvent}.
     * @param eventListenersList The local copy of registered event listeners
     * @param event The event
     * @since 1.0
     */
    @Override
    protected void notifyListeners(final EventListenerList eventListenerList
            , final ProgressEvent progressEvent)
    {
        for (final ProgressListener listener : eventListenerList.getListeners(ProgressListener.class))
        {
            if(progressEvent.isDone())
            {
                listener.progressDone(progressEvent);
            }
            else
            {
                listener.progressReceived(progressEvent);
            }
        }
    }
}
