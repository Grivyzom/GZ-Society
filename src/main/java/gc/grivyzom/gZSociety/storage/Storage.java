package gc.grivyzom.gZSociety.storage;

import gc.grivyzom.gZSociety.objects.SocialPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for data storage operations.
 * All operations are asynchronous and return a CompletableFuture.
 */
public interface Storage {

    /**
     * Loads a player's social data from the storage.
     * If the player does not exist, it should create a new default record.
     *
     * @param playerId The UUID of the player to load.
     * @param playerName The current name of the player.
     * @return A CompletableFuture that will complete with the SocialPlayer object.
     */
    CompletableFuture<SocialPlayer> loadPlayer(UUID playerId, String playerName);

    /**
     * Saves a player's social data to the storage.
     *
     * @param player The SocialPlayer object to save.
     * @return A CompletableFuture that will complete when the save operation is finished.
     */
    CompletableFuture<Void> savePlayer(SocialPlayer player);

}
