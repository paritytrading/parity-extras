package com.paritytrading.parity.sim;

import com.paritytrading.foundation.ASCII;
import com.paritytrading.parity.net.poe.POE;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Random;
import org.apache.commons.math3.distribution.ExponentialDistribution;

class Model extends Agent {

    private Config config;

    private POE.EnterOrder  enterOrder;
    private POE.CancelOrder cancelOrder;

    private PriorityQueue<Order> orders;

    private ExponentialDistribution sleepDistribution;
    private ExponentialDistribution expirationDistribution;

    private Random uniformDistribution;

    private long sleepUntilMillis;

    public Model(OrderEntry orderEntry, Config config, long instrument) {
        super(orderEntry);

        this.config = config;

        this.enterOrder = new POE.EnterOrder();

        this.enterOrder.instrument = instrument;
        this.enterOrder.quantity   = config.sigma();

        this.cancelOrder = new POE.CancelOrder();

        this.orders = new PriorityQueue<>();

        this.sleepDistribution      = new ExponentialDistribution(config.tau());
        this.expirationDistribution = new ExponentialDistribution(1 / config.delta());

        this.uniformDistribution = new Random();
    }

    @Override
    public void start(long currentTimeMillis) {
        scheduleWakeUp(currentTimeMillis);
    }

    @Override
    public void tick(MarketData.TopOfBook topOfBook, long currentTimeMillis) throws IOException {
        expire(currentTimeMillis);

        if (currentTimeMillis < sleepUntilMillis)
            return;

        double askPrice = topOfBook.getAskPrice() / 10000.0;
        double bidPrice = topOfBook.getBidPrice() / 10000.0;

        if (uniformDistribution.nextDouble() < config.p()) {
            if (uniformDistribution.nextBoolean()) {
                if (askPrice > config.l()) {
                    double price = price(askPrice - config.l(), askPrice);

                    enter(POE.BUY, price, currentTimeMillis);
                }
            } else {
                if (bidPrice > 0) {
                    double price = price(bidPrice, bidPrice + config.l());

                    enter(POE.SELL, price, currentTimeMillis);
                }
            }
        } else {

            // As the trading system does not natively support market orders,
            // use marketable limit orders instead.
            if (uniformDistribution.nextBoolean()) {
                if (askPrice > 0)
                    enter(POE.BUY, askPrice + 1.00);
            } else {
                if (bidPrice > 1.00)
                    enter(POE.SELL, bidPrice - 1.00);
            }

        }

        scheduleWakeUp(currentTimeMillis);
    }

    private void expire(long currentTimeMillis) throws IOException {
        while (true) {
            Order order = orders.peek();
            if (order == null)
                break;

            if (order.getExpireTimeMillis() > currentTimeMillis)
                break;

            cancel(order.getOrderId());

            orders.poll();
        }
    }

    private double price(double min, double max) {
        return min + (max - min) * uniformDistribution.nextDouble();
    }

    private void enter(byte side, double price, long currentTimeMillis) throws IOException {
        enter(side, price);

        orders.offer(new Order(ASCII.get(enterOrder.orderId), scheduleExpiration(currentTimeMillis)));
    }

    private void enter(byte side, double price) throws IOException {
        ASCII.putLeft(enterOrder.orderId, orderId.next());
        enterOrder.side  = side;
        enterOrder.price = (long)Math.round(price * 100.0) * 100;

        getOrderEntry().send(enterOrder);
    }

    private void cancel(String orderId) throws IOException {
        ASCII.putLeft(cancelOrder.orderId, orderId);
        cancelOrder.quantity = 0;

        getOrderEntry().send(cancelOrder);
    }

    private void scheduleWakeUp(long currentTimeMillis) {
        sleepUntilMillis = currentTimeMillis + (long)(sleepDistribution.sample() * 1000);
    }

    private long scheduleExpiration(long currentTimeMillis) {
        return currentTimeMillis + (long)(expirationDistribution.sample() * 1000);
    }

    private static class Order implements Comparable<Order> {

        private String orderId;

        private long expireTimeMillis;

        Order(String orderId, long expireTimeMillis) {
            this.orderId = orderId;

            this.expireTimeMillis = expireTimeMillis;
        }

        String getOrderId() {
            return orderId;
        }

        long getExpireTimeMillis() {
            return expireTimeMillis;
        }

        @Override
        public int compareTo(Order order) {
            return Long.compare(expireTimeMillis, order.expireTimeMillis);
        }

    }

    public static class Config {

        private double n;
        private double s;
        private double mu;
        private double delta;
        private long   sigma;

        public Config(double n, double s, double mu, double delta, long sigma) {
            this.n     = n;
            this.s     = s;
            this.mu    = mu;
            this.delta = delta;
            this.sigma = sigma;
        }

        public double n() {
            return n;
        }

        public double s() {
            return s;
        }

        public double mu() {
            return mu;
        }

        public double delta() {
            return delta;
        }

        public long sigma() {
            return sigma;
        }

        public double alpha() {
            return (mu() + 2 * delta()) / s();
        }

        public double l() {
            return 75 * s();
        }

        public double tau() {
            return n() / (mu() + alpha() * l());
        }

        public double p() {
            return (alpha() * l()) / (mu() + alpha() * l());
        }

    }

}
