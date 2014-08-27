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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

/**
 * Extends the {@link FileMeta} class an represents an file with data.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class FileContent extends FileMeta
{
    private final byte[] data;
    
    /**
     * Constructor
     * @param name The name of the file
     * @param parentDirectory The parent directory of the file
     * @param lastModified The last modification date
     * @param data The binary data of the file
     * @throws IllegalArgumentException If parameter name, lastModified or data
     *         is <code>null</code>
     * @since 1.0
     */
    /* package */ FileContent(final String name, final FileMeta parentDirectory
            , final Date lastModified, final byte[] data)
    {
        super(name, parentDirectory, lastModified, data.length);
        this.data = data;
    }
    
    /**
     * This constructor is designed for put a file.
     * @param name The name of the file
     * @param lastModified The last modification date
     * @param data The binary data of the file
     * @throws IllegalArgumentException If an input value is <code>null</code>
     * @since 1.0
     */
    public FileContent(final String name, final Date lastModified, final byte[] data)
    {
        super(name, null, lastModified, data.length);
        if(data == null)
        {
            throw new IllegalArgumentException("The parameter data cant be null");
        }
        this.data = data;
    }
    
    /**
     * Constructs an instance of this class from {@link File}.
     * @param file
     * @throws FileNotFoundException If the file not exists
     * @throws IOException An error at opening file was received
     * @throws IllegalArgumentException If the parameter file is <code>null</code>
     * @since 1.0
     */
    public FileContent(final File file) 
            throws FileNotFoundException, IOException
    { 
        super();
        if(file == null)
        {
            throw new IllegalArgumentException("The parameter file cant be null");
        }
        
        if(!file.exists())
        {
            throw new FileNotFoundException("The file: " + file.getAbsolutePath() + " doesnt exist");
        }
        name = file.getName();
        lastModified = new Date(file.lastModified());
        data = new byte[(int)file.length()];
        size = data.length;
        final FileInputStream fis = new FileInputStream(file);
        fis.read(data);
        fis.close();
        this.parentDirectory = null;
        System.out.println("Intern Length: " + data.length);
    }

    /**
     * Gets the binary data of the file.
     * @return The binary data of the file
     * @since 1.0
     */
    public byte[] getData()
    {
        return data;
    }
    
}
