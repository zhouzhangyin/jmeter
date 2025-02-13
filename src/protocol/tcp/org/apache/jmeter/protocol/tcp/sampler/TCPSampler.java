/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.protocol.tcp.sampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

/**
 * A sampler which understands Tcp requests.
 *
 */
public class TCPSampler extends AbstractSampler implements ThreadListener, Interruptible {
    private static final long serialVersionUID = 280L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final Set<String> APPLIABLE_CONFIG_CLASSES = new HashSet<>(
            Arrays.asList(
                    "org.apache.jmeter.config.gui.LoginConfigGui",
                    "org.apache.jmeter.protocol.tcp.config.gui.TCPConfigGui",
                    "org.apache.jmeter.config.gui.SimpleConfigGui"
            ));

    //++ JMX file constants - do not change
    public static final String SERVER = "TCPSampler.server"; //$NON-NLS-1$

    public static final String PORT = "TCPSampler.port"; //$NON-NLS-1$

    public static final String FILENAME = "TCPSampler.filename"; //$NON-NLS-1$

    public static final String CLASSNAME = "TCPSampler.classname";//$NON-NLS-1$

    public static final String NODELAY = "TCPSampler.nodelay"; //$NON-NLS-1$

    public static final String TIMEOUT = "TCPSampler.timeout"; //$NON-NLS-1$

    public static final String TIMEOUT_CONNECT = "TCPSampler.ctimeout"; //$NON-NLS-1$

    public static final String REQUEST = "TCPSampler.request"; //$NON-NLS-1$

    public static final String RE_USE_CONNECTION = "TCPSampler.reUseConnection"; //$NON-NLS-1$
    public static final boolean RE_USE_CONNECTION_DEFAULT = true;

    public static final String CLOSE_CONNECTION = "TCPSampler.closeConnection"; //$NON-NLS-1$
    public static final boolean CLOSE_CONNECTION_DEFAULT = false;

    public static final String SO_LINGER = "TCPSampler.soLinger"; //$NON-NLS-1$

    public static final String EOL_BYTE = "TCPSampler.EolByte"; //$NON-NLS-1$

    //-- JMX file constants - do not change

    private static final String TCPKEY = "TCP"; //$NON-NLS-1$ key for HashMap

    private static final String ERRKEY = "ERR"; //$NON-NLS-1$ key for HashMap

    // If set, this is the regex that is used to extract the status from the
    // response
    // NOT implemented yet private static final String STATUS_REGEX =
    // JMeterUtils.getPropDefault("tcp.status.regex","");

    // Otherwise, the response is scanned for these strings
    private static final String STATUS_PREFIX = JMeterUtils.getPropDefault("tcp.status.prefix", ""); //$NON-NLS-1$

    private static final String STATUS_SUFFIX = JMeterUtils.getPropDefault("tcp.status.suffix", ""); //$NON-NLS-1$

    private static final String STATUS_PROPERTIES = JMeterUtils.getPropDefault("tcp.status.properties", ""); //$NON-NLS-1$

    private static final Properties statusProps = new Properties();

    private static final boolean haveStatusProps;

