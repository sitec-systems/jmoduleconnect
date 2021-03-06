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
package de.sitec_systems.jmoduleconnect;

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
import java.util.EnumSet;
import java.util.List;
import java.util.TooManyListenersException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link CommHandler} interface. This class abstracts the 
 * communication to the connected device. For receiving data its necessary to 
 * add an {@link ProtocolParser} with 
 * {@link #addProtocolParser(de.sitec_systems.jmoduleconnect.ProtocolParser) }.
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
    private OutputStream serialOut;
    
    private static final Logger LOG = LoggerFactory.getLogger(CommHandlerImpl.class);
    private static final String APP_PORT_NAME = "jModuleConnect";
    private static final short SERIAL_PORT_TIMEOUT = 2000;
    private static final int STREAM_BUFFER_SIZE = 65536;

    private CommHandlerImpl()
    {
        protocolParserList = new ArrayList<>();
    }
    
    /**
     * Creates an instance of this class. Enables flow control mode RTC and CTS.
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
        return createCommHandler(commPortIdentifier, baudrate
                , EnumSet.of(FlowControlMode.RTSCTS_IN, FlowControlMode.RTSCTS_OUT));
    }
    
    /**
     * Creates an instance of this class.
     * @param commPortIdentifier Must point to an serial port
     * @param baudrate The baudrate of the communication. The baudrate must be 
     *        setted with <code>AT+IPR=*baudrate*</code>. The default baudrate
     *        of an device is <code>115200</code>
     * @param flowControlMode The flow control mode of the serial port
     * @return An instance of <code>CommHandlerImpl</code>
     * @throws PortInUseException The selected port is used by another application
     * @throws IOException The communication to the device failed
     * @throws IllegalArgumentException If parameter commPortIdentifier is 
     *         <code>null</code> or the result of {@link CommPortIdentifier#open(java.lang.String, int) } 
     *         is not an instance of {@link SerialPort}.
     * @since 1.5
     */
    public static final CommHandler createCommHandler(final CommPortIdentifier commPortIdentifier
            , final int baudrate, final EnumSet<FlowControlMode> flowControlMode) 
            throws PortInUseException, IOException
    {
        final CommHandlerImpl commHandler = new CommHandlerImpl();
            
        try
        {
            commHandler.init(commPortIdentifier, baudrate, flowControlMode);
            return commHandler;
        }
        catch (final PortInUseException | IOException ex)
        {
            commHandler.close();
            throw ex;
        }
    }
    
    private void init(final CommPortIdentifier commPortIdentifier, final int baudrate
            , final EnumSet<FlowControlMode> flowControlMode) 
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
            serialPort.setFlowControlMode(FlowControlMode.getValue(flowControlMode));
        }
        catch (final UnsupportedCommOperationException ex)
        {
            throw new IOException("System not supported", ex);
        }
                
        serialOut = serialPort.getOutputStream();
        try
        {
            this.serialPort.addEventListener(new Listener());
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
        protocolParserList.clear();
        
        if(serialPort != null)
        {
            serialPort.removeEventListener();
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
        /** {@inheritDoc } */
        @Override
        public void serialEvent(final SerialPortEvent spe)
        {
            try(final BufferedInputStream serialIn = new BufferedInputStream(serialPort.getInputStream()
                    , STREAM_BUFFER_SIZE);)
            {
                if(spe.getEventType() == SerialPortEvent.DATA_AVAILABLE)
                {
    //                System.out.println("SerialPortEvent - " + Thread.currentThread().getId());
                    try
                    {
                        while(serialIn.available() > 0)
                        {
                            boolean parsed = false;
                            for(final ProtocolParser protocolParser: protocolParserList)
                            {
                                if(protocolParser.isProtocol(serialIn))
                                {
                                    protocolParser.parse(serialIn);
                                    parsed = true;
                                    break;
                                }
                            }

                            if(!parsed)
                            {
                               serialIn.skip(1);
                               serialIn.mark(0);
                            }
                        }
                    }
                    catch (final IOException ex)
                    {
                        LOG.error("Error at receiving data", ex);
                    }
                }
            }
            catch(final IOException ex)
            {
                LOG.error("Processing serial event has failed", ex);
            }
        }
    }
    
    /**
     * An enumeration for flow control mode.
     * @since 1.5
     */
    public static enum FlowControlMode
    {
        NONE(SerialPort.FLOWCONTROL_NONE)
        , RTSCTS_IN(SerialPort.FLOWCONTROL_RTSCTS_IN)
        , RTSCTS_OUT(SerialPort.FLOWCONTROL_RTSCTS_OUT)
        , XONXOFF_IN(SerialPort.FLOWCONTROL_XONXOFF_IN)
        , XONXOFF_OUT(SerialPort.FLOWCONTROL_XONXOFF_OUT);
        
        private final int value;

        private FlowControlMode(final int value)
        {
            this.value = value;
        }
        
        private static int getValue(final EnumSet<FlowControlMode> flowContolMode)
        {
            int result = 0;
            
            if(flowContolMode.contains(NONE) && flowContolMode.size() > 1)
            {
                throw new IllegalArgumentException("Flow control mode 'none' can't combine with other modes");
            }
            
            for(final FlowControlMode fcm: values())
            {
                if(flowContolMode.contains(fcm))
                {
                    result |= fcm.value;
                }
            }
            
            return result;
        }
    }
}
