package gc.grivyzom.gZSociety.manager;

import com.velocitypowered.api.proxy.Player;
import gc.grivyzom.gZSociety.objects.SocialPlayer;
import gc.grivyzom.gZSociety.storage.Storage;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the loading, caching, and unloading of SocialPlayer objects.
 */
public class PlayerManager {

    private final Storage storage;
    private final ConcurrentMap<UUID, SocialPlayer> loadedPlayers = new ConcurrentHashMap<>();

    public PlayerManager(Storage storage) {
        this.storage = storage;
    }

    /**
     * Handles the logic when a player joins the server.
     * It loads the player's data into the cache.
     *
     * @param player The player who joined.
     */
    public void handlePlayerJoin(Player player) {
        storage.loadPlayer(player.getUniqueId(), player.getUsername()).thenAccept(socialPlayer -> {
            loadedPlayers.put(player.getUniqueId(), socialPlayer);
        });
    }

    /**
     * Handles the logic when a player leaves the server.
     * It saves the player's data and removes them from the cache.
     *
     * @param player The player who left.
     */
    public void handlePlayerLeave(Player player) {
        SocialPlayer socialPlayer = loadedPlayers.remove(player.getUniqueId());
        if (socialPlayer != null) {
            storage.savePlayer(socialPlayer);
        }
    }

    /**
     * Gets a loaded SocialPlayer from the cache.
     *
     * @param playerId The UUID of the player.
     * @return The SocialPlayer object, or null if not loaded.
     */
    public SocialPlayer getPlayer(UUID playerId) {
        return loadedPlayers.get(playerId);
    }

    public ConcurrentMap<UUID, SocialPlayer> getLoadedPlayers() {
        return loadedPlayers;
    }
}
