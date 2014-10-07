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

import java.util.EventListener;

/**
 * An interface for receiving <b>AT</b> events from device.
 * @author sitec systems GmbH
 * @since 1.0
 */
public interface AtListener extends EventListener
{
    /**
     * Notifys about new AT event.
     * @param atEvent The event data
     * @since 1.0
     */
    void atEventReceived(final AtEvent atEvent);
}
