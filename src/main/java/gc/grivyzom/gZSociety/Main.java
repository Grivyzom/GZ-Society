package gc.grivyzom.gZSociety;

import com.google.inject.Inject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import gc.grivyzom.gZSociety.commands.FriendCommand;
import gc.grivyzom.gZSociety.commands.SocietyAdminCommand;
import gc.grivyzom.gZSociety.config.ConfigManager;
import gc.grivyzom.gZSociety.config.LanguageManager;
import gc.grivyzom.gZSociety.listeners.FriendNotificationListener;
import gc.grivyzom.gZSociety.listeners.PlayerConnectionListener;
import gc.grivyzom.gZSociety.manager.PlayerManager;
import gc.grivyzom.gZSociety.storage.InMemoryStorage;
import gc.grivyzom.gZSociety.storage.SQLStorage;
import gc.grivyzom.gZSociety.storage.Storage;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "gzsociety", name = "GZ-Society", version = "1.2.0-SNAPSHOT", description = "A professional society and friends plugin.", authors = {
        "GrivyZom" })
public class Main {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private PlayerManager playerManager;
    private Storage storage;

    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GZ-Society is initializing...");

        // 1. Initialize Configuration
        this.configManager = new ConfigManager(dataDirectory);
        try {
            configManager.load();
            logger.info("Configuration loaded successfully.");
        } catch (Exception e) {
            logger.error("Failed to load configuration! Disabling plugin.", e);
            return;
        }

        // 2. Initialize Language Manager
        this.languageManager = new LanguageManager(dataDirectory, logger);
        try {
            languageManager.load(configManager.getLanguage());
            logger.info("Language '{}' loaded successfully.", configManager.getLanguage());
        } catch (Exception e) {
            logger.error("Failed to load language files!", e);
            return;
        }

        // 3. Initialize Storage
        String storageType = configManager.getStorageType();
        logger.info("Using storage type: " + storageType);
        if (storageType.equalsIgnoreCase("mysql")) {
            try {
                SQLStorage sqlStorage = new SQLStorage(configManager.getMySqlSettings());
                sqlStorage.initDatabase();
                this.storage = sqlStorage;
                logger.info("MySQL storage initialized successfully.");
            } catch (Exception e) {
                logger.error("Failed to initialize MySQL storage! Falling back to in-memory.", e);
                this.storage = new InMemoryStorage();
            }
        } else {
            this.storage = new InMemoryStorage();
        }

        // 4. Initialize Managers
        this.playerManager = new PlayerManager(storage);

        // 5. Register Listeners
        server.getEventManager().register(this, new PlayerConnectionListener(playerManager));
        server.getEventManager().register(this, new FriendNotificationListener(this));

        // 6. Register Commands with localized aliases
        registerCommands();

        logger.info("GZ-Society has been enabled successfully!");
    }

    /**
     * Registers all commands with their localized aliases.
     */
    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();

        // Register Friend command
        BrigadierCommand friendCommand = FriendCommand.create(this);
        commandManager.register(friendCommand);

        // Register localized alias for friend command (e.g., /amigo in Spanish)
        String localizedFriendName = languageManager.getCommandName("friend");
        if (!localizedFriendName.equals("friend")) {
            // Only register alias if it's different from base command
            commandManager.register(
                    new BrigadierCommand(
                            LiteralArgumentBuilder.<CommandSource>literal(localizedFriendName)
                                    .redirect(friendCommand.getNode())
                                    .build()));
            logger.info("Registered command alias: /{} -> /friend", localizedFriendName);
        }

        // Register Admin command
        commandManager.register(SocietyAdminCommand.create(this));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("GZ-Society is disabling...");
        if (storage instanceof SQLStorage) {
            ((SQLStorage) storage).close();
            logger.info("Database connection pool closed.");
        }
    }

    // Getters
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public Storage getStorage() {
        return storage;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}