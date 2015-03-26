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
package de.sitec.jmoduleconnect.file;

import de.sitec.jmoduleconnect.ProtocolParser;
import de.sitec.jmoduleconnect.utils.BinaryUtils;
import de.sitec.jmoduleconnect.CommHandler;
import de.sitec.jmoduleconnect.at.AtCommandFailedException;
import de.sitec.jmoduleconnect.file.ObexHeader.Code;
import de.sitec.jmoduleconnect.at.At;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link FileManager} interface for module flash filesystem.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class ModuleFileManager implements FileManager, ProtocolParser
{
    private final CommHandler commHandler;
    private final At at;
    private final ProgressEventNotifier eventNotifier;
    private final Thread eventNotifierThread;
    private FileMeta currentDirectory;
    private volatile boolean obexMode;
    private byte progress;
    private Set<FileMeta> currentFileListing;
    private Obex obexRepsonse;
    private final Lock responseLock = new ReentrantLock();
    private final Condition responseAvailable = responseLock.newCondition();
    private final Lock operationLock = new ReentrantLock();
    private final Lock interruptLock = new ReentrantLock(true);
    private volatile boolean running;
    private volatile boolean interruptable;
    private ByteArrayOutputStream responseBuffer;
    
    private static final Logger LOG = LoggerFactory.getLogger(ModuleFileManager.class);
    private static final byte DEFAULT_SLEEP_MILLIS = 10;
    private static final short OBEX_RESPONSE_TIMEOUT = 5000;
    private static final byte WAIT_TIMEOUT = 2;
    private static final byte WAIT_TIMEOUT_DELETE_ALL = 30;
    private static final byte WAIT_TRAILS = 3;
    private static final Charset BYTE_CHARSET = Charset.forName("ISO_8859_1");
    private static final Charset NAME_CHARSET = Charset.forName("UTF_16BE");
    private static final short MAX_PACKET_LENGTH = 512;

    private ModuleFileManager(final CommHandler commHandler, final At at)
    {
        this.commHandler = commHandler;
        this.at = at;     
        eventNotifier = new ProgressEventNotifier();
        eventNotifierThread = new Thread(eventNotifier);
    }
    
    /**
     * Creates an instance of this class.
     * @param commHandler The communication handler
     * @param at For the AT communication
     * @return The instance of this class
     * @throws AtCommandFailedException The response from device contains 
     *         <code>ERROR</code>
     * @throws IOException The communication to the device failed
     * @throws IllegalArgumentException If the input values ar <code>null</code>
     * @since 1.0
     */
    public static final FileManager createFileManager(final CommHandler commHandler
            , final At at) throws AtCommandFailedException, IOException 
    {
        if(commHandler == null || at == null)
        {
            throw new IllegalArgumentException("The input values cant be null");
        }
        
        ModuleFileManager fileManager = new ModuleFileManager(commHandler, at);
        try
        {
            fileManager.init();
            
            return fileManager;
        }
        catch (final AtCommandFailedException ex)
        {
            fileManager.close();
            throw ex;
        }
        catch (final IOException ex)
        {
            fileManager.close();
            throw ex;
        }
    }
    
    private void init() throws AtCommandFailedException, IOException
    {
        at.send("AT");
//        this.at.sendAtCommand("ATE");
        at.send("AT");
        at.send("AT");
        at.send("ATI");
                
        eventNotifierThread.start();
        
        openObexMode();
        readFileListing();
    }

    /**
     * If the object in the OBEX mode all incoming data will be interpreted as
     * OBEX response.
     * @param is The {@link InputStream}
     * @return <code>true</code> if the object in OBEX mode
     * @throws IOException An exception at reading from {@link InputStream}
     * @since 1.0
     */
    @Override
    public boolean isProtocol(final InputStream is) throws IOException
    {
//        System.out.println("Obex: " + Integer.toHexString(is.read()));
//        is.reset();
        return obexMode;
    }

    /** {@inheritDoc } */
    @Override
    public void parse(final InputStream is) throws IOException
    {
        final Obex obexRespTemp = receiveObex(is);
        responseLock.lock();
        try
        {
            obexRepsonse = obexRespTemp;
            responseAvailable.signalAll();
        }
        finally
        {
            responseLock.unlock();
        }
    }
    
    private void send(final Obex request) throws IOException
    {
        LOG.trace("OBEX send - RAW: {}", BinaryUtils.toHexString(request.toByteArray()));
        LOG.debug("OBEX send: {}", request);
        commHandler.send(request.toByteArray());
    }
    
    /**
     * Waits for OBEX response from device.
     * @param waitForDeleteAll <code>true</code> - Wait longer for the {@link #deleteAll() }
     *        operation / <code>false</code> - Wait default time for a reponse
     * @return The OBEX response
     * @throws IOException An error at receiving OBEX response
     * @since 1.4
     * @see #deleteAll() 
     */
    private Obex receive(final boolean waitForDeleteAll) throws IOException
    {
        final byte waitTimeout;
        if(waitForDeleteAll)
        {
            waitTimeout = WAIT_TIMEOUT_DELETE_ALL;
        }
        else
        {
            waitTimeout = WAIT_TIMEOUT;
        }
        
        Obex response = null;
        byte waitTrails = 0;

        responseLock.lock();
        try
        {
            while(obexRepsonse == null && waitTrails < WAIT_TRAILS)
            {
                try
                {
                    responseAvailable.await(waitTimeout, TimeUnit.SECONDS);
                }
                catch (final InterruptedException ex)
                {
                    ex.printStackTrace();
                }
                waitTrails++;
            }
            response = obexRepsonse;
            obexRepsonse = null;
        }
        finally
        {
            responseLock.unlock();
        }
        
        if(response == null)
        {
            throw new IOException("No OBEX response after receiving");
        }
        
        LOG.trace("OBEX receive - RAW: {}", BinaryUtils.toHexString(response.toByteArray()));
        LOG.debug("OBEX receive: {}", response);
        
        return response;
    }
    
    /**
     * Switches the module from AT to OBEX mode and deregisters the AT parser from
     * {@link CommHandler}.
     * @throws IOException The switching to OBEX mode failed
     * @since 1.0
     */
    private void openObexMode() throws IOException 
    {
        try
        {
            at.send("AT\\Q3");
            at.send("AT^SQWE=0");
            at.send("AT^SQWE=3");
            
            commHandler.removeProtocolParser(at);
            commHandler.addProtocolParser(this);
            obexMode = true;

            final byte[] fsUid = new byte[] {(byte)0x6b, (byte)0x01, (byte)0xcb
                    , (byte) 0x31, (byte) 0x41, (byte) 0x06, (byte) 0x11
                    , (byte) 0xd4, (byte) 0x9a, (byte) 0x77, (byte) 0x00
                    , (byte) 0x50, (byte) 0xda, (byte) 0x3f, (byte) 0x47
                    , (byte) 0x1f };

            final ObexHeader<byte[]> target = new ObexHeader<byte[]>(ObexHeader.Code.TARGET, fsUid);

            final Obex req = new Obex(Obex.Code.REQUEST_CONNECT, target);

            send(req);
            final Obex response = receive(false);
            if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
                throw new IOException("Error response received: " + response.getObexCode()); 
            }
            
            currentDirectory = FileMeta.ROOT_DIRECTORY;
            progress = -1;
            
            LOG.debug("OBEX Mode");
        }
        catch (final AtCommandFailedException ex)
        {
            throw new IOException("AT commands for opening OBEX mode failed", ex);
        }
    }
    
    private boolean getRunning()
    {
        interruptLock.lock();
        try
        {
            return running;
        }
        finally
        {
            interruptLock.unlock();
        }
    }
    
    private void setRunning(final boolean runnning)
    {
        interruptLock.lock();
        try
        {
            this.running = runnning;
        }
        finally
        {
            interruptLock.unlock();
        }
    }

    /** {@inheritDoc } */
    @Override
    public void cancel() throws IOException
    {
        interruptLock.lock();
        try
        {
            if(obexMode && interruptable)
            {
                setRunning(false);
                final Obex req = new Obex(Obex.Code.REQUEST_ABORT);
                send(req);
                final Obex response = receive(false);

                if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
                {
                    throw new IOException("Error response received at operation canceling: " + response.getObexCode()); 
                }
                
                LOG.debug("Before Listing");
                readFileListing();
                LOG.debug("Abort Message was send");
            }
        }
        finally
        {
            interruptLock.unlock();
        }
    }
    
    /** {@inheritDoc } */
    @Override
    public void addProgressListener(final ProgressListener progressListener)
    {
        eventNotifier.addProgressListener(progressListener);
    }

    /** {@inheritDoc } */
    @Override
    public void removeProgressListener(final ProgressListener progressListener)
    {
        eventNotifier.removeProgressListener(progressListener);
    }
    
    private synchronized void notifyProgress(final OperationType opType
            , final int progress)
    {
        if(this.progress != progress)
        {
            final ProgressEvent progressEvent = new ProgressEvent(this, opType, progress, false);
            eventNotifier.addEvent(progressEvent);
            this.progress = (byte)progress;
        }
    }
    
    private synchronized void notifyProgressDone(final OperationType opType
            ,final boolean done)
    {
        final ProgressEvent progressEvent = new ProgressEvent(this, opType, progress, done);
        eventNotifier.addEvent(progressEvent);
        progress = -1;
    }
    
    /**
     * Switches the module from OBEX to AT mode and register the AT parser again
     * at {@link CommHandler}.
     * @throws IOException The switching to AT mode failed
     * @since 1.0
     */
    private void closeObexMode() throws IOException
    { 
        final Obex req = new Obex(Obex.Code.REQUEST_DISCONNECT);
        
        send(req);
        final Obex response = receive(false);
        
        if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
        {
            throw new IOException("Error response received at closing obex mode: " + response.getObexCode()); 
        }
        
        obexMode = false;
        commHandler.removeProtocolParser(this);
        commHandler.addProtocolParser(at);
        
        try
        {
            at.closeMode();
            at.send("ATE1");
        }
        catch (final AtCommandFailedException ex)
        {
            throw new IOException("AT commands for closing OBEX mode failed", ex);
        }
        LOG.debug("AT Mode");
    }
    
    /** {@inheritDoc } */
    @Override
    public void close() throws IOException
    {
        operationLock.lock();
        interruptLock.lock();
        try
        {
            eventNotifierThread.interrupt();
            if(obexMode)
            {
                LOG.debug("Disconnecting");
                closeObexMode();
            }
            commHandler.removeProtocolParser(this);
        }
        finally
        {
            interruptLock.unlock();
            operationLock.unlock();
        }
    }

    /** {@inheritDoc } */
    @Override
    public FileMeta getCurrenctDirectory()
    {
        FileMeta result = null;
        operationLock.lock();
        try
        {
            result = currentDirectory;
        }
        finally
        {
            operationLock.unlock();
        }
        return result;
    }
    
    /** {@inheritDoc } */
    @Override
    public void makeDirectory(final String directoryName) throws IOException
    {
        if(directoryName.contains("/") || directoryName.contains("\\"))
        {
            throw new IllegalArgumentException("Paths not allowed");
        }
        operationLock.lock();
        try
        {
            setPath(directoryName, true);
            setPath("..", false);
        }
        finally
        {
            operationLock.unlock();
        }
    }

    /** {@inheritDoc } */
    @Override
    public void changeDirectory(final String pathname) throws IOException
    {
        if(pathname == null)
        {
            throw new IllegalArgumentException("Parameter pathname cant be null");
        }
        String parameter = pathname;
        operationLock.lock();
        try
        {
            FileMeta directory = currentDirectory;
            if(pathname.contains(":"))
            {
                parameter = pathname.toUpperCase();
                directory = null;
            }
            setPath(parameter, false);
            currentDirectory = new FileMeta(pathname, directory, true);
        }
        finally
        {
            operationLock.unlock();
        }
    }
    
    private void setPath(final String pathname, final boolean create) 
            throws IOException
    {
        if(create)
        {
            checkDirectoryExist(pathname);
        }
        
        if(!obexMode) openObexMode();
        final Obex req;
        final byte flags;
        if (pathname == null || pathname.length() == 0 || pathname.equals("..")) 
        {
            flags = Obex.Code.FLAG_SETPATH_PARENT_FOLDER.getCode();
            req = new Obex(Obex.Code.REQUEST_SETPATH, flags);
        }
        else
        {
            final ObexHeader<String> header = new ObexHeader<String>(ObexHeader.Code.NAME, pathname);
            if(create)
            {
                flags = Obex.Code.FLAG_SETPATH_CREATE.getCode();
            }
            else
            {
                flags = Obex.Code.FLAG_SETPATH_NOCREATE.getCode();
            }
            req = new Obex(Obex.Code.REQUEST_SETPATH, header, flags);
        }
        
        interruptLock.lock();
        try
        {
            send(req);
            final Obex response = receive(false);
            if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
            throw new IOException("Error response received: " + response.getObexCode()); 
            }
        }
        finally
        {
            interruptLock.unlock();
        }
        readFileListing();
    }

    /** {@inheritDoc } */
    @Override
    public void deleteAll() throws IOException
    {
        operationLock.lock();
        LOG.debug("Start delete all");
        try
        {
            if(!obexMode) openObexMode();

            final byte[] param = new byte[]{(byte)0x31, (byte)0x00};
            final ObexHeader<byte[]> appParams = new ObexHeader<byte[]>(Code.APP_PARAMETERS, param);

            final Obex req = new Obex(Obex.Code.REQUEST_PUT_FINAL, appParams);

            send(req);
            final Obex response = receive(true);
            if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
                throw new IOException("Error response received at deleting all files: " + response.getObexCode()); 
            }

            readFileListing();
            LOG.debug("Finish delete all");
        }
        finally
        {
            operationLock.unlock();
        }
    }
    
    /** {@inheritDoc } */
    @Override
    public void deleteFile(final String filename, final boolean filledFolder) 
            throws IOException
    {
        FileMeta target = null;
        operationLock.lock();
        try
        {
            for(final FileMeta file: currentFileListing)
            {
                if(file.getName().equalsIgnoreCase(filename))
                {
                    target = file;
                }
            }

            if(target != null)
            {
                if(target.isDirectory())
                {
                    changeDirectory(filename);
                    if(currentFileListing.isEmpty())
                    {
                        changeDirectory("..");
                        delete(filename);
                    }
                    else
                    {
                        if(filledFolder)
                        {
                            for(final FileMeta file: currentFileListing)
                            {
                                deleteFile(file.getName(), true);
                            }
                            changeDirectory("..");
                            delete(filename);
                            currentFileListing.remove(target);
                        }
                        else
                        {
                            changeDirectory("..");
                            throw new IOException("Folder: " + filename 
                                    + " is not empty");
                        }
                    }
                }
                else
                {
                    delete(filename);
                }
            }
            else
            {
                throw new IOException("File/Folder: " + filename 
                        + " dont exist on flash");
            }
        }
        finally
        {
            operationLock.unlock();
        }
    }
    
    private void delete(final String filename) throws IOException
    {
        if(!obexMode) openObexMode();
     
        final ObexHeader<String> name = new ObexHeader<String>(ObexHeader.Code.NAME, filename);
        final Obex req = new Obex(Obex.Code.REQUEST_PUT_FINAL, name);
        
        interruptLock.lock();
        try
        {
            send(req);
            final Obex response = receive(false);

            if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
                throw new IOException("Error response received at deleting file: " + response.getObexCode()); 
            }
        }
        finally
        {
            interruptLock.unlock();
        }
    }
    
    /**
     * Sends an OBEX message their receives OBEX repsonse thats need more then 
     * one frame and continue messages after each frame.
     * @param request The request to send
     * @param opType The type of operation
     * @return The received binary data
     * @throws InterruptedIOException Thrown after an interrupt by {@link #cancel() }
     * @throws IOException The parameter filename is an directory, dont exist 
     *         or the communication to the device failed
     * @since 1.0
     */
    private byte[] processObexMultipart(final Obex request, final OperationType opType) 
            throws InterruptedIOException, IOException 
    {
        if(opType != null) notifyProgress(opType, 0);

        int writeCount = 0;
        
        Obex response = null;
        
        interruptLock.lock();
        
        setRunning(true);
        interruptable = true;
        
        try
        {
            send(request);

            response = receive(false);

            if(response.getObexCode() != Obex.Code.RESPONSE_CONTINUE &&
                    response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
                throw new IOException("Error response received: " + response.getObexCode()); 
            }
        }
        finally
        {
            interruptLock.unlock();
        }
        
        
        
        final ObexHeader<Integer> lengthHeader = response.getHeader(Code.LENGTH);
        
        final double length = lengthHeader.getData();
        
        final ByteBuffer byteBuffer = ByteBuffer.allocate(lengthHeader.getData());
        
        ObexHeader<byte[]> bodyHeader = response.getHeader(Code.BODY);
        if(bodyHeader != null)
        {
            byteBuffer.put(bodyHeader.getData());
            writeCount = bodyHeader.getData().length;
        }
        
        while(response.getObexCode() == Obex.Code.RESPONSE_CONTINUE && getRunning())
        {
            final Obex req = new Obex(Obex.Code.REQUEST_GET);
            
            interruptLock.lock();
            try
            {
                send(req);
                response = receive(false);

                if(response.getObexCode() != Obex.Code.RESPONSE_CONTINUE &&
                        response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
                {
                    throw new IOException("Error response received: " + response.getObexCode()); 
                }
            }
            finally
            {
                interruptLock.unlock();
            }

            bodyHeader = response.getHeader(Code.BODY);
            if(bodyHeader != null)
            {
                byteBuffer.put(bodyHeader.getData());
                writeCount += bodyHeader.getData().length;
            }
            
            
            double preValue = writeCount * 99;
            if(opType != null) notifyProgress(opType, (int)(preValue / length));
        }
        
        if(!getRunning())
        {
            final InterruptedIOException ex = new InterruptedIOException(opType + " was interrupted by user");
            ex.bytesTransferred = writeCount;
            throw ex;
        }
        
        setRunning(false);
        interruptable = false;
        

        if(opType != null) notifyProgress(opType, 100);
        
        return byteBuffer.array();
    }

    /** {@inheritDoc } */
    @Override
    public FileContent getFile(final String filename) 
            throws InterruptedIOException, IOException
    {
        FileContent result = null;
        operationLock.lock();
        try
        {
            if(!obexMode) openObexMode();

            if(!checkFileExist(filename))
            {
                throw new IOException("Input value: " + filename 
                        + " is an folder or dont exist");
            }

            final ObexHeader<String> name = new ObexHeader<String>(ObexHeader.Code.NAME, filename);
            final Obex req = new Obex(Obex.Code.REQUEST_GET_FINAL, name);

            final byte[] body = processObexMultipart(req, OperationType.GET_FILE);

            notifyProgressDone(OperationType.GET_FILE, true);

            result = new FileContent(filename, currentDirectory, new Date(), body);
        }
        finally
        {
            operationLock.unlock();
        }
        
        return result;
    }
    
    /**
     * Checks in the current filelisting an directory with same name like 
     * directoryName exists.
     * @param directoryName The name of the directory
     * @throws IOException If an directory with same name exists
     * @since 1.0
     */
    private void checkDirectoryExist(final String directoryName) throws IOException
    {
        final FileMeta fileMeta = getFileMetaFromListing(directoryName);
        
        if(fileMeta != null)
        {
            throw new IOException("The directory: " + directoryName + " exist");
        }
    }
    
    /**
     * Checks in the current filelisting if the input value exist and is not an
     * folder.
     * @param filename The filename
     * @return <code>true</code> its an file and exist
     * @since 1.0
     */
    private boolean checkFileExist(final String filename)
    {
        final FileMeta fileMeta = getFileMetaFromListing(filename);
        
        if(fileMeta != null)
        {
            return !fileMeta.isDirectory();
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Searchs an file or folder in the current file listing.
     * @param filename The name of the file or folder
     * @return If <code>null</code> no passing element in file listing else the
     *         passing {@link FileMeta}
     * @since 1.0
     */
    private FileMeta getFileMetaFromListing(final String filename)
    {
        FileMeta target= null;
        for(final FileMeta fileMeta: currentFileListing)
        {
            if(fileMeta.getName().equalsIgnoreCase(filename))
            {
                target = fileMeta;
            }
        }
        
        return target;
    }

    /** {@inheritDoc } */
    @Override
    public Set<FileMeta> getFileListing() throws IOException
    {
        operationLock.lock();
        try
        {
            return currentFileListing;
        }
        finally
        {
            operationLock.unlock();
        }
    }
    
    /**
     * Reads the file listing from module an save it in an member.
     * @throws InterruptedIOException Thrown after an interrupt by {@link #cancel() }
     * @throws IOException The parameter filename is an directory, dont exist 
     *         or the communication to the device failed
     * @since 1.0
     */
    private void readFileListing() throws InterruptedIOException, IOException
    {
        if(!obexMode) openObexMode();

        final ObexHeader<String> type = new ObexHeader<String>(ObexHeader.Code.TYPE, "x-obex/folder-listing");
        final Obex req = new Obex(Obex.Code.REQUEST_GET_FINAL, type);

        final byte[] body = processObexMultipart(req, null);
        final String xml = new String(body, BYTE_CHARSET);
        
        LOG.trace("FileListing - XML: {}", xml);

        try
        {
            currentFileListing = parseFileData(xml);
        }
        catch (final ParseException ex)
        {
            throw new IOException(ex);
        }
        catch (final XMLStreamException ex)
        {
            throw new IOException(ex);
        }
    }
    
    /** {@inheritDoc } */
    @Override
    public void moveFile(final String oldPath, final String newPath) throws IOException
    {
        if(oldPath == null || newPath == null)
        {
            throw new IllegalArgumentException("The input values cant be null");
        }
        operationLock.lock();
        try
        {
            final byte[] oldPathByteArr = oldPath.getBytes(NAME_CHARSET);
            final byte[] newPathByteArr = newPath.getBytes(NAME_CHARSET);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(oldPathByteArr.length + newPathByteArr.length + 10);
            bos.write(new byte[]{0x34, 0x04, 0x6D, 0x6F, 0x76, 0x65});
            bos.write(0x35);
            bos.write(oldPathByteArr.length);
            bos.write(oldPathByteArr);
            bos.write(0x36);
            bos.write(newPathByteArr.length);
            bos.write(newPathByteArr);
            byte[] param = bos.toByteArray();
            bos.close();

            final ObexHeader<byte[]> appHeader = new ObexHeader<byte[]>(Code.APP_PARAMETERS, param);
            final Obex req = new Obex(Obex.Code.REQUEST_PUT_FINAL, appHeader);
            
            interruptLock.lock();
            try
            {
                send(req);
                final Obex response = receive(false);

                if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
                {
                    throw new IOException("Error response received: " + response.getObexCode()); 
                }
            }
            finally
            {
                interruptLock.unlock();
            }

            readFileListing();
        }
        finally
        {
            operationLock.unlock();
        }
    }
    
    private long getSpace(final byte parameter) throws IOException
    {
        ObexHeader<byte[]> appHeaderResp = null;
        operationLock.lock();
        try
        {
            if(!obexMode) openObexMode();
            final ObexHeader<byte[]> appHeader = new ObexHeader<byte[]>(Code.APP_PARAMETERS, new byte[]{0x32, 0x01, parameter});
            final Obex req = new Obex(Obex.Code.REQUEST_PUT_FINAL, appHeader);

            interruptLock.lock();
            try
            {
                send(req);
                final Obex response = receive(false);
                if(response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
                {
                    throw new IOException("Error response received: " + response.getObexCode()); 
                }
                appHeaderResp = response.getHeader(Code.APP_PARAMETERS);
            }
            finally
            {
                interruptLock.unlock();
            }
        }
        finally
        {
            operationLock.unlock();
        }
        
        return BinaryUtils.byteArrToInt(appHeaderResp.getData(), false, 2);
    }

    /** {@inheritDoc } */
    @Override
    public long getDiskSpace() throws IOException
    {
        return getSpace((byte)0x01);
    }
    
    /** {@inheritDoc } */
    @Override
    public long getFreeSpace() throws IOException
    {
        return getSpace((byte)0x02);
    }
    
    /**
     * Parses an {@link Set} of {@link FileMeta} from the received file listing 
     * XML.
     * @param xml THe file listing XML
     * @return The {@link Set} of {@link FileMeta}
     * @throws XMLStreamException An error at readin the XML
     * @throws ParseException An error at parsing the XML
     * @since 1.0
     */
    private Set<FileMeta> parseFileData(final String xml) throws XMLStreamException, ParseException
    {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader parser = factory.createXMLStreamReader(new StringReader(xml));

        final Set<FileMeta> files = new TreeSet<FileMeta>();
        
        while (parser.hasNext())
        {
            switch (parser.getEventType())
            {
                case XMLStreamConstants.START_ELEMENT:
                    if((parser.getLocalName().equals("file") 
                            || parser.getLocalName().equals("folder"))
                            & !parser.getAttributeValue(0).equals("telecom") )
                    {
                        final String name = parser.getAttributeValue(0);
                        
                        final FileMeta file;
                        if(parser.getLocalName().equals("file"))
                        {
                            final Date date = Obex.DATE_FORMAT.parse(parser.getAttributeValue(2));
                            final int size = Integer.parseInt(parser.getAttributeValue(1));
                            file = new FileMeta(name, currentDirectory, false, date, size);
                        }
                        else
                        {
                            final Date date = Obex.DATE_FORMAT.parse(parser.getAttributeValue(1));
                            file = new FileMeta(name, currentDirectory, true, date, 0);
                        }           
                        
                        files.add(file);
                    }
                    break;
                default:
                    break;
            }
            parser.next();
        }
        
        return files;
    }

    /** {@inheritDoc } */
    @Override
    public void putFile(final FileContent file, final boolean override) 
            throws InterruptedIOException, IOException
    {
        operationLock.lock();
        try
        {
            FileMeta target= null;
            for(final FileMeta fileMeta: currentFileListing)
            {
                if(fileMeta.getName().equalsIgnoreCase(file.getName()))
                {
                    target = fileMeta;
                }
            }

            if(target != null)
            {
                if(override)
                {
                    deleteFile(target.getName(), true);
                }
                else
                {
                    throw new IOException("The file: " + file.getName() 
                            + " exist on flash");
                }
            }

            if(file.getData().length > getFreeSpace())
            {
                throw new IOException("Not enough space available on flash");
            }

            setRunning(true);
            interruptable = true;
            notifyProgress(OperationType.PUT_FILE, 0);
            if(!obexMode) openObexMode();

            final double fileSize = file.getData().length;
            final double filePartLength = MAX_PACKET_LENGTH;
            final double parts = fileSize / filePartLength;
            final double partsRounded = Math.ceil(parts);
            final double partPercent;

            if(parts < 1)
            {
                partPercent = 99;
            }
            else
            {
                partPercent = 99 / partsRounded;
            }
            
            int part = 1;

    //        System.out.println("PartPercent: " + partPercent);
    //        System.out.println("Parts: " + parts);    

            int maxPartLength = MAX_PACKET_LENGTH;

            putFirstPart(file);

            int writeCount = 0;

            while(writeCount < file.getData().length && getRunning())
            {
                final boolean isLastPart;
                int contentPartLength = file.getData().length - writeCount;

                if(contentPartLength > maxPartLength)
                {
                    contentPartLength = maxPartLength;
                    isLastPart = false;
                } 
                else 
                {
                    isLastPart = true;
                }
                putFilePart(file, writeCount, contentPartLength, isLastPart);
                notifyProgress(OperationType.PUT_FILE, (int)(partPercent * part));
                part++;
                writeCount += contentPartLength;
            }

            interruptable = false;
            if(!getRunning())
            {
                deleteFile(file.getName(), false);
                notifyProgressDone(OperationType.PUT_FILE, false);
                final InterruptedIOException ex = new InterruptedIOException("Put file was interrupted by user");
                ex.bytesTransferred = writeCount;
                throw ex;
            }
    //        
            setRunning(false);
        }
        finally
        {
            operationLock.unlock();
        }
        
        readFileListing();

        notifyProgress(OperationType.PUT_FILE, 100);
        notifyProgressDone(OperationType.PUT_FILE, true);
    }
    
    /**
     * Sends the first frame of an PUT operation to device. The frame contains
     * only meta data then data in the first frame makes the operation very slow.
     * @param file The file for PUT operation
     * @throws IOException An error at sending OBEX frame
     * @since 1.0
     */
    private void putFirstPart(final FileContent file) throws IOException
    {
        // HEADER NAME
        final ObexHeader<String> name = new ObexHeader<String>(ObexHeader.Code.NAME, file.getName());
        // HEADER LENGTH
        final ObexHeader<Integer> length = new ObexHeader<Integer>(ObexHeader.Code.LENGTH, file.getData().length);
        // HEADER TIME
        final ObexHeader<Date> time = new ObexHeader<Date>(ObexHeader.Code.TIME, file.getLastModified());
        
        final Map<ObexHeader.Code, ObexHeader> headers = new EnumMap<ObexHeader.Code, ObexHeader>(ObexHeader.Code.class);
        headers.put(name.getObexCode(), name);
        headers.put(length.getObexCode(), length);
        headers.put(time.getObexCode(), time);
        final Obex req = new Obex(Obex.Code.REQUEST_PUT, headers);
        
        interruptLock.lock();
        try
        {
            send(req);
            final Obex response = receive(false);
            if(response.getObexCode() != Obex.Code.RESPONSE_CONTINUE &&
                    response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
                throw new IOException("Error response received: " + response.getObexCode()); 
            }
        }
        finally
        {
            interruptLock.unlock();
        }
    }
    
    /**
     * Sends an OBEX frame with data to the module.
     * @param file The file for PUT operation
     * @param contentPartOffset The offset in the binary data of the input file
     * @param contentPartLength The length of data to send from the input file
     * @param isLastPart Marks the frame as the last frame
     * @throws IOException An error at sending OBEX frame
     * @since 1.0 
     */
    private void putFilePart(final FileContent file, final int contentPartOffset
            , final int contentPartLength, final boolean isLastPart) 
            throws IOException 
    {
//        System.out.println("CPO: " + contentPartOffset + " CPL: " + contentPartLength);
//        System.out.println("ContentPartLength: " + contentPartLength);
        byte[] contentPart = Arrays.copyOfRange(file.getData(), contentPartOffset
                , contentPartOffset + contentPartLength);
        // HEADER_BODY
        final ObexHeader<byte[]> body;
        Code bodyCode;

        if ( isLastPart )
        {
            bodyCode = ObexHeader.Code.END_OF_BODY;		
        } 
        else 
        {
            bodyCode = ObexHeader.Code.BODY;	
        }
        body = new ObexHeader<byte[]>(bodyCode, contentPart);
        
        // REQUEST_PUT
        Obex.Code reqCode;
        
        if (isLastPart) 
        {
            reqCode = Obex.Code.REQUEST_PUT_FINAL;
        } 
        else 
        {
            reqCode = Obex.Code.REQUEST_PUT;
        }
        
        final Obex req = new Obex(reqCode, body);
        
        interruptLock.lock();
        try
        {
            send(req);
            final Obex response = receive(false);
            if(response.getObexCode() != Obex.Code.RESPONSE_CONTINUE &&
                    response.getObexCode() != Obex.Code.RESPONSE_SUCCESS)
            {
                throw new IOException("Error response received: " + response.getObexCode()); 
            }
        }
        finally
        {
            interruptLock.unlock();
        }
}
    
    /**
     * Reads the response from OBEX request from stream an parse it to an {@link Obex}
     * object.
     * @param serialIn The {@link InputStream}
     * @return The parsed {@link Obex} object
     * @throws IOException An error at reading from {@link InputStream}
     * @since 1.0
     */
    private Obex receiveObex(final InputStream serialIn) throws IOException
    {
        final long t1 = System.currentTimeMillis();
        final byte[] buffer = new byte[512];
        boolean received = false;
        Obex result = null;
        
        if(responseBuffer == null)
        {
            responseBuffer = new ByteArrayOutputStream(600);
        }
        
        while(!received)
        {
            if(serialIn.available() > 0) 
            {
                final int readCount = serialIn.read(buffer);
//                System.out.println("ReadCount: " + readCount);
                responseBuffer.write(buffer, 0, readCount);
                
                final byte[] response = responseBuffer.toByteArray();
                
                if(response.length > 2)
                {
                    final int expectedLength = BinaryUtils.byteArrToShort(response, false, 1);

//                    System.out.println("Received: " + BinaryUtils.toHexString(response));
                    if(response.length == expectedLength)
                    {
                        received = true;
                        result = new Obex(response);
                    }
                }
            }
            else
            {
                final long runtime = System.currentTimeMillis() - t1;
                if(runtime > OBEX_RESPONSE_TIMEOUT)
                {
                    throw new IOException("Response timeout "+ runtime + " ms and: " 
                            + responseBuffer.toByteArray().length);
                }
                try
                {
//                    System.out.println("Sleep - " + Thread.currentThread().getId());
                    Thread.sleep(DEFAULT_SLEEP_MILLIS);
                }
                catch (final InterruptedException ex)
                {
                    LOG.error("Interrupt at receiving OBEX", ex);
                }
            }
        }
        
        responseBuffer.close();
        responseBuffer = null;
        
        return result;
    }
}
