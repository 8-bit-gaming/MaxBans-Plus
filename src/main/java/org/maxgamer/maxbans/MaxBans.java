package org.maxgamer.maxbans;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.flywaydb.core.Flyway;
import org.maxgamer.maxbans.command.*;
import org.maxgamer.maxbans.config.JdbcConfig;
import org.maxgamer.maxbans.config.PluginConfig;
import org.maxgamer.maxbans.context.PluginContext;
import org.maxgamer.maxbans.exception.ConfigException;
import org.maxgamer.maxbans.listener.RestrictionListener;
import org.maxgamer.maxbans.locale.Locale;

import java.io.File;

/**
 * @author Dirk Jamieson
 */
public class MaxBans extends JavaPlugin {
    private Locale locale = new Locale();
    private PluginContext context;
    private File messagesFile;

    @Override
    public void onLoad() {
        messagesFile = new File(getDataFolder(), "messages.yml");
    }
    
    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        
        if(!this.messagesFile.exists()) {
            this.saveResource(messagesFile.getName(), false);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        if(context != null) {
            try {
                context.getConfig().load(getConfig());
            } catch (ConfigException e) {
                getLogger().severe("Configuration failed validation at " + e.getSection().getCurrentPath() + ": " + e.getMessage());
                getPluginLoader().disablePlugin(this);
                return;
            }
        }
        
        ConfigurationSection localeConfig = YamlConfiguration.loadConfiguration(messagesFile);
        locale.load(localeConfig);
    }

    /**
     * Migrates flyway. If the database is not empty and no schema version is detected,
     * this will raise an exception.
     */
    public void migrate() {
        Flyway flyway = new Flyway();
        JdbcConfig jdbc = context.getConfig().getJdbcConfig();
        
        flyway.setClassLoader(getClass().getClassLoader());
        flyway.setDataSource(jdbc.getUrl(), jdbc.getUsername(), jdbc.getPassword());
        
        flyway.migrate();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        PluginConfig config;
        try {
            config = new PluginConfig(getConfig());
        } catch (ConfigException e) {
            getLogger().severe("Configuration failed validation at " + e.getSection().getCurrentPath() + ": " + e.getMessage());
            getPluginLoader().disablePlugin(this);
            return;
        }

        context = new PluginContext(config, getServer(), getDataFolder());
        
        migrate();
        
        RestrictionListener restrictionListener = new RestrictionListener(context.getTransactor(), context.getUserService(), context.getLockdownService());
        
        getServer().getPluginManager().registerEvents(restrictionListener, this);
        getCommand("ban").setExecutor(new BanCommandExecutor(context.getLocatorService(), context.getUserService(), context.getBroadcastService(), locale));
        getCommand("mute").setExecutor(new MuteCommandExecutor(context.getLocatorService(), context.getUserService(), context.getBroadcastService(), locale));
        getCommand("iplookup").setExecutor(new IPLookupCommandExecutor(locale, context.getLocatorService(), context.getAddressService()));
        getCommand("kick").setExecutor(new KickCommand(locale, context.getLocatorService(), context.getBroadcastService()));
        getCommand("warn").setExecutor(new WarnCommandExecutor(locale, context.getLocatorService(), context.getUserService(), context.getWarningService()));
    }

    @Override
    public void onDisable() {
        
    }
}
