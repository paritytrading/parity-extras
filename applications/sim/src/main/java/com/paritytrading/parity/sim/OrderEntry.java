package com.paritytrading.parity.sim;

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.nassau.MessageListener;
import com.paritytrading.nassau.soupbintcp.SoupBinTCP;
import com.paritytrading.nassau.soupbintcp.SoupBinTCPClient;
import com.paritytrading.nassau.soupbintcp.SoupBinTCPClientStatusListener;
import com.paritytrading.parity.net.poe.POE;
import com.paritytrading.parity.net.poe.POEClientListener;
import com.paritytrading.parity.net.poe.POEClientParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class OrderEntry {

    private SoupBinTCPClient transport;

    private ByteBuffer txBuffer;

    private OrderEntry(SoupBinTCPClient transport) {
        this.transport = transport;

        this.txBuffer = ByteBuffer.allocateDirect(POE.MAX_INBOUND_MESSAGE_LENGTH);
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
        SoupBinTCP.LoginRequest request = new SoupBinTCP.LoginRequest();

        ASCII.putLeft(request.username, username);
        ASCII.putLeft(request.password, password);
        ASCII.putRight(request.requestedSession, "");
        ASCII.putLongRight(request.requestedSequenceNumber, 0);

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

    }

    private static class StatusListener implements SoupBinTCPClientStatusListener {

        @Override
        public void heartbeatTimeout(SoupBinTCPClient session) {
            error("Heartbeat timeout");
        }

        @Override
        public void endOfSession(SoupBinTCPClient session) {
        }

        @Override
        public void loginAccepted(SoupBinTCPClient session, SoupBinTCP.LoginAccepted payload) {
        }

        @Override
        public void loginRejected(SoupBinTCPClient session, SoupBinTCP.LoginRejected payload) {
            error("Login rejected");
        }

    }

}
