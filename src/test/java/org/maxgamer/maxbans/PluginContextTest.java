package org.maxgamer.maxbans;

import org.bukkit.Server;
import org.junit.After;
import org.junit.Before;
import org.maxgamer.maxbans.config.PluginConfig;
import org.maxgamer.maxbans.context.PluginContext;
import org.maxgamer.maxbans.repository.H2Test;
import org.maxgamer.maxbans.service.MetricService;
import org.maxgamer.maxbans.test.IntegrationTest;

import java.io.File;

import static org.mockito.Mockito.mock;

/**
 * @author Dirk Jamieson <dirk@redeye.co>
 */
public class PluginContextTest extends H2Test implements IntegrationTest {
    private PluginContext context;
    
    @Before
    public void init() {
        super.init();

        PluginConfig config = new PluginConfig();
        config.setJdbcConfig(getJdbc());
        Server server = mock(Server.class);
        File folder = new File("maxbans");
        if(!folder.isDirectory() && !folder.mkdir()) {
            throw new IllegalStateException("Can't create data folder");
        }

        context = new PluginContext(config, server, folder, mock(MetricService.class));
    }

    @After
    public void destroy() {
        if(context == null) return;
        if(context.getDataFolder() == null) return;

        File[] files = context.getDataFolder().listFiles();
        if(files != null) {
            for (File f : context.getDataFolder().listFiles()) {
                f.delete();
            }
        }

        context.getDataFolder().delete();
    }

    public PluginContext getContext() {
        return context;
    }
}
