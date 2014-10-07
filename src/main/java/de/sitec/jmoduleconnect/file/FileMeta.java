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

import java.util.Date;

/**
 * An instance of this class represents an file or an directory. It contains only
 * meta informations.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class FileMeta implements Comparable<FileMeta>
{
    protected String name;
    protected Date lastModified;
    protected boolean directory;
    protected int size;
    protected FileMeta parentDirectory;
    
    /* package */ static final FileMeta ROOT_DIRECTORY = new FileMeta("a:", null, true);

    protected FileMeta(){}
    
    /**
     * Constructor
     * @param name The name of the file or directory
     * @param parentDirectory The parent directory of the file or directory
     * @throws IllegalArgumentException If parameter name or lastModified <code>
     *         null</code>
     * @since 1.0
     */
    protected FileMeta(final String name, final FileMeta parentDirectory)
    {
        this(name, parentDirectory, new Date(), 0);
    }
    
    /**
     * Constructor
     * @param name The name of the file or directory
     * @param parentDirectory The parent directory of the file or directory
     * @param directory Is directory or file
     * @throws IllegalArgumentException If parameter name or lastModified <code>
     *         null</code>
     * @since 1.0
     */
    protected  FileMeta(final String name, final FileMeta parentDirectory
            , final boolean directory)
    {
        this(name, parentDirectory, directory, new Date(), 0);
    }

    /**
     * Constructor
     * @param name The name of the file or directory
     * @param parentDirectory The parent directory of the file or directory
     * @param lastModified The last modification date
     * @param size The size of the file
     * @throws IllegalArgumentException If parameter name or lastModified <code>
     *         null</code>
     * @since 1.0
     */
    protected  FileMeta(final String name, final FileMeta parentDirectory
            , final Date lastModified, final int size)
    {
        this(name, parentDirectory, false, lastModified, size);
    }
    
    /**
     * Constructor
     * @param name The name of the file or directory
     * @param parentDirectory The parent directory of the file or directory
     * @param directory Is directory or file
     * @param lastModified The last modification date
     * @param size The size of the file
     * @throws IllegalArgumentException If parameter name or lastModified <code>
     *         null</code>
     * @since 1.0
     */
    protected  FileMeta(final String name, final FileMeta parentDirectory
            , final boolean directory, final Date lastModified, final int size)
    {
        if(name == null || lastModified == null)
        {
            throw new IllegalArgumentException("The parameters name and lastModified cant be null");
        }
        this.name = name;
        this.lastModified = lastModified;
        this.directory = directory;
        this.size = size;
        this.parentDirectory = parentDirectory;
    }

    /**
     * Gets an {@link Date} thats contains the information about the last 
     * modification on the file or directory.
     * @return The last modification date
     * @since 1.0
     */
    public Date getLastModified()
    {
        return lastModified;
    }

    /**
     * Gets the name of the file or directory.
     * @return The name of the file or directory
     * @since 1.0
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the parent directory of the file or directory. If the return value
     * <code>null</code> then represents the object the root directory.
     * @return The parent directory of the file or directory
     * @since 1.0
     */
    public FileMeta getParentDirectory()
    {
        return parentDirectory;
    }

    /**
     * Gets the size of an file. If the object an directory then is size always
     * <code>0</code>.
     * @return The size of an file
     * @since 1.0
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Gets the information is this object an directory or a file.
     * @return <code>true</code> - is directory / <code>false</code> - is file
     * @since 1.0
     */
    public boolean isDirectory()
    {
        return directory;
    }
    
    /** {@inheritDoc } */
    @Override
    public int compareTo(final FileMeta otherFileModule)
    {
        final int result;
        if((directory && otherFileModule.directory)
                || (!directory && !otherFileModule.directory))
        {
            result = name.toLowerCase().compareTo(otherFileModule.name.toLowerCase());
        }
        else if(directory && !otherFileModule.directory)
        {
            result = -1;
        }
        else
        {
            result = 1;
        }
        
        return result;
    }
}
