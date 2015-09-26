package org.jvirtanen.parity.sim;

import java.io.IOException;
import org.jvirtanen.nassau.soupbintcp.SoupBinTCPClient;

abstract class Agent {

    private OrderEntry orderEntry;

    protected OrderIDGenerator orderId;

    protected Agent(OrderEntry orderEntry) {
        this.orderEntry = orderEntry;

        this.orderId = new OrderIDGenerator();
    }

    public OrderEntry getOrderEntry() {
        return orderEntry;
    }

    abstract void start(long currentTimeMillis) throws IOException;

    abstract void tick(MarketData.TopOfBook topOfBook, long currentTimeMillis) throws IOException;

}