    static {
        boolean hsp = false;
        log.debug("Status prefix=" + STATUS_PREFIX); //$NON-NLS-1$
        log.debug("Status suffix=" + STATUS_SUFFIX); //$NON-NLS-1$
        log.debug("Status properties=" + STATUS_PROPERTIES); //$NON-NLS-1$
        if (STATUS_PROPERTIES.length() > 0) {
            File f = new File(STATUS_PROPERTIES);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                statusProps.load(fis);
                log.debug("Successfully loaded properties"); //$NON-NLS-1$
                hsp = true;
            } catch (FileNotFoundException e) {
                log.debug("Property file not found"); //$NON-NLS-1$
            } catch (IOException e) {
                log.debug("Property file error " + e.toString()); //$NON-NLS-1$
            } finally {
                JOrphanUtils.closeQuietly(fis);
            }
        }
        haveStatusProps = hsp;
    }

    /** the cache of TCP Connections */
    // KEY = TCPKEY or ERRKEY, Entry= Socket or String
    private static final ThreadLocal<Map<String, Object>> tp =
        new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    private transient TCPClient protocolHandler;
    
    private transient boolean firstSample; // Are we processing the first sample?

    private transient volatile Socket currentSocket; // used for handling interrupt

    public TCPSampler() {
        log.debug("Created " + this); //$NON-NLS-1$
    }

    private String getError() {
        Map<String, Object> cp = tp.get();
        return (String) cp.get(ERRKEY);
    }

    private Socket getSocket(String socketKey) {
        Map<String, Object> cp = tp.get();
        Socket con = null;
        if (isReUseConnection()) {
            con = (Socket) cp.get(socketKey);
            if (con != null) {
                log.debug(this + " Reusing connection " + con); //$NON-NLS-1$
            }
        }
        if (con == null) {
            // Not in cache, so create new one and cache it
            try {
                closeSocket(socketKey); // Bug 44910 - close previous socket (if any)
                SocketAddress sockaddr = new InetSocketAddress(getServer(), getPort());
                con = new Socket();
                if (getPropertyAsString(SO_LINGER,"").length() > 0){
                    con.setSoLinger(true, getSoLinger());
                }
                con.connect(sockaddr, getConnectTimeout());
                if(log.isDebugEnabled()) {
                    log.debug("Created new connection " + con); //$NON-NLS-1$
                }
                cp.put(socketKey, con);
            } catch (UnknownHostException e) {
                log.warn("Unknown host for " + getLabel(), e);//$NON-NLS-1$
                cp.put(ERRKEY, e.toString());
                return null;
            } catch (IOException e) {
                log.warn("Could not create socket for " + getLabel(), e); //$NON-NLS-1$
                cp.put(ERRKEY, e.toString());
                return null;
            }     
        }
        // (re-)Define connection params - Bug 50977 
        try {
            con.setSoTimeout(getTimeout());
            con.setTcpNoDelay(getNoDelay());
            if(log.isDebugEnabled()) {
                log.debug(this + "  Timeout " + getTimeout() + " NoDelay " + getNoDelay()); //$NON-NLS-1$
            }
        } catch (SocketException se) {
            log.warn("Could not set timeout or nodelay for " + getLabel(), se); //$NON-NLS-1$
            cp.put(ERRKEY, se.toString());
        }
        return con;
    }

    /**
     * @return String socket key in cache Map
     */
    private String getSocketKey() {
        return TCPKEY+"#"+getServer()+"#"+getPort()+"#"+getUsername()+"#"+getPassword();
    }

    public String getUsername() {
        return getPropertyAsString(ConfigTestElement.USERNAME);
    }

    public String getPassword() {
        return getPropertyAsString(ConfigTestElement.PASSWORD);
    }

    public void setServer(String newServer) {
        this.setProperty(SERVER, newServer);
    }

    public String getServer() {
        return getPropertyAsString(SERVER);
    }

    public boolean isReUseConnection() {
        return getPropertyAsBoolean(RE_USE_CONNECTION, RE_USE_CONNECTION_DEFAULT);
    }

    public void setCloseConnection(String close) {
        this.setProperty(CLOSE_CONNECTION, close, "");
    }

    public boolean isCloseConnection() {
        return getPropertyAsBoolean(CLOSE_CONNECTION, CLOSE_CONNECTION_DEFAULT);
    }

    public void setSoLinger(String soLinger) {
        this.setProperty(SO_LINGER, soLinger, "");
    }

    public int getSoLinger() {
        return getPropertyAsInt(SO_LINGER);
    }
    
    public void setEolByte(String eol) {
        this.setProperty(EOL_BYTE, eol, "");
    }
    
    public int getEolByte() {
        return getPropertyAsInt(EOL_BYTE);
    }
    

    public void setPort(String newFilename) {
        this.setProperty(PORT, newFilename);
    }

    public int getPort() {
        return getPropertyAsInt(PORT);
    }

    public void setFilename(String newFilename) {
        this.setProperty(FILENAME, newFilename);
    }

    public String getFilename() {
        return getPropertyAsString(FILENAME);
    }

    public void setRequestData(String newRequestData) {
        this.setProperty(REQUEST, newRequestData);
    }

    public String getRequestData() {
        return getPropertyAsString(REQUEST);
    }

    public void setTimeout(String newTimeout) {
        this.setProperty(TIMEOUT, newTimeout);
    }

    public int getTimeout() {
        return getPropertyAsInt(TIMEOUT);
    }

    public void setConnectTimeout(String newTimeout) {
        this.setProperty(TIMEOUT_CONNECT, newTimeout, "");
    }

    public int getConnectTimeout() {
        return getPropertyAsInt(TIMEOUT_CONNECT, 0);
    }

    public boolean getNoDelay() {
        return getPropertyAsBoolean(NODELAY);
    }

    public void setClassname(String classname) {
        this.setProperty(CLASSNAME, classname, ""); //$NON-NLS-1$
    }

    public String getClassname() {
        String clazz = getPropertyAsString(CLASSNAME,"");
        if (clazz==null || clazz.length()==0){
            clazz = JMeterUtils.getPropDefault("tcp.handler", "TCPClientImpl"); //$NON-NLS-1$ $NON-NLS-2$
        }
        return clazz;
    }

    /**
     * Returns a formatted string label describing this sampler Example output:
     * Tcp://Tcp.nowhere.com/pub/README.txt
     *
     * @return a formatted string label describing this sampler
     */
    public String getLabel() {
        return ("tcp://" + this.getServer() + ":" + this.getPort());//$NON-NLS-1$ $NON-NLS-2$
    }

    private static final String protoPrefix = "org.apache.jmeter.protocol.tcp.sampler."; //$NON-NLS-1$

    private Class<?> getClass(String className) {
        Class<?> c = null;
        try {
            c = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                c = Class.forName(protoPrefix + className, false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e1) {
                log.error("Could not find protocol class '" + className+"'"); //$NON-NLS-1$
            }
        }
        return c;

    }

    private TCPClient getProtocol() {
        TCPClient TCPClient = null;
        Class<?> javaClass = getClass(getClassname());
        if (javaClass == null){
            return null;
        }
        try {
            TCPClient = (TCPClient) javaClass.newInstance();
            if (getPropertyAsString(EOL_BYTE, "").length()>0){
                TCPClient.setEolByte(getEolByte());
                log.info("Using eolByte=" + getEolByte());
            }

            if (log.isDebugEnabled()) {
                log.debug(this + "Created: " + getClassname() + "@" + Integer.toHexString(TCPClient.hashCode())); //$NON-NLS-1$
            }
        } catch (Exception e) {
            log.error(this + " Exception creating: " + getClassname(), e); //$NON-NLS-1$
        }
        return TCPClient;
    }

    @Override
    public SampleResult sample(Entry e)// Entry tends to be ignored ...
    {
        if (firstSample) { // Do stuff we cannot do as part of threadStarted()
            initSampling();
            firstSample=false;
        }
        final boolean reUseConnection = isReUseConnection();
        final boolean closeConnection = isCloseConnection();
        String socketKey = getSocketKey();
        if (log.isDebugEnabled()){
            log.debug(getLabel() + " " + getFilename() + " " + getUsername() + " " + getPassword());
        }
        SampleResult res = new SampleResult();
        boolean isSuccessful = false;
        res.setSampleLabel(getName());// Use the test element name for the label
        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(getServer()); // $NON-NLS-1$
        sb.append(" Port: ").append(getPort()); // $NON-NLS-1$
        sb.append("\n"); // $NON-NLS-1$
        sb.append("Reuse: ").append(reUseConnection); // $NON-NLS-1$
        sb.append(" Close: ").append(closeConnection); // $NON-NLS-1$
        sb.append("\n["); // $NON-NLS-1$
        sb.append("SOLINGER: ").append(getSoLinger()); // $NON-NLS-1$
        sb.append(" EOL: ").append(getEolByte()); // $NON-NLS-1$
        sb.append(" noDelay: ").append(getNoDelay()); // $NON-NLS-1$
        sb.append("]"); // $NON-NLS-1$
        res.setSamplerData(sb.toString()); 
        res.sampleStart();
        try {
            Socket sock = getSocket(socketKey);
            if (sock == null) {
                res.setResponseCode("500"); //$NON-NLS-1$
                res.setResponseMessage(getError());
            } else if (protocolHandler == null){
                res.setResponseCode("500"); //$NON-NLS-1$
                res.setResponseMessage("Protocol handler not found");
            } else {
                currentSocket = sock;
                InputStream is = sock.getInputStream();
                OutputStream os = sock.getOutputStream();
                String req = getRequestData();
                // TODO handle filenames
                res.setSamplerData(req);
                protocolHandler.write(os, req);
                String in = protocolHandler.read(is);
                isSuccessful = setupSampleResult(res, in, null, protocolHandler.getCharset());
            }
        } catch (ReadException ex) {
            log.error("", ex);
            isSuccessful=setupSampleResult(res, ex.getPartialResponse(), ex,protocolHandler.getCharset());
            closeSocket(socketKey);
        } catch (Exception ex) {
            log.error("", ex);
            isSuccessful=setupSampleResult(res, "", ex, protocolHandler.getCharset());
            closeSocket(socketKey);
        } finally {
            currentSocket = null;
            // Calculate response time
            res.sampleEnd();

            // Set if we were successful or not
            res.setSuccessful(isSuccessful);

            if (!reUseConnection || closeConnection) {
                closeSocket(socketKey);
            }
        }
        return res;
    }

    /**
     * Fills SampleResult object
     * @param sampleResult {@link SampleResult}
     * @param readResponse Response read until error occured
     * @param exception Source exception
     * @param encoding sample encoding
     * @return boolean if sample is considered as successful
     */
    private boolean setupSampleResult(SampleResult sampleResult,
            String readResponse, 
            Exception exception,
            String encoding) {
        sampleResult.setResponseData(readResponse, encoding);
        sampleResult.setDataType(SampleResult.TEXT);
        if(exception==null) {
            sampleResult.setResponseCodeOK();
            sampleResult.setResponseMessage("OK"); //$NON-NLS-1$
        } else {
            sampleResult.setResponseCode("500"); //$NON-NLS-1$
            sampleResult.setResponseMessage(exception.toString()); //$NON-NLS-1$
        }
        boolean isSuccessful = exception == null;
        // Reset the status code if the message contains one
        if (!StringUtils.isEmpty(readResponse) && STATUS_PREFIX.length() > 0) {
            int i = readResponse.indexOf(STATUS_PREFIX);
            int j = readResponse.indexOf(STATUS_SUFFIX, i + STATUS_PREFIX.length());
            if (i != -1 && j > i) {
                String rc = readResponse.substring(i + STATUS_PREFIX.length(), j);
                sampleResult.setResponseCode(rc);
                isSuccessful = isSuccessful && checkResponseCode(rc);
                if (haveStatusProps) {
                    sampleResult.setResponseMessage(statusProps.getProperty(rc, "Status code not found in properties")); //$NON-NLS-1$
                } else {
                    sampleResult.setResponseMessage("No status property file");
                }
            } else {
                sampleResult.setResponseCode("999"); //$NON-NLS-1$
                sampleResult.setResponseMessage("Status value not found");
                isSuccessful = false;
            }
        }
        return isSuccessful;
    }

    /**
     * @param rc response code
     * @return whether this represents success or not
     */
    private boolean checkResponseCode(String rc) {
        if (rc.compareTo("400") >= 0 && rc.compareTo("499") <= 0) { //$NON-NLS-1$ $NON-NLS-2$
            return false;
        }
        if (rc.compareTo("500") >= 0 && rc.compareTo("599") <= 0) { //$NON-NLS-1$ $NON-NLS-2$
            return false;
        }
        return true;
    }

    @Override
    public void threadStarted() {
        log.debug("Thread Started"); //$NON-NLS-1$
        firstSample = true;
    }

    // Cannot do this as part of threadStarted() because the Config elements have not been processed.
    private void initSampling(){
        protocolHandler = getProtocol();
        log.debug("Using Protocol Handler: " +  //$NON-NLS-1$
                (protocolHandler == null ? "NONE" : protocolHandler.getClass().getName())); //$NON-NLS-1$
        if (protocolHandler != null){
            protocolHandler.setupTest();
        }
    }

    /**
     * Close socket of current sampler
     */
    private void closeSocket(String socketKey) {
        Map<String, Object> cp = tp.get();
        Socket con = (Socket) cp.remove(socketKey);
        if (con != null) {
            log.debug(this + " Closing connection " + con); //$NON-NLS-1$
            try {
                con.close();
            } catch (IOException e) {
                log.warn("Error closing socket "+e); //$NON-NLS-1$
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void threadFinished() {
        log.debug("Thread Finished"); //$NON-NLS-1$
        tearDown();
        if (protocolHandler != null){
            protocolHandler.teardownTest();
        }
    }

    /**
     * Closes all connections, clears Map and remove thread local Map
     */
    private void tearDown() {
        Map<String, Object> cp = tp.get();
        for (Map.Entry<String, Object> element : cp.entrySet()) {
            if(element.getKey().startsWith(TCPKEY)) {
                try {
                    ((Socket)element.getValue()).close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
        cp.clear();
        tp.remove();
    }
    
    /**
     * @see org.apache.jmeter.samplers.AbstractSampler#applies(org.apache.jmeter.config.ConfigTestElement)
     */
    @Override
    public boolean applies(ConfigTestElement configElement) {
        String guiClass = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();
        return APPLIABLE_CONFIG_CLASSES.contains(guiClass);
    }

    @Override
    public boolean interrupt() {
        Socket sock = currentSocket; // fetch in case gets nulled later
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException e) {
                // ignored
            }
            return true;
        }
        return false;
    }
}
