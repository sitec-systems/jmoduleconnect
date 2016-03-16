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
package de.sitec_systems.jmoduleconnect.utils;

import java.util.EventListener;
import java.util.EventObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.event.EventListenerList;
import org.slf4j.LoggerFactory;

/**
 * An abstract class for multithreading safe using of listeners.
 * @author sitec systems GmbH
 * @since 1.0
 */
public abstract class AbstractEventNotifier<E extends EventObject, L extends EventListener> implements Runnable
{
    private final EventListenerList listeners;
    private final BlockingQueue<E> notifyMessages;
    private final ReadWriteLock lockObj = new ReentrantReadWriteLock();
    private final Lock readLock = lockObj.readLock();
    private final Lock writeLock = lockObj.writeLock();
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AbstractEventNotifier.class);

    protected AbstractEventNotifier()
    {
        notifyMessages = new LinkedBlockingQueue<E>();
        listeners = new EventListenerList();
    }
    
    /**
     * Adds an event to the notifier queue.
     * @param event The event
     * @since 1.0
     */
    public void addEvent(final E event)
    {
        notifyMessages.add(event);
    }
    
    /**
     * Adds an listener.
     * @param clazz The type of the listener
     * @param eventListener The listener
     * @since 1.0
     */
    protected void addEventListener(final Class<L> clazz, final L eventListener)
    {
        writeLock.lock();
        try
        {
            listeners.add(clazz, eventListener);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Removes an listener
     * @param clazz The type of the listener
     * @param eventListener The listener
     * @since 1.0
     */
    protected void removeEventListener(final Class<L> clazz, final L eventListener)
    {
        writeLock.lock();
        try
        {
            listeners.remove(clazz, eventListener);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Takes an local copy of the {@link EventListenerList} and pass it to the
     * {@link #notifyListeners(javax.swing.event.EventListenerList, java.util.EventObject) }
     * method.
     * @param event The event
     * @since 1.0
     */
    private void notifyEventListeners(final E event)
    {
        EventListenerList eventListenerList = null;
        readLock.lock();
        try
        {
            eventListenerList = listeners;
        }
        finally
        {
            readLock.unlock();
        }
        
        notifyListeners(eventListenerList, event);
    }
    
    /**
     * Must be overridden by an implementaion. The parameter eventListenerList is
     * an local copy and therewith thread safety.
     * @param eventListenersList The local copy of registered event listeners
     * @param event The event
     * @since 1.0
     */
    protected abstract void notifyListeners(final EventListenerList eventListenersList
            , final E event);
    
    /**
     * Waits for new events and after receiving an new event it will call the 
     * method {@link #notifyEventListeners(java.util.EventObject) }. Finish it
     * with {@link Thread#interrupt() }.
     * @since 1.0
     */
    @Override
    public void run()
    {
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                final E event = notifyMessages.take();
                notifyEventListeners(event);
            }
            catch (final InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                LOG.debug("EventNotifier was finished");
            }
        }
    }
}
