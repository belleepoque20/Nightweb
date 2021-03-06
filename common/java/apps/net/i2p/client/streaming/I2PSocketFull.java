package net.i2p.client.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import net.i2p.I2PAppContext;
import net.i2p.client.I2PSession;
import net.i2p.data.Destination;
import net.i2p.util.Log;

/**
 * Bridge between the full streaming lib and the I2PSocket API
 *
 */
class I2PSocketFull implements I2PSocket {
    private final Log log;
    private volatile Connection _connection;
    private final Destination _remotePeer;
    private final Destination _localPeer;
    private volatile MessageChannel _channel;
    private final AtomicBoolean _closed = new AtomicBoolean(false);
    
    public I2PSocketFull(Connection con, I2PAppContext context) {
        log = context.logManager().getLog(I2PSocketFull.class);
        _connection = con;
        if (con != null) {
            _remotePeer = con.getRemotePeer();
            _localPeer = con.getSession().getMyDestination();
        } else
            _remotePeer = _localPeer = null;
    }
    
    public void close() throws IOException {
        if (!_closed.compareAndSet(false,true)) {
            // log a trace to find out why
            LogUtil.logCloseLoop(log, "I2PSocket",_localPeer,"-->",_remotePeer,_connection);
            return;
        }
        Connection c = _connection;
        if (c == null) return;
        if (c.getIsConnected()) {
            OutputStream out = c.getOutputStream();
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    // ignore any write error, as we want to keep on and kill the
                    // con (thanks Complication!)
                }
            }
	    c.disconnect(true);
        } else {
            //throw new IOException("Not connected");
        }
        destroy();
    }
    
    Connection getConnection() { return _connection; }
    
    /**
     *  Warning, may return null instead of throwing IOE,
     *  which is not what the interface says.
     */
    public InputStream getInputStream() {
        Connection c = _connection;
        if (c != null)
            return c.getInputStream();
        else
            return null;
    }
    
    public I2PSocketOptions getOptions() {
        Connection c = _connection;
        if (c != null)
            return c.getOptions();
        else
            return null;
    }

    /**
     *  @since 0.8.9
     */
    public synchronized SelectableChannel getChannel() {
        if (_channel == null)
            _channel = new MessageChannel(this);
        return _channel;
    }
    
    /**
     *  Warning, may return null instead of throwing IOE,
     *  which is not what the interface says.
     */
    public OutputStream getOutputStream() throws IOException {
        Connection c = _connection;
        if (c != null)
            return c.getOutputStream();
        else
            return null;
    }
    
    public Destination getPeerDestination() { return _remotePeer; }
    
    public long getReadTimeout() {
        I2PSocketOptions opts = getOptions();
        if (opts != null) 
            return opts.getReadTimeout();
        else 
            return -1;
    }
    
    public Destination getThisDestination() { return _localPeer; }
    
    public void setOptions(I2PSocketOptions options) {
        Connection c = _connection;
        if (c == null) return;
        
        if (options instanceof ConnectionOptions)
            c.setOptions((ConnectionOptions)options);
        else
            c.setOptions(new ConnectionOptions(options));
    }
    
    public void setReadTimeout(long ms) {
        Connection c = _connection;
        if (c == null) return;
        
        c.getInputStream().setReadTimeout((int)ms);
        c.getOptions().setReadTimeout(ms);
    }
    
    public void setSocketErrorListener(I2PSocket.SocketErrorListener lsnr) {
    }
    
    public boolean isClosed() { 
        Connection c = _connection;
        return ((c == null) ||
                (!c.getIsConnected()) || 
                (c.getResetReceived()) ||
                (c.getResetSent()));
    }
    
    void destroy() { 
        Connection c = _connection;
        destroy2();
        if (c != null)
            c.disconnectComplete();
    }
    
    /**
     *  Call from Connection.disconnectComplete()
     *  instead of destroy() so we don't loop
     *  @since 0.8.13
     */
    void destroy2() { 
        _connection = null;
    }
    
    /**
     * The remote port.
     * @return the port or 0 if unknown
     * @since 0.8.9
     */
    public int getPort() {
        Connection c = _connection;
        return c == null ? I2PSession.PORT_UNSPECIFIED : c.getPort();
    }

    /**
     * The local port.
     * @return the port or 0 if unknown
     * @since 0.8.9
     */
    public int getLocalPort() {
        Connection c = _connection;
        return c == null ? I2PSession.PORT_UNSPECIFIED : c.getLocalPort();
    }

    @Override
    public String toString() {
        Connection c = _connection;
        if (c == null)
            return super.toString();
        else
            return c.toString();
    }
}
