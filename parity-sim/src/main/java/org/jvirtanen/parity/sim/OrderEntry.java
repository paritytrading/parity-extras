package org.jvirtanen.parity.sim;

import static org.jvirtanen.parity.util.Applications.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.jvirtanen.nassau.MessageListener;
import org.jvirtanen.nassau.soupbintcp.SoupBinTCP;
import org.jvirtanen.nassau.soupbintcp.SoupBinTCPClient;
import org.jvirtanen.nassau.soupbintcp.SoupBinTCPClientStatusListener;
import org.jvirtanen.parity.net.poe.POE;
import org.jvirtanen.parity.net.poe.POEClientListener;
import org.jvirtanen.parity.net.poe.POEClientParser;

class OrderEntry {

    private SoupBinTCPClient transport;

    private ByteBuffer txBuffer;

    private OrderEntry(SoupBinTCPClient transport) {
        this.transport = transport;

        this.txBuffer = ByteBuffer.allocate(POE.MAX_INBOUND_MESSAGE_LENGTH);
    }

    public static OrderEntry open(InetSocketAddress address) throws IOException {
        SocketChannel channel = SocketChannel.open();

        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        channel.connect(address);
        channel.configureBlocking(false);

        MessageListener listener = new POEClientParser(new Listener());

        SoupBinTCPClient transport = new SoupBinTCPClient(channel, POE.MAX_OUTBOUND_MESSAGE_LENGTH,
                listener, new StatusListener());

        return new OrderEntry(transport);
    }

    public void login(String username, String password) throws IOException {
        SoupBinTCP.LoginRequest request = new SoupBinTCP.LoginRequest(username, password, "", 0);

        transport.login(request);
    }

    public void send(POE.InboundMessage message) throws IOException {
        txBuffer.clear();
        message.put(txBuffer);
        txBuffer.flip();

        transport.send(txBuffer);
    }

    public SoupBinTCPClient getTransport() {
        return transport;
    }

    private static class Listener implements POEClientListener {

        @Override
        public void orderAccepted(POE.OrderAccepted message) {
        }

        @Override
        public void orderRejected(POE.OrderRejected message) {
            error("Order rejected");
        }

        @Override
        public void orderExecuted(POE.OrderExecuted message) {
        }

        @Override
        public void orderCanceled(POE.OrderCanceled message) {
        }

        @Override
        public void brokenTrade(POE.BrokenTrade message) {
        }

    }

    private static class StatusListener implements SoupBinTCPClientStatusListener {

        @Override
        public void heartbeatTimeout() {
            error("Heartbeat timeout");
        }

        @Override
        public void endOfSession() {
        }

        @Override
        public void loginAccepted(SoupBinTCP.LoginAccepted payload) {
        }

        @Override
        public void loginRejected(SoupBinTCP.LoginRejected payload) {
            error("Login rejected");
        }

    }

}
