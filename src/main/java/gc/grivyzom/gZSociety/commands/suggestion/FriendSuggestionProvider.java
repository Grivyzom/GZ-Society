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
 * Provides suggestions based on the player's friend list.
 * Used for commands that require a friend name, like /friend remove.
 */
public class FriendSuggestionProvider implements SuggestionProvider<CommandSource> {

    private final PlayerManager playerManager;

    public FriendSuggestionProvider(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public static FriendSuggestionProvider create(PlayerManager playerManager) {
        return new FriendSuggestionProvider(playerManager);
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

        // Get friend names from cache (we need to store names, for now use UUID string)
        // In a production setup, you'd maintain a UUID -> Name mapping
        for (UUID friendId : socialPlayer.getFriends()) {
            // Try to get the player name from online players first
            SocialPlayer friendPlayer = playerManager.getPlayer(friendId);
            if (friendPlayer != null) {
                String friendName = friendPlayer.getPlayerName();
                if (friendName.toLowerCase().startsWith(remaining)) {
                    builder.suggest(friendName);
                }
            }
        }

        return builder.buildFuture();
    }
}
