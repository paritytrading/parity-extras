package com.paritytrading.parity.sim;

import com.paritytrading.nassau.soupbintcp.SoupBinTCPClient;
import com.paritytrading.parity.util.OrderIDGenerator;
import java.io.IOException;

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
