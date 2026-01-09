package gc.grivyzom.gZSociety.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages language files and message translations.
 * Loads YAML language files from the lang/ folder.
 */
public class LanguageManager {

    private final Path langDirectory;
    private final Logger logger;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, String> commandNames = new HashMap<>();
    private String currentLanguage;

    public LanguageManager(Path dataDirectory, Logger logger) {
        this.langDirectory = dataDirectory.resolve("lang");
        this.logger = logger;
    }

    /**
     * Loads the specified language file.
     *
     * @param language The language code (e.g., "es", "en")
     */
    public void load(String language) throws IOException {
        this.currentLanguage = language;

        // Create lang directory if it doesn't exist
        if (Files.notExists(langDirectory)) {
            Files.createDirectories(langDirectory);
        }

        // Copy default language files if they don't exist
        copyDefaultLanguageFile("es.yml");
        copyDefaultLanguageFile("en.yml");

        // Load the selected language file
        Path langFile = langDirectory.resolve(language + ".yml");

        if (Files.notExists(langFile)) {
            logger.warn("Language file '{}' not found, falling back to 'es'", language);
            langFile = langDirectory.resolve("es.yml");
            this.currentLanguage = "es";
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(langFile)
                .build();

        ConfigurationNode root = loader.load();

        // Load all messages recursively
        messages.clear();
        commandNames.clear();
        loadMessages(root, "");

        // Load command names specifically
        ConfigurationNode commandsNode = root.node("commands");
        if (!commandsNode.virtual()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : commandsNode.childrenMap().entrySet()) {
                String cmdKey = entry.getKey().toString();
                String cmdName = entry.getValue().getString(cmdKey);
                commandNames.put(cmdKey, cmdName);
            }
        }

        logger.info("Loaded {} messages and {} command names for language '{}'",
                messages.size(), commandNames.size(), currentLanguage);
    }

    /**
     * Recursively loads messages from the configuration node.
     */
    private void loadMessages(ConfigurationNode node, String prefix) {
        if (node.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                String key = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey().toString();
                loadMessages(entry.getValue(), key);
            }
        } else {
            String value = node.getString("");
            if (!value.isEmpty()) {
                messages.put(prefix, value);
            }
        }
    }

    /**
     * Copies a default language file from resources if it doesn't exist.
     */
    private void copyDefaultLanguageFile(String filename) {
        Path targetFile = langDirectory.resolve(filename);
        if (Files.notExists(targetFile)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("lang/" + filename)) {
                if (in != null) {
                    Files.copy(in, targetFile);
                    logger.info("Created default language file: {}", filename);
                }
            } catch (IOException e) {
                logger.error("Failed to copy default language file: {}", filename, e);
            }
        }
    }

    /**
     * Gets a message by key.
     *
     * @param key The message key
     * @return The translated message, or the key itself if not found
     */
    public String getMessage(String key) {
        String message = messages.getOrDefault(key, "<red>Missing: " + key + "</red>");
        // Auto-replace {cmd} with the localized friend command name
        return message.replace("{cmd}", getCommandName("friend"));
    }

    /**
     * Gets a message and replaces placeholders.
     *
     * @param key          The message key
     * @param placeholders Pairs of placeholder-value (e.g., "{player}", "Steve")
     * @return The translated message with placeholders replaced
     */
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return message;
    }

    /**
     * Gets a localized command name.
     *
     * @param commandKey The command key (e.g., "friend")
     * @return The localized command name (e.g., "amigo" in Spanish)
     */
    public String getCommandName(String commandKey) {
        return commandNames.getOrDefault(commandKey, commandKey);
    }

    /**
     * Gets all command names map.
     */
    public Map<String, String> getCommandNames() {
        return new HashMap<>(commandNames);
    }

    /**
     * Gets the current language code.
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Reloads the current language.
     */
    public void reload() throws IOException {
        load(currentLanguage);
    }
}
