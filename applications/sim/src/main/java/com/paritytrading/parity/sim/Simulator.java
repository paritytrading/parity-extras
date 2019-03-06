package com.paritytrading.parity.sim;

import static com.paritytrading.foundation.Longs.*;
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
        // Load instrument config.
        String instrument = config.getString("instrument");
        int priceFractionDigits = config.getInt("price-fraction-digits");
        int sizeFractionDigits  = config.getInt("size-fraction-digits");
        long priceFactor = POWERS_OF_TEN[priceFractionDigits];
        long sizeFactor  = POWERS_OF_TEN[sizeFractionDigits];

        MarketData marketData = marketData(config, instrument);

        List<Agent> agents = new ArrayList<>();

        agents.add(open(config, instrument, priceFactor, sizeFactor));

        Model.Config modelConfig = modelConfig(config);

        for (int i = 0; i < modelConfig.n(); i++)
            agents.add(model(config, modelConfig, instrument, priceFactor, sizeFactor));

        String usernamePrefix = config.getString("order-entry.username-prefix");
        String password       = config.getString("order-entry.password");

        for (int i = 0; i < agents.size(); i++) {
            String username = String.format("%3.3s%03d", usernamePrefix, i + 1);

            agents.get(i).getOrderEntry().login(username, password);
        }

        new Events(marketData, agents).run();
    }

    private static MarketData marketData(Config config, String instrument) throws IOException {
        NetworkInterface multicastInterface = Configs.getNetworkInterface(config, "market-data.multicast-interface");
        InetAddress      multicastGroup     = Configs.getInetAddress(config, "market-data.multicast-group");
        int              multicastPort      = Configs.getPort(config, "market-data.multicast-port");
        InetAddress      requestAddress     = Configs.getInetAddress(config, "market-data.request-address");
        int              requestPort        = Configs.getPort(config, "market-data.request-port");

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

    private static Open open(Config config, String instrument, long priceFactor, long sizeFactor) throws IOException {
        double bidPrice = config.getDouble("open.bid.price");
        double bidSize  = config.getDouble("open.bid.size");
        double askPrice = config.getDouble("open.ask.price");
        double askSize  = config.getDouble("open.ask.size");

        return new Open(orderEntry(config), ASCII.packLong(instrument),
                (long)(bidPrice * priceFactor),
                (long)(bidSize * sizeFactor),
                (long)(askPrice * sizeFactor),
                (long)(askSize * sizeFactor));
    }

    private static Model model(Config config, Model.Config modelConfig, String instrument, long priceFactor, long sizeFactor) throws IOException {
        return new Model(orderEntry(config), modelConfig, ASCII.packLong(instrument), priceFactor, sizeFactor);
    }

    private static Model.Config modelConfig(Config config) {
        double n     = config.getDouble("model.n");
        double s     = config.getDouble("model.s");
        double mu    = config.getDouble("model.mu");
        double delta = config.getDouble("model.delta");
        double sigma = config.getDouble("model.sigma");

        return new Model.Config(n, s, mu, delta, sigma);
    }

}
