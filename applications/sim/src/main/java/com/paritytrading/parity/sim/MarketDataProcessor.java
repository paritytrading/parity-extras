package com.paritytrading.parity.sim;

import com.paritytrading.parity.book.Market;
import com.paritytrading.parity.book.Side;
import com.paritytrading.parity.net.pmd.PMD;
import com.paritytrading.parity.net.pmd.PMDListener;

class MarketDataProcessor implements PMDListener {

    private Market market;

    public MarketDataProcessor(Market market) {
        this.market = market;
    }

    @Override
    public void version(PMD.Version message) {
    }

    @Override
    public void orderAdded(PMD.OrderAdded message) {
        market.add(message.instrument, message.orderNumber, side(message.side),
                message.price, message.quantity);
    }

    @Override
    public void orderExecuted(PMD.OrderExecuted message) {
        market.execute(message.orderNumber, message.quantity);
    }

    @Override
    public void orderCanceled(PMD.OrderCanceled message) {
        market.cancel(message.orderNumber, message.canceledQuantity);
    }

    private Side side(byte side) {
        switch (side) {
        case PMD.BUY:
            return Side.BUY;
        case PMD.SELL:
            return Side.SELL;
        }

        return null;
    }

}
