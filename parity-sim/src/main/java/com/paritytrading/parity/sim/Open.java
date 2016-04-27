package com.paritytrading.parity.sim;

import com.paritytrading.parity.net.poe.POE;
import java.io.IOException;

class Open extends Agent {

    private POE.EnterOrder enterBuyOrder;
    private POE.EnterOrder enterSellOrder;

    public Open(OrderEntry orderEntry, long instrument, long bidPrice, long bidSize,
            long askPrice, long askSize) {
        super(orderEntry);

        this.enterBuyOrder = new POE.EnterOrder();

        this.enterBuyOrder.orderId    = orderId.next();
        this.enterBuyOrder.side       = POE.BUY;
        this.enterBuyOrder.instrument = instrument;
        this.enterBuyOrder.quantity   = bidSize;
        this.enterBuyOrder.price      = bidPrice;

        this.enterSellOrder = new POE.EnterOrder();

        this.enterSellOrder.orderId    = orderId.next();
        this.enterSellOrder.side       = POE.SELL;
        this.enterSellOrder.instrument = instrument;
        this.enterSellOrder.quantity   = askSize;
        this.enterSellOrder.price      = askPrice;
    }

    @Override
    public void start(long currentTimeMillis) throws IOException {
        getOrderEntry().send(enterBuyOrder);
        getOrderEntry().send(enterSellOrder);
    }

    @Override
    public void tick(MarketData.TopOfBook topOfBook, long currentTimeMillis) {
    }

}
