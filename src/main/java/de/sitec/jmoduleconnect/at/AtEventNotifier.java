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
package de.sitec.jmoduleconnect.at;

import de.sitec.jmoduleconnect.utils.AbstractEventNotifier;
import javax.swing.event.EventListenerList;

/**
 * An event notifiert for {@link AtEvent}.
 * @author sitec systems GmbH
 * @since 1.0
 */
/* package*/ class AtEventNotifier extends AbstractEventNotifier<AtEvent, AtListener>
{
    /**
     * Adds an {@link AtListener}.
     * @param atListener The {@link AtListener}
     * @since 1.0
     */
    public void addAtListener(final AtListener atListener)
    {
        addEventListener(AtListener.class, atListener);
    }

    /**
     * Removes an {@link AtListener}.
     * @param atListener The {@link AtListener}.
     * @since 1.0
     */
    public void removeAtListener(final AtListener atListener)
    {
        removeEventListener(AtListener.class, atListener);
    }
    
    /**
     * Notifys all registered {@link AtListener} about a new {@link AtEvent}.
     * @param eventListenersList The local copy of registered event listeners
     * @param event The event
     * @since 1.0
     */
    @Override
    protected void notifyListeners(final EventListenerList eventListenerList
            , final AtEvent atEvent)
    {
        for (final AtListener listener : eventListenerList.getListeners(AtListener.class))
        {
            listener.atEventReceived(atEvent);
        }
    }
}
