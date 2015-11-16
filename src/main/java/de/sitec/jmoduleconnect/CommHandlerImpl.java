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
package de.sitec.jmoduleconnect;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link CommHandler} interface. This class abstracts the 
 * communication to the connected device. For receiving data its necessary to 
 * add an {@link ProtocolParser} with 
 * {@link #addProtocolParser(de.sitec.jmoduleconnect.ProtocolParser) }.
 * If data available on the {@link InputStream} it will commit to an registered 
 * {@link ProtocolParser}. The {@link ProtocolParser} can check for valid protocol 
 * with {@link ProtocolParser#isProtocol(java.io.InputStream) }. If the response 
 * <code>true</code> then will commit the {@link InputStream} to 
 * {@link ProtocolParser#parse(java.io.InputStream) } and the protocol can be
 * processed. After the processing the iteration over the registered 
 * {@link ProtocolParser} will stop and wait for the notification about new
 * available data.
 * @author sitec systems GmbH
 * @since 1.0
 */
public class CommHandlerImpl implements CommHandler
{
    private final List<ProtocolParser> protocolParserList;
    private SerialPort serialPort;
    private BufferedInputStream serialIn;
    private OutputStream serialOut;
    
    private static final Logger LOG = LoggerFactory.getLogger(CommHandlerImpl.class);
    private static final String APP_PORT_NAME = "jModuleConnect";
    private static final short SERIAL_PORT_TIMEOUT = 2000;

    private CommHandlerImpl()
    {
        protocolParserList = new ArrayList<ProtocolParser>();
    }
    
    /**
     * Creates an instance of this class.
     * @param commPortIdentifier Must point to an serial port
     * @param baudrate The baudrate of the communication. The baudrate must be 
     *        setted with <code>AT+IPR=*baudrate*</code>. The default baudrate
     *        of an device is <code>115200</code>
     * @return An instance of <code>CommHandlerImpl</code>
     * @throws PortInUseException The selected port is used by another application
     * @throws IOException The communication to the device failed
     * @throws IllegalArgumentException If parameter commPortIdentifier is 
     *         <code>null</code> or the result of {@link CommPortIdentifier#open(java.lang.String, int) } 
     *         is not an instance of {@link SerialPort}.
     * @since 1.0
     */
    public static final CommHandler createCommHandler(final CommPortIdentifier commPortIdentifier
            , final int baudrate) 
            throws PortInUseException, IOException
    {
        final CommHandlerImpl commHandler = new CommHandlerImpl();
            
        try
        {
            commHandler.init(commPortIdentifier, baudrate);
            return commHandler;
        }
        catch (final PortInUseException ex)
        {
            commHandler.close();
            throw ex;
        }
        catch (final IOException ex)
        {
            commHandler.close();
            throw ex;
        }
    }
    
    private void init(final CommPortIdentifier commPortIdentifier, final int baudrate) 
            throws IOException, PortInUseException
    {
        if(commPortIdentifier == null)
        {
            throw new IllegalArgumentException("The parameter commPortIdentifier cant be null");
        }
        final CommPort commPort = commPortIdentifier.open(APP_PORT_NAME, SERIAL_PORT_TIMEOUT);
        
        if(!(commPort instanceof SerialPort))
        {
            throw new IllegalArgumentException("The choosed CommPort is not from type SerialPort");
        }
        
        serialPort = (SerialPort)commPort;
        try
        {
            serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8
                    , SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.enableReceiveTimeout(2000);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT 
                    | SerialPort.FLOWCONTROL_RTSCTS_IN);
        }
        catch (final UnsupportedCommOperationException ex)
        {
            throw new IOException("System not supported", ex);
        }
        
        serialIn = new BufferedInputStream(serialPort.getInputStream());
        serialOut = serialPort.getOutputStream();
        try
        {
            this.serialPort.addEventListener(new Listener(this.serialIn));
        }
        catch (final TooManyListenersException ex)
        {
            LOG.error("Error at initialising of SerialPort", ex);
        }
        this.serialPort.notifyOnDataAvailable(true);
    }

    /** {@inheritDoc } */
    @Override
    public void addProtocolParser(final ProtocolParser protocolParser)
    {
        if(protocolParser == null)
        {
            throw new IllegalArgumentException("The parameter protcolParser cant be null");
        }
        protocolParserList.add(protocolParser);
    }

    /** {@inheritDoc } */
    @Override
    public void close() throws IOException
    {
        if(serialIn != null)
        {
            serialIn.close();
            serialIn = null;
        }
        
        if(serialOut != null)
        {
            serialOut.close();
            serialOut = null;
        }
        
        if(serialPort != null)
        {
            serialPort.close();
            serialPort = null;
        }
        
        protocolParserList.clear();
    }

    /** {@inheritDoc } */
    @Override
    public void removeProtocolParser(final ProtocolParser protocolParser)
    {
        if(protocolParser == null)
        {
            throw new IllegalArgumentException("The parameter protcolParser cant be null");
        }
        protocolParserList.remove(protocolParser);
    }
    
    /** {@inheritDoc } */
    @Override
    public void send(final byte[] data) throws IOException
    {
        serialOut.write(data);
        serialOut.flush();
    }
    
    /**
     * Implements the {@link SerialPortEventListener} interface for receiving an
     * notificaten if data available on the {@link InputStream}. If data available
     * the {@link InputStream} will commit to an registered {@link ProtocolParser}.
     * The {@link ProtocolParser} can check for valid protocol with 
     * {@link ProtocolParser#isProtocol(java.io.InputStream) }. If the response 
     * <code>true</code> then will commit the {@link InputStream} to 
     * {@link ProtocolParser#parse(java.io.InputStream) } an the protocol can be
     * processed. After the processing the iteration over the registered 
     * {@link ProtocolParser} will stop and wait for the notification about new
     * available data.
     * @since 1.0
     */
    private final class Listener implements SerialPortEventListener
    {
        private final BufferedInputStream serialIn;

        /**
         * Constructor of this class.
         * @param serialIn 
         * @throws IllegalArgumentException If the parameter serialIn <code>
         *         null</code>
         * @since 1.0
         */
        public Listener(final BufferedInputStream serialIn)
        {
            if(serialIn == null)
            {
                throw new IllegalArgumentException("Parameter serialIn cant be null");
            }
            this.serialIn = serialIn;
        }
        
        /** {@inheritDoc } */
        @Override
        public void serialEvent(final SerialPortEvent spe)
        {
            if(spe.getEventType() == SerialPortEvent.DATA_AVAILABLE)
            {
//                System.out.println("SerialPortEvent - " + Thread.currentThread().getId());
                try
                {
//                    final byte firstByte = (byte)serialIn.read();
//                    System.out.println("FirstByte (FAC) " + Thread.currentThread().getName() + " : " + Integer.toHexString(firstByte));
                    for(final ProtocolParser protocolParser: protocolParserList)
                    {
                        if(protocolParser.isProtocol(serialIn))
                        {
//                            System.out.println("Protocol");
                            protocolParser.parse(serialIn);
                            break;
                        }
                    }
                }
                catch (final IOException ex)
                {
                    LOG.error("Error at receiving data", ex);
                }
            }
        }
    }
}
