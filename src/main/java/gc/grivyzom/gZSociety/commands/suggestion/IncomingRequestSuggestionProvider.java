package gc.grivyzom.gZSociety.commands.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import gc.grivyzom.gZSociety.manager.PlayerManager;
import gc.grivyzom.gZSociety.objects.SocialPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides suggestions based on the player's incoming friend requests.
 * Used for commands like /friend accept and /friend deny.
 */
public class IncomingRequestSuggestionProvider implements SuggestionProvider<CommandSource> {

    private final PlayerManager playerManager;

    public IncomingRequestSuggestionProvider(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public static IncomingRequestSuggestionProvider create(PlayerManager playerManager) {
        return new IncomingRequestSuggestionProvider(playerManager);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSource> context,
            SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof Player player)) {
            return builder.buildFuture();
        }

        SocialPlayer socialPlayer = playerManager.getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemainingLowerCase();

        // Suggest players who have sent us friend requests
        for (UUID senderId : socialPlayer.getIncomingRequests()) {
            // Try to get the player name from loaded players
            SocialPlayer senderPlayer = playerManager.getPlayer(senderId);
            if (senderPlayer != null) {
                String senderName = senderPlayer.getPlayerName();
                if (senderName.toLowerCase().startsWith(remaining)) {
                    builder.suggest(senderName);
                }
            }
        }

        return builder.buildFuture();
    }
}
