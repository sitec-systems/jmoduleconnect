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

import de.sitec.jmoduleconnect.CommHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link At} interface. <b>IMPORTANT: </b> DONT disable the echo 
 * on the module (<code>ATE0</code>). This can disrupt the communction to the 
 * module.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class AtImpl implements At
{
    private final CommHandler commHandler;
    private final AtEventNotifier eventNotifier;
    private final Thread eventNotifierThread;
    private String atResponse;
    private final Lock lock = new ReentrantLock();
    private final Condition resonseAvailable = lock.newCondition();
    private boolean atMode;
    
    private static final Logger LOG = LoggerFactory.getLogger(AtImpl.class);
    private static final byte DEFAULT_SLEEP_MILLIS = 10;
    private static final short AT_RESPONSE_TIMEOUT = 5000;
    private static final int AT_RESPONSE_TIMEOUT_ATD = 150000;
    private static final byte WAIT_TIMEOUT = 2;
    private static final byte WAIT_TRAILS = 3;
    private static final byte WAIT_TRAILS_ATD = 90;
    private static final Charset BYTE_CHARSET = Charset.forName("ISO_8859_1");
    private static final String AT_START = "AT"; 
    private static final String AT_OK = "OK\r"; 
    private static final String AT_ERROR = "ERROR\r";
    private static final String AT_NO_CARRIER = "NO CARRIER\r";
    private static final String AT_NO_DIALTONE = "NO DIALTONE\r";
    private static final String AT_BUSY = "BUSY\r";

    private AtImpl(final CommHandler commHandler)
    {
        this.commHandler = commHandler;
        eventNotifier = new AtEventNotifier();
        eventNotifierThread = new Thread(eventNotifier);
    }
    
    /**
     * Creates an instance of this class.
     * @param commHandler The communication handler
     * @return The instance of this class
     * @throws AtCommandFailedException The response from device contains 
     *         <code>ERROR</code>
     * @throws IOException The communication to the device failed
     * @throws IllegalArgumentException If the parameter commHandler <code>null</code>
     * @since 1.0
     */
    public static final At createAt(final CommHandler commHandler) throws AtCommandFailedException, IOException
    {
        if(commHandler == null)
        {
            throw new IllegalArgumentException("The parameter commHandler cant be null");
        }
        
        final AtImpl at = new AtImpl(commHandler);
        at.init();
        try
        {
            at.send("ATE1");
            
            return at;
        }
        catch (final AtCommandFailedException ex)
        {
            at.close();
            throw ex;
        }
        catch (final IOException ex)
        {
            at.close();
            throw ex;
        }
    }
    
    private void init()
    {
        this.commHandler.addProtocolParser(this);
        atMode = true;
        eventNotifierThread.start();
    }

    /** {@inheritDoc } */
    @Override
    public void addAtListener(final AtListener atListener)
    {
        eventNotifier.addAtListener(atListener);
    }

    /** {@inheritDoc } */
    @Override
    public void close()
    {
        commHandler.removeProtocolParser(this);
        eventNotifierThread.interrupt();
    }

    /** {@inheritDoc } */
    @Override
    public void closeMode() throws AtCommandFailedException, IOException
    {
        if(atMode)
        {
            throw new IOException("Device is in AT mode - +++ not allowed in at mode");
        }
        
        try
        {
            commHandler.send("+++".getBytes(BYTE_CHARSET));
            
            String response = null;
            int trails = 0;
            
            while(atResponse == null && trails < 5)
            {
                lock.lock();
                try
                {
                    try
                    {
                        resonseAvailable.await(2, TimeUnit.SECONDS);
                        response = atResponse;
                    }
                    catch (final InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }
                }
                finally
                {
                    lock.unlock();
                }
                
                if(response == null)
                {
                    commHandler.send("+++".getBytes(BYTE_CHARSET));
                    trails++;
                }
            }
            
            lock.lock();
            try
            {
                atResponse = null;
            }
            finally
            {
                lock.unlock();
            }
            atMode = true;
        }
        catch (final IOException ex)
        {
            throw new IOException("The AT command +++ failed", ex);
        }
    }
    
    /**
     * If the first two bytes <code>\r\n</code> or <code>AT</code> then return
     * <code>true</code>.
     * @param is The {@link InputStream}
     * @return <code>true</code> if the input can interpreted by this parser
     * @throws IOException An exception at reading from {@link InputStream}
     * @since 1.0
     */
    @Override
    public boolean isProtocol(final InputStream is) throws IOException
    {
        is.mark(0);
        final byte[] buffer = new byte[2];
        is.read(buffer);
        is.reset();
        final String result = new String(buffer, BYTE_CHARSET);
        return result.contains(AT_START) || result.contains("\r\n");
    }
    
    /** {@inheritDoc } */
    @Override
    public void parse(final InputStream is) throws IOException
    {
        final String atRespTemp = receiveAtResponse(is);
        
        lock.lock();
        try
        {
            atResponse = atRespTemp;
            resonseAvailable.signalAll();
        }
        finally
        {
            lock.unlock();
        }
    }
    
    /** {@inheritDoc } */
    @Override
    public void removeAtListener(AtListener atListener)
    {
        eventNotifier.removeAtListener(atListener);
    }
    
    /** {@inheritDoc } */
    @Override
    public String send(final String atCommand) 
            throws AtCommandFailedException, IOException
    {   
        if(!atMode)
        {
            throw new IOException("Device is not in AT mode");
        }
        
        if(atCommand == null)
        {
            throw new IllegalArgumentException("Parameter atCommand cant be null");
        }
        
        final String atConv = atCommand.toUpperCase();
        final String atCommUpper = atConv.replaceAll(".JAD", ".jad");
        
        if(!atCommUpper.startsWith(AT_START))
        {
            throw new IllegalArgumentException("An AT command must start with AT. Input: " 
                    + atCommand);
        }
        
        // This check is necessary to prevent again a long timeout of ATD command
        // if the modem not available
        if(atCommUpper.contains("ATD"))
        {
            try
            {
                sendAtCommand("AT");
            }
            catch(final AtCommandFailedException ex)
            {
                throw new AtCommandFailedException("The sending of ATD has failed", ex);
            }
            catch(final IOException ex)
            {
                throw new IOException("The sending of ATD has failed", ex);
            }
        }
        
        return sendAtCommand(atCommUpper);
    }
    
    /**
     * Sends an AT command without checks and gets the response.
     * @param atCommand The AT command
     * @return The response of the AT command
     * @throws AtCommandFailedException The AT command has failed
     * @throws IOException The communication to modem has failed
     * @since 1.2
     */
    private String sendAtCommand(final String atCommand) 
            throws AtCommandFailedException, IOException
    {   
        final byte waitTrails;
        
        if(atCommand.contains("ATD"))
        {
            waitTrails = WAIT_TRAILS_ATD;
        }
        else
        {
            waitTrails = WAIT_TRAILS;
        }
        
        try
        {
            LOG.debug("Send AT command: {}", atCommand);
            final String parameter = atCommand + "\r";
            commHandler.send(parameter.getBytes(BYTE_CHARSET));
            
            String response = null;
            byte waitTrailsCount = 0;
            
            lock.lock();
            try
            {
                while(atResponse == null && waitTrailsCount < waitTrails)
                {
                    try
                    {
                        resonseAvailable.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
                    }
                    catch (final InterruptedException ex)
                    {
                        LOG.error("Interrupt at waiting for AT response", ex);
                    }
                    waitTrailsCount++;
                }
                response = atResponse;
                atResponse = null;
            }
            finally
            {
                lock.unlock();
            }
            
            if(response == null || response.contains(AT_ERROR))
            {
                throw new AtCommandFailedException("The AT command: " + atCommand
                        + " failed");
            }
            
            LOG.debug("Response of AT command: {} is: {}", atCommand, response);
            
            checkModeChange(atCommand);
            
            return removeEcho(atCommand, response);
        }
        catch (final IOException ex)
        {
            throw new IOException("Sending the AT command: " + atCommand 
                    + " failed", ex);
        }
    }
    
    /**
     * Removes the echo from a AT response.
     * @param atCommand The AT command is same like the echo
     * @param response The response of the AT command
     * @return AT response without echo part
     * @since 1.0
     */
    private static String removeEcho(final String atCommand, final String response)
    {
        return response.replace(atCommand, "");
    }
    
    /**
     * Check for AT commands they are switch the device in a non AT mode. After 
     * a switch to non AT mode at commands are not allowed. Switch to AT mode 
     * back with {@link #checkModeChange(java.lang.String) }.
     * @param atCommand The AT command to check
     * @since 1.0
     * @see #checkModeChange(java.lang.String) 
     */
    private void checkModeChange(final String atCommand)
    {
        if(atCommand.contains(AT_START + "^SQWE=3"))
        {
            atMode = false;
        }
    }
    
    private void notifyAtEvent(final String atEvent)
    {
        eventNotifier.addEvent(new AtEvent(this, atEvent));
    }
    
    /**
     * Read AT response from the input {@link InputStream} and parse it to an
     * {@link String}. Interpretation of different formats:
     * 
     * <table border="1">
     * <tr>
     * <th>Format</th>
     * <th>Interpretation</th>
     * </tr>
     * <tr>
     * <td><code>\r\n text\r\n</code></td>
     * <td>Event</td>
     * </tr>
     * <tr>
     * <td><code>\r\n OK\r\n</code></td>
     * <td>Response of <code>+++</code></td>
     * </tr>
     * <tr>
     * <td><code>AT ... OK/ERROR</code></td>
     * <td>Response of an AT command</td>
     * </tr>
     * </table>
     * @param serialIn The {@link InputStream}
     * @return The AT response as {@link String}
     * @throws IOException The communication to the device failed
     * @since 1.0
     */
    private String receiveAtResponse(final InputStream serialIn) throws IOException
    {
        final long t1 = System.currentTimeMillis();
        final byte[] buf = new byte[512];
        String result = null;
        
        try(final ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            boolean received = false;
            int atResponseTimeout = AT_RESPONSE_TIMEOUT;

            while(!received)
            {
                if(serialIn.available() > 0) 
                {
                    final int readCount = serialIn.read(buf);
                    bos.write(buf, 0, readCount);

                    final String response = new String(bos.toByteArray(), BYTE_CHARSET);

                    if(response.contains("ATD"))
                    {
                        atResponseTimeout = AT_RESPONSE_TIMEOUT_ATD;
                    }

                    if(response.length() > 3)
                    {
                        if(response.contains(AT_START))
                        {
                            if(response.contains(AT_OK) || response.contains(AT_ERROR) 
                                    || response.contains(AT_NO_CARRIER)
                                    || response.contains(AT_NO_DIALTONE)
                                    || response.contains(AT_BUSY))
                            {
                                received = true;
                                result = response;
                            }
                        }
                        else if(response.endsWith("\r\n"))
                        {
                            if(response.contains(AT_OK))
                            {
                                received = true;
                                result = response;
                            }
                            else
                            {
                                received = true;
                                notifyAtEvent(response);
                            }
                        }
                    }
                }
                else
                {
                    final long runtime = System.currentTimeMillis() - t1;
                    if(runtime > atResponseTimeout)
                    {
                        throw new IOException("Response timeout waiting for OK or ERROR after "
                                + runtime + " ms and: " + bos.toByteArray().length);
                    }
                    try
                    {
                        Thread.sleep(DEFAULT_SLEEP_MILLIS);
                    }
                    catch (final InterruptedException ex)
                    {
                        LOG.error("Interrupt at receiving AT response", ex);
                    }
                }
            }
        }
        
        return result;
    }
}
