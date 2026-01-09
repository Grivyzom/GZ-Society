package gc.grivyzom.gZSociety.commands.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.CompletableFuture;

/**
 * Provides online player name suggestions for tab completion.
 * This is a reusable suggestion provider that can be applied to any command
 * that needs player name autocomplete.
 */
public class PlayerSuggestionProvider implements SuggestionProvider<CommandSource> {

    private final ProxyServer server;
    private final boolean excludeSelf;

    /**
     * Creates a new PlayerSuggestionProvider.
     *
     * @param server      The proxy server instance.
     * @param excludeSelf If true, excludes the command executor from suggestions.
     */
    public PlayerSuggestionProvider(ProxyServer server, boolean excludeSelf) {
        this.server = server;
        this.excludeSelf = excludeSelf;
    }

    /**
     * Creates a provider that includes all online players.
     */
    public static PlayerSuggestionProvider allPlayers(ProxyServer server) {
        return new PlayerSuggestionProvider(server, false);
    }

    /**
     * Creates a provider that excludes the command executor.
     */
    public static PlayerSuggestionProvider otherPlayers(ProxyServer server) {
        return new PlayerSuggestionProvider(server, true);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSource> context,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        for (Player player : server.getAllPlayers()) {
            // Exclude self if configured
            if (excludeSelf && context.getSource() instanceof Player source) {
                if (source.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
            }

            // Filter by input prefix
            if (player.getUsername().toLowerCase().startsWith(remaining)) {
                builder.suggest(player.getUsername());
            }
        }

        return builder.buildFuture();
    }
}
