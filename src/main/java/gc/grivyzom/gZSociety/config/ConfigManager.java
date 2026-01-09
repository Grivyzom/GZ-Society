package gc.grivyzom.gZSociety.config;

import org.spongepowered.configurate.ConfigurationNode;
import me.lucko.configurate.toml.TOMLConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigManager {

    private final Path dataDirectory;
    private final Path configFile;
    private ConfigurationNode root;

    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configFile = dataDirectory.resolve("config.toml");
    }

    public void load() throws IOException {
        // Create data directory if it doesn't exist
        if (Files.notExists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }

        // Create config file from resources if it doesn't exist
        if (Files.notExists(configFile)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.toml")) {
                Files.copy(Objects.requireNonNull(in), configFile);
            }
        }

        // Load the configuration
        TOMLConfigurationLoader loader = TOMLConfigurationLoader.builder()
                .path(configFile)
                .build();

        this.root = loader.load();
    }

    public ConfigurationNode getRoot() {
        return root;
    }

    public String getStorageType() {
        return root.node("storage-type").getString("in-memory");
    }

    public ConfigurationNode getMySqlSettings() {
        return root.node("mysql");
    }

    public String getLanguage() {
        return root.node("language").getString("es");
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
