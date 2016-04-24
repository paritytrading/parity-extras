package org.jvirtanen.parity.sim;

import static org.jvirtanen.util.Applications.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class Events {

    private static final int TIMEOUT_MILLIS = 100;

    private MarketData marketData;

    private List<Agent> agents;

    public Events(MarketData marketData, List<Agent> agents) {
        this.marketData = marketData;

        this.agents = agents;
    }

    public void run() throws IOException {
        Selector selector = Selector.open();

        SelectionKey marketDataKey = marketData.getTransport().getChannel().register(selector,
                SelectionKey.OP_READ, null);

        SelectionKey marketDataResponseKey = marketData.getTransport().getRequestChannel().register(selector,
                SelectionKey.OP_READ, null);

        long currentTimeMillis = System.currentTimeMillis();

        for (Agent agent : agents) {
            agent.getOrderEntry().getTransport().getChannel().register(selector, SelectionKey.OP_READ, agent);

            agent.start(currentTimeMillis);
        }

        while (true) {
            int numKeys = selector.select(TIMEOUT_MILLIS);

            if (numKeys > 0) {
                Set<SelectionKey> keys = selector.selectedKeys();

                if (keys.contains(marketDataResponseKey)) {
                    marketData.getTransport().receiveResponse();

                    keys.remove(marketDataResponseKey);
                }

                if (keys.contains(marketDataKey)) {
                    marketData.getTransport().receive();

                    keys.remove(marketDataKey);
                }

                if (keys.isEmpty())
                    continue;

                for (SelectionKey key : keys) {
                    Agent agent = (Agent)key.attachment();

                    if (agent.getOrderEntry().getTransport().receive() < 0)
                        error("Connection closed");
                }

                keys.clear();
            }

            tick();
        }
    }

    private void tick() throws IOException {
        long currentTimeMillis = System.currentTimeMillis();

        for (int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);

            agent.tick(marketData.getTopOfBook(), currentTimeMillis);

            agent.getOrderEntry().getTransport().keepAlive();
        }
    }

}
