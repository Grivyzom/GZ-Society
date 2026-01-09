package gc.grivyzom.gZSociety.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import gc.grivyzom.gZSociety.Main;
import gc.grivyzom.gZSociety.objects.SocialPlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.UUID;

/**
 * Listens for player join/leave events and sends friend notifications.
 */
public class FriendNotificationListener {

    private final Main plugin;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public FriendNotificationListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a player connects to a server (after login).
     * We use ServerPostConnectEvent to ensure player data is loaded.
     */
    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        // Only notify on first join (when previous server is null)
        if (event.getPreviousServer() != null) {
            return;
        }

        Player joiningPlayer = event.getPlayer();
        UUID joiningUUID = joiningPlayer.getUniqueId();
        String joiningName = joiningPlayer.getUsername();

        // Notify all online players who have this player as a friend
        for (Player onlinePlayer : plugin.getServer().getAllPlayers()) {
            if (onlinePlayer.getUniqueId().equals(joiningUUID)) {
                continue; // Skip self
            }

            SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
            if (socialPlayer == null || !socialPlayer.isNotificationsEnabled()) {
                continue;
            }

            // Check if the joining player is a friend
            if (socialPlayer.getFriends().contains(joiningUUID)) {
                boolean isBestFriend = socialPlayer.isBestFriend(joiningUUID);

                if (isBestFriend) {
                    // Best friend: show title + message
                    sendBestFriendJoinNotification(onlinePlayer, joiningName);
                } else {
                    // Regular friend: just message
                    String message = plugin.getLanguageManager()
                            .getMessage("friend-joined", "{player}", joiningName);
                    onlinePlayer.sendMessage(MINI_MESSAGE.deserialize(message));
                }
            }
        }
    }

    /**
     * Called when a player disconnects from the proxy.
     */
    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        Player leavingPlayer = event.getPlayer();
        UUID leavingUUID = leavingPlayer.getUniqueId();
        String leavingName = leavingPlayer.getUsername();

        // Notify all online players who have this player as a friend
        for (Player onlinePlayer : plugin.getServer().getAllPlayers()) {
            if (onlinePlayer.getUniqueId().equals(leavingUUID)) {
                continue; // Skip self
            }

            SocialPlayer socialPlayer = plugin.getPlayerManager().getPlayer(onlinePlayer.getUniqueId());
            if (socialPlayer == null || !socialPlayer.isNotificationsEnabled()) {
                continue;
            }

            // Check if the leaving player is a friend
            if (socialPlayer.getFriends().contains(leavingUUID)) {
                String message = plugin.getLanguageManager()
                        .getMessage("friend-left", "{player}", leavingName);
                onlinePlayer.sendMessage(MINI_MESSAGE.deserialize(message));
            }
        }
    }

    /**
     * Sends a special notification for best friends joining.
     */
    private void sendBestFriendJoinNotification(Player player, String friendName) {
        // Send title
        String titleText = plugin.getLanguageManager()
                .getMessage("bestfriend-joined-title", "{player}", friendName);
        String subtitleText = plugin.getLanguageManager()
                .getMessage("bestfriend-joined-subtitle", "{player}", friendName);

        Title title = Title.title(
                MINI_MESSAGE.deserialize(titleText),
                MINI_MESSAGE.deserialize(subtitleText),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500)));
        player.showTitle(title);

        // Also send chat message
        String message = plugin.getLanguageManager()
                .getMessage("friend-joined", "{player}", friendName);
        player.sendMessage(MINI_MESSAGE.deserialize(message));

        // Play sound (Velocity doesn't have direct sound API, but you can send sound
        // via Adventure)
        // Note: Velocity proxies don't play sounds directly; this would need a plugin
        // message to backend
    }
}
