package gc.grivyzom.gZSociety.storage;

import gc.grivyzom.gZSociety.objects.SocialPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An in-memory storage implementation for testing and basic use.
 * This implementation does NOT persist data across server restarts.
 */
public class InMemoryStorage implements Storage {

    private final ConcurrentMap<UUID, SocialPlayer> database = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<SocialPlayer> loadPlayer(UUID playerId, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            // Get the player if they exist, otherwise create a new one and store it.
            return database.computeIfAbsent(playerId, k -> new SocialPlayer(playerId, playerName));
        });
    }

    @Override
    public CompletableFuture<Void> savePlayer(SocialPlayer player) {
        return CompletableFuture.runAsync(() -> {
            // In a real database this would be an UPDATE or INSERT statement.
            // Here, we just ensure the latest version of the object is in the map.
            database.put(player.getPlayerId(), player);
        });
    }
}
