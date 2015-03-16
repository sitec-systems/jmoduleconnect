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
package de.sitec.jmoduleconnect.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Set;


/**
 * An interface to handle files on the flash filesystem of the connected device.
 * @author sitec systems GmbH
 * @since 1.0
 */
public interface FileManager
{
    /**
     * Adds an {@link ProgressListener}.
     * @param progressListener The {@link ProgressListener}
     * @since 1.0
     */
    void addProgressListener(final ProgressListener progressListener);
    
    /**
     * Cancel an time intensive operation. This method is {@link Thread} safety.
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    void cancel() throws IOException;
    
    /**
     * Change the directory pointer. <b>Important:</b> drive letters only supported
     * on modules with firmware 2.004 or greater.
     * @param pathname The new path
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    void changeDirectory(final String pathname) throws IOException;
    
    /**
     * Erase the complete flash filesystem on the device. <b>Caution:</b> The 
     * operation will take a lot of time (many seconds) in dependency to the file 
     * system size.
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    void deleteAll() throws IOException;
    
    /**
     * Delete a file or folder on the flash filesystem.
     * @param filename The specific file or folder. Only files or folder on the 
     *        current directory. Paths not allowed
     * @param filledFolder If <code>true</code> and the filename is an not empty 
     *        folder then erases the folder with included files and sub folders. 
     *        If <code>false</code> and the filename is an not empty folder then
     *        throws an {@link IOException}
     * @throws IOException The parameter <code>filledFolder</code> is false and 
     *         the filename is an not empty folder or the communication to the 
     *         device failed
     * @since 1.0
     */
    void deleteFile(final String filename, final boolean filledFolder) 
            throws IOException;
    
    /**
     * Closes all resources and switch back to AT mode
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    void close() throws IOException;
    
    /**
     * Gets the complete memory capacity of the flash filesystem. This dont means
     * the free space.
     * @return The complete memory capacity in Byte
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    long getDiskSpace() throws IOException;
    
    /**
     * Gets the current directory.
     * @return The current directory
     * @since 1.0
     */
    FileMeta getCurrenctDirectory();
    
    /**
     * Transfers an file from flash filesystem to local memory. Only files 
     * allowed to transfer. 
     * @param filename The name of file
     * @return The file with data
     * @throws InterruptedIOException Thrown after an interrupt by {@link #cancel() }
     * @throws IOException The parameter filename is an directory, dont exist 
     *         or the communication to the device failed
     * @since 1.0
     */
    FileContent getFile(final String filename) 
            throws InterruptedIOException, IOException;
    
    /**
     * Gets the file listing of the current directory.
     * @return The files and folders from current directory
     * @throws IOException The communication to the device failed
     */
    Set<FileMeta> getFileListing() throws IOException;
    
    /**
     * Gets the free available space on the flash filesystem.
     * @return The free available space in Byte
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    long getFreeSpace() throws IOException;
    
    /**
     * Creates a new directory on the flash filesystem.
     * @param directoryName The name for new directory
     * @throws IOException If the directory exists or the communication to the 
     *         device failed
     * @since 1.0
     */
    void makeDirectory(final String directoryName) throws IOException;
    
    /**
     * Moves an file within the flash filesystem. Example: 
     * <code>moveFile("folderA/file.txt", "folderB/file.txt")</code>
     * @param oldPath The old path (drive letter only on Firmware 2.004 and greater)
     * @param newPath The new path (drive letter only on Firmware 2.004 and greater)
     * @throws IOException If the directory exists or the communication to the 
     *         device failed
     * @since 1.0
     */
    void moveFile(final String oldPath, final String newPath) throws IOException;
    
    /**
     * Puts the input file to the flash filesystem on the current directory.
     * @param file The file for putting
     * @param override If <code>true</code> it will override the old one on 
     *        flash filesystem. If <code>false</code> and a file with same name
     *        exists on flash file system then throws an {@link IOException}
     * @throws InterruptedIOException Thrown after an interrupt by {@link #cancel() }
     * @throws IOException If parameter override <code>false</code> and a file 
     *         with same name exists on flash filesystem or the communication to 
     *         the device failed
     * @since 1.0
     */
    void putFile(final FileContent file, final boolean override)
            throws InterruptedIOException, IOException;

    /**
     * Removes an {@link ProgressListener}.
     * @param progressListener The {@link ProgressListener}
     * @since 1.0
     */
    void removeProgressListener(final ProgressListener progressListener);
}
