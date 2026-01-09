package gc.grivyzom.gZSociety.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import gc.grivyzom.gZSociety.Main;
import gc.grivyzom.gZSociety.commands.suggestion.FriendSuggestionProvider;
import gc.grivyzom.gZSociety.commands.suggestion.IncomingRequestSuggestionProvider;
import gc.grivyzom.gZSociety.commands.suggestion.PlayerSuggestionProvider;
import gc.grivyzom.gZSociety.objects.SocialPlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Optional;
import java.util.UUID;

public final class FriendCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static BrigadierCommand create(Main plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("friend")
                // Base command: /friend (shows help)
                .executes(ctx -> {
                    String helpMessage = plugin.getLanguageManager().getMessage("friend-help");
                    ctx.getSource().sendMessage(MINI_MESSAGE.deserialize(helpMessage));
                    return Command.SINGLE_SUCCESS;
                })
                // Subcommand: /friend request <player>
                .then(LiteralArgumentBuilder.<CommandSource>literal("request")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(PlayerSuggestionProvider.otherPlayers(plugin.getServer()))
                                .executes(ctx -> executeRequest(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), plugin)))
                        .executes(ctx -> {
                            ctx.getSource().sendMessage(
                                    MINI_MESSAGE.deserialize("<red>Usage: /friend request <player></red>"));
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand: /friend accept <player>
                .then(LiteralArgumentBuilder.<CommandSource>literal("accept")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(IncomingRequestSuggestionProvider.create(plugin.getPlayerManager()))
                                .executes(ctx -> executeAccept(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), plugin)))
                        .executes(ctx -> {
                            ctx.getSource()
                                    .sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /friend accept <player></red>"));
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand: /friend deny <player>
                .then(LiteralArgumentBuilder.<CommandSource>literal("deny")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(IncomingRequestSuggestionProvider.create(plugin.getPlayerManager()))
                                .executes(ctx -> executeDeny(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), plugin)))
                        .executes(ctx -> {
                            ctx.getSource()
                                    .sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /friend deny <player></red>"));
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand: /friend requests
                .then(LiteralArgumentBuilder.<CommandSource>literal("requests")
                        .executes(ctx -> executeRequests(ctx.getSource(), plugin)))
                // Subcommand: /friend remove <player>
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(FriendSuggestionProvider.create(plugin.getPlayerManager()))
                                .executes(ctx -> executeRemove(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), plugin)))
                        .executes(ctx -> {
                            ctx.getSource()
                                    .sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /friend remove <player></red>"));
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand: /friend best <player>
                .then(LiteralArgumentBuilder.<CommandSource>literal("best")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(FriendSuggestionProvider.create(plugin.getPlayerManager()))
                                .executes(ctx -> executeBest(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), plugin)))
                        .executes(ctx -> {
                            ctx.getSource()
                                    .sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /friend best <player></red>"));
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand: /friend unbest <player>
                .then(LiteralArgumentBuilder.<CommandSource>literal("unbest")
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(FriendSuggestionProvider.create(plugin.getPlayerManager()))
                                .executes(ctx -> executeUnbest(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"), plugin)))
                        .executes(ctx -> {
                            ctx.getSource()
                                    .sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /friend unbest <player></red>"));
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand: /friend list
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .executes(ctx -> executeList(ctx.getSource(), plugin)))
                // Subcommand: /friend notifications [on|off]
                .then(LiteralArgumentBuilder.<CommandSource>literal("notifications")
                        .executes(ctx -> executeNotifications(ctx.getSource(), plugin, null))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("on")
                                .executes(ctx -> executeNotifications(ctx.getSource(), plugin, true)))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("off")
                                .executes(ctx -> executeNotifications(ctx.getSource(), plugin, false))))
                .build();

        return new BrigadierCommand(node);
    }

    // ==================== FRIEND REQUEST ====================

    private static int executeRequest(CommandSource source, String targetName, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        if (player.getUsername().equalsIgnoreCase(targetName)) {
            source.sendMessage(
                    MINI_MESSAGE.deserialize(plugin.getLanguageManager().getMessage("friend-cannot-add-self")));
            return Command.SINGLE_SUCCESS;
        }

        Optional<Player> targetOptional = plugin.getServer().getPlayer(targetName);
        if (targetOptional.isEmpty()) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        Player target = targetOptional.get();
        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        SocialPlayer targetSocialPlayer = plugin.getPlayerManager().getPlayer(target.getUniqueId());

        if (socialPlayer == null || targetSocialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Player data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        // Check if target has blocked us
        if (targetSocialPlayer.hasBlocked(player.getUniqueId())) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        // Check if already friends
        if (socialPlayer.getFriends().contains(target.getUniqueId())) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("friend-already-added").replace("{player}",
                            target.getUsername())));
            return Command.SINGLE_SUCCESS;
        }

        // Check if already sent request
        if (socialPlayer.hasSentRequestTo(target.getUniqueId())) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("request-already-sent").replace("{player}",
                            target.getUsername())));
            return Command.SINGLE_SUCCESS;
        }

        // Check if target already sent us a request (auto-accept)
        if (socialPlayer.hasPendingRequestFrom(target.getUniqueId())) {
            return executeAccept(source, targetName, plugin);
        }

        // Send request
        socialPlayer.sendRequest(target.getUniqueId());
        targetSocialPlayer.receiveRequest(player.getUniqueId());

        // Save both players
        plugin.getStorage().savePlayer(socialPlayer);
        plugin.getStorage().savePlayer(targetSocialPlayer);

        // Notify both players
        source.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("request-sent").replace("{player}", target.getUsername())));
        target.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("request-received").replace("{player}", player.getUsername())));

        plugin.getLogger().info("{} sent friend request to {}", player.getUsername(), target.getUsername());
        return Command.SINGLE_SUCCESS;
    }

    // ==================== ACCEPT REQUEST ====================

    private static int executeAccept(CommandSource source, String senderName, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        // Find sender by name
        UUID senderUUID = findPlayerUUID(senderName, plugin);
        if (senderUUID == null) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", senderName)));
            return Command.SINGLE_SUCCESS;
        }

        // Check if we have a request from this player
        if (!socialPlayer.hasPendingRequestFrom(senderUUID)) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("request-not-found").replace("{player}", senderName)));
            return Command.SINGLE_SUCCESS;
        }

        // Accept request
        socialPlayer.acceptRequest(senderUUID);
        socialPlayer.addFriend(senderUUID);

        // Update sender's data
        SocialPlayer senderSocialPlayer = plugin.getPlayerManager().getPlayer(senderUUID);
        if (senderSocialPlayer != null) {
            senderSocialPlayer.cancelRequest(player.getUniqueId());
            senderSocialPlayer.addFriend(player.getUniqueId());
            plugin.getStorage().savePlayer(senderSocialPlayer);

            // Notify sender if online
            Optional<Player> senderOnline = plugin.getServer().getPlayer(senderUUID);
            senderOnline.ifPresent(sender -> sender.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("request-accepted-notify").replace("{player}",
                            player.getUsername()))));
        }

        plugin.getStorage().savePlayer(socialPlayer);

        source.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("request-accepted").replace("{player}", senderName)));

        plugin.getLogger().info("{} accepted friend request from {}", player.getUsername(), senderName);
        return Command.SINGLE_SUCCESS;
    }

    // ==================== DENY REQUEST ====================

    private static int executeDeny(CommandSource source, String senderName, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        UUID senderUUID = findPlayerUUID(senderName, plugin);
        if (senderUUID == null) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", senderName)));
            return Command.SINGLE_SUCCESS;
        }

        if (!socialPlayer.denyRequest(senderUUID)) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("request-not-found").replace("{player}", senderName)));
            return Command.SINGLE_SUCCESS;
        }

        // Also remove from sender's outgoing
        SocialPlayer senderSocialPlayer = plugin.getPlayerManager().getPlayer(senderUUID);
        if (senderSocialPlayer != null) {
            senderSocialPlayer.cancelRequest(player.getUniqueId());
            plugin.getStorage().savePlayer(senderSocialPlayer);
        }

        plugin.getStorage().savePlayer(socialPlayer);

        source.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("request-denied").replace("{player}", senderName)));

        plugin.getLogger().info("{} denied friend request from {}", player.getUsername(), senderName);
        return Command.SINGLE_SUCCESS;
    }

    // ==================== LIST REQUESTS ====================

    private static int executeRequests(CommandSource source, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        source.sendMessage(MINI_MESSAGE.deserialize(plugin.getLanguageManager().getMessage("requests-list-header")));

        // Incoming requests
        if (socialPlayer.getIncomingRequests().isEmpty()) {
            source.sendMessage(MINI_MESSAGE.deserialize("<gray>No incoming requests.</gray>"));
        } else {
            source.sendMessage(MINI_MESSAGE.deserialize("<yellow>Incoming:</yellow>"));
            for (UUID senderId : socialPlayer.getIncomingRequests()) {
                String name = getPlayerName(senderId, plugin);
                source.sendMessage(MINI_MESSAGE
                        .deserialize("  <green>• " + name + "</green> <gray>[/friend accept " + name + "]</gray>"));
            }
        }

        // Outgoing requests
        if (socialPlayer.getOutgoingRequests().isEmpty()) {
            source.sendMessage(MINI_MESSAGE.deserialize("<gray>No outgoing requests.</gray>"));
        } else {
            source.sendMessage(MINI_MESSAGE.deserialize("<yellow>Outgoing:</yellow>"));
            for (UUID receiverId : socialPlayer.getOutgoingRequests()) {
                String name = getPlayerName(receiverId, plugin);
                source.sendMessage(MINI_MESSAGE.deserialize("  <aqua>• " + name + "</aqua> <gray>(pending)</gray>"));
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    // ==================== REMOVE FRIEND ====================

    private static int executeRemove(CommandSource source, String targetName, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetUUID = findPlayerUUID(targetName, plugin);
        if (targetUUID == null) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        if (!socialPlayer.getFriends().contains(targetUUID)) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("friend-not-in-list").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        socialPlayer.removeFriend(targetUUID);
        plugin.getStorage().savePlayer(socialPlayer);

        // Also remove from target's list if online
        SocialPlayer targetSocialPlayer = plugin.getPlayerManager().getPlayer(targetUUID);
        if (targetSocialPlayer != null) {
            targetSocialPlayer.removeFriend(player.getUniqueId());
            plugin.getStorage().savePlayer(targetSocialPlayer);
        }

        source.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("friend-removed").replace("{player}", targetName)));

        plugin.getLogger().info("{} removed {} from friends", player.getUsername(), targetName);
        return Command.SINGLE_SUCCESS;
    }

    // ==================== BEST FRIEND ====================

    private static int executeBest(CommandSource source, String targetName, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetUUID = findPlayerUUID(targetName, plugin);
        if (targetUUID == null) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        if (!socialPlayer.getFriends().contains(targetUUID)) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("not-a-friend").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        if (socialPlayer.isBestFriend(targetUUID)) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("already-best-friend").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        socialPlayer.addBestFriend(targetUUID);
        plugin.getStorage().savePlayer(socialPlayer);

        source.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("best-friend-added").replace("{player}", targetName)));

        plugin.getLogger().info("{} marked {} as best friend", player.getUsername(), targetName);
        return Command.SINGLE_SUCCESS;
    }

    // ==================== UNBEST FRIEND ====================

    private static int executeUnbest(CommandSource source, String targetName, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetUUID = findPlayerUUID(targetName, plugin);
        if (targetUUID == null) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("player-not-found").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        if (!socialPlayer.isBestFriend(targetUUID)) {
            source.sendMessage(MINI_MESSAGE.deserialize(
                    plugin.getLanguageManager().getMessage("not-best-friend").replace("{player}", targetName)));
            return Command.SINGLE_SUCCESS;
        }

        socialPlayer.removeBestFriend(targetUUID);
        plugin.getStorage().savePlayer(socialPlayer);

        source.sendMessage(MINI_MESSAGE.deserialize(
                plugin.getLanguageManager().getMessage("best-friend-removed").replace("{player}", targetName)));

        plugin.getLogger().info("{} removed {} from best friends", player.getUsername(), targetName);
        return Command.SINGLE_SUCCESS;
    }

    // ==================== LIST FRIENDS ====================

    private static int executeList(CommandSource source, Main plugin) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is still loading. Please try again.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        if (socialPlayer.getFriends().isEmpty()) {
            source.sendMessage(MINI_MESSAGE.deserialize(plugin.getLanguageManager().getMessage("friend-list-empty")));
            return Command.SINGLE_SUCCESS;
        }

        source.sendMessage(MINI_MESSAGE.deserialize(plugin.getLanguageManager().getMessage("friend-list-header")));

        for (UUID friendId : socialPlayer.getFriends()) {
            Optional<Player> friendOnline = plugin.getServer().getPlayer(friendId);
            String name = getPlayerName(friendId, plugin);
            boolean isBest = socialPlayer.isBestFriend(friendId);

            String prefix = isBest ? "<gold>★</gold> " : "";
            String status = friendOnline.isPresent() ? "<green>●</green>" : "<gray>○</gray>";

            source.sendMessage(MINI_MESSAGE.deserialize(prefix + status + " " + name));
        }

        return Command.SINGLE_SUCCESS;
    }

    // ==================== UTILITIES ====================

    private static UUID findPlayerUUID(String name, Main plugin) {
        // Check online players first
        Optional<Player> online = plugin.getServer().getPlayer(name);
        if (online.isPresent()) {
            return online.get().getUniqueId();
        }

        // Check loaded players cache
        for (SocialPlayer loaded : plugin.getPlayerManager().getLoadedPlayers().values()) {
            if (loaded.getPlayerName().equalsIgnoreCase(name)) {
                return loaded.getPlayerId();
            }
        }

        return null;
    }

    private static String getPlayerName(UUID uuid, Main plugin) {
        // Check online players
        Optional<Player> online = plugin.getServer().getPlayer(uuid);
        if (online.isPresent()) {
            return online.get().getUsername();
        }

        // Check loaded cache
        SocialPlayer loaded = plugin.getPlayerManager().getPlayer(uuid);
        if (loaded != null) {
            return loaded.getPlayerName();
        }

        return uuid.toString().substring(0, 8);
    }

    // ==================== NOTIFICATIONS TOGGLE ====================

    private static int executeNotifications(CommandSource source, Main plugin, Boolean enabled) {
        if (!(source instanceof Player player)) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>This command can only be used by players.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (socialPlayer == null) {
            source.sendMessage(MINI_MESSAGE.deserialize("<red>Your data is not loaded. Please reconnect.</red>"));
            return Command.SINGLE_SUCCESS;
        }

        if (enabled == null) {
            // Toggle current state
            enabled = !socialPlayer.isNotificationsEnabled();
        }

        socialPlayer.setNotificationsEnabled(enabled);

        // Save to database
        plugin.getStorage().savePlayer(socialPlayer);

        String messageKey = enabled ? "notifications-enabled" : "notifications-disabled";
        player.sendMessage(MINI_MESSAGE.deserialize(plugin.getLanguageManager().getMessage(messageKey)));

        return Command.SINGLE_SUCCESS;
    }
}
