package org.jvirtanen.parity.sim;

import org.jvirtanen.parity.net.pmd.PMD;
import org.jvirtanen.parity.net.pmd.PMDListener;
import org.jvirtanen.parity.top.Market;
import org.jvirtanen.parity.top.Side;

class MarketDataProcessor implements PMDListener {

    private Market market;

    public MarketDataProcessor(Market market) {
        this.market = market;
    }

    @Override
    public void version(PMD.Version message) {
    }

    @Override
    public void seconds(PMD.Seconds message) {
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

    @Override
    public void orderDeleted(PMD.OrderDeleted message) {
        market.delete(message.orderNumber);
    }

    @Override
    public void brokenTrade(PMD.BrokenTrade message) {
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
