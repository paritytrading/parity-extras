package org.jvirtanen.parity.sim;

import static org.jvirtanen.util.Applications.*;

import com.paritytrading.foundation.ASCII;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import org.jvirtanen.config.Configs;

class Simulator {

    private static final long PRICE_FACTOR = 10000;

    public static void main(String[] args) {
        if (args.length != 1)
            usage("parity-sim <configuration-file>");

        try {
            main(config(args[0]));
        } catch (ConfigException | FileNotFoundException e) {
            error(e);
        } catch (IOException e) {
            fatal(e);
        }
    }

    private static void main(Config config) throws IOException {
        MarketData marketData = marketData(config);

        List<Agent> agents = new ArrayList<>();

        agents.add(open(config));

        Model.Config modelConfig = modelConfig(config);

        for (int i = 0; i < modelConfig.n(); i++)
            agents.add(model(config, modelConfig));

        String usernamePrefix = config.getString("order-entry.username-prefix");
        String password       = config.getString("order-entry.password");

        for (int i = 0; i < agents.size(); i++) {
            String username = String.format("%3.3s%03d", usernamePrefix, i + 1);

            agents.get(i).getOrderEntry().login(username, password);
        }

        new Events(marketData, agents).run();
    }

    private static MarketData marketData(Config config) throws IOException {
        NetworkInterface multicastInterface = Configs.getNetworkInterface(config, "market-data.multicast-interface");
        InetAddress      multicastGroup     = Configs.getInetAddress(config, "market-data.multicast-group");
        int              multicastPort      = Configs.getPort(config, "market-data.multicast-port");
        InetAddress      requestAddress     = Configs.getInetAddress(config, "market-data.request-address");
        int              requestPort        = Configs.getPort(config, "market-data.request-port");

        String instrument = config.getString("instrument");

        return MarketData.open(multicastInterface,
                new InetSocketAddress(multicastGroup, multicastPort),
                new InetSocketAddress(requestAddress, requestPort),
                ASCII.packLong(instrument));
    }

    private static OrderEntry orderEntry(Config config) throws IOException {
        InetAddress address = Configs.getInetAddress(config, "order-entry.address");
        int         port    = Configs.getPort(config, "order-entry.port");

        return OrderEntry.open(new InetSocketAddress(address, port));
    }

    private static Open open(Config config) throws IOException {
        double bidPrice = config.getDouble("open.bid.price");
        long   bidSize  = config.getLong("open.bid.size");
        double askPrice = config.getDouble("open.ask.price");
        long   askSize  = config.getLong("open.ask.size");

        String instrument = config.getString("instrument");

        return new Open(orderEntry(config), ASCII.packLong(instrument),
                (long)(bidPrice * PRICE_FACTOR), bidSize,
                (long)(askPrice * PRICE_FACTOR), askSize);
    }

    private static Model model(Config config, Model.Config modelConfig) throws IOException {
        String instrument = config.getString("instrument");

        return new Model(orderEntry(config), modelConfig, ASCII.packLong(instrument));
    }

    private static Model.Config modelConfig(Config config) {
        double n     = config.getDouble("model.n");
        double s     = config.getDouble("model.s");
        double mu    = config.getDouble("model.mu");
        double delta = config.getDouble("model.delta");
        long   sigma = config.getLong("model.sigma");

        return new Model.Config(n, s, mu, delta, sigma);
    }

}
