package gc.grivyzom.gZSociety.objects;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player's social data, including friends, blocked players, etc.
 * This object is designed to be thread-safe.
 */
public class SocialPlayer {

    private final UUID playerId;
    private final String playerName;

    // Using ConcurrentHashMap for thread-safe collections
    private final Set<UUID> friends = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> bestFriends = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> ignored = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> blocked = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Friend request system
    private final Set<UUID> outgoingRequests = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> incomingRequests = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Settings
    private boolean notificationsEnabled = true;

    public SocialPlayer(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    // --- Getters ---

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Set<UUID> getFriends() {
        return Collections.unmodifiableSet(friends);
    }

    public Set<UUID> getBestFriends() {
        return Collections.unmodifiableSet(bestFriends);
    }

    public Set<UUID> getIgnored() {
        return Collections.unmodifiableSet(ignored);
    }

    public Set<UUID> getBlocked() {
        return Collections.unmodifiableSet(blocked);
    }

    public Set<UUID> getOutgoingRequests() {
        return Collections.unmodifiableSet(outgoingRequests);
    }

    public Set<UUID> getIncomingRequests() {
        return Collections.unmodifiableSet(incomingRequests);
    }

    // --- Settings ---

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    // --- Friend Request Methods ---

    /**
     * Sends a friend request to another player.
     * This should be paired with the target's receiveRequest().
     */
    public void sendRequest(UUID targetId) {
        this.outgoingRequests.add(targetId);
    }

    /**
     * Cancels an outgoing friend request.
     */
    public void cancelRequest(UUID targetId) {
        this.outgoingRequests.remove(targetId);
    }

    /**
     * Receives a friend request from another player.
     * This should be paired with the sender's sendRequest().
     */
    public void receiveRequest(UUID senderId) {
        this.incomingRequests.add(senderId);
    }

    /**
     * Accepts a friend request - both players become friends.
     * This removes the request from incoming and adds to friends.
     */
    public boolean acceptRequest(UUID senderId) {
        if (this.incomingRequests.remove(senderId)) {
            this.friends.add(senderId);
            return true;
        }
        return false;
    }

    /**
     * Denies a friend request.
     */
    public boolean denyRequest(UUID senderId) {
        return this.incomingRequests.remove(senderId);
    }

    /**
     * Checks if there's a pending request from a specific player.
     */
    public boolean hasPendingRequestFrom(UUID senderId) {
        return this.incomingRequests.contains(senderId);
    }

    /**
     * Checks if there's an outgoing request to a specific player.
     */
    public boolean hasSentRequestTo(UUID targetId) {
        return this.outgoingRequests.contains(targetId);
    }

    // --- Friend Mutators ---

    public void addFriend(UUID friendId) {
        this.friends.add(friendId);
    }

    public void removeFriend(UUID friendId) {
        this.friends.remove(friendId);
        // A player can't be a best friend if they are not a friend
        this.bestFriends.remove(friendId);
    }

    public boolean addBestFriend(UUID friendId) {
        if (this.friends.contains(friendId)) {
            this.bestFriends.add(friendId);
            return true;
        }
        return false;
    }

    public void removeBestFriend(UUID friendId) {
        this.bestFriends.remove(friendId);
    }

    public boolean isBestFriend(UUID friendId) {
        return this.bestFriends.contains(friendId);
    }

    // --- Ignore/Block Mutators ---

    public void ignorePlayer(UUID targetId) {
        this.ignored.add(targetId);
    }

    public void unignorePlayer(UUID targetId) {
        this.ignored.remove(targetId);
    }

    public void blockPlayer(UUID targetId) {
        this.blocked.add(targetId);
        // Blocking a player should probably remove them from friends lists
        removeFriend(targetId);
        // Also remove any pending requests
        this.incomingRequests.remove(targetId);
        this.outgoingRequests.remove(targetId);
    }

    public void unblockPlayer(UUID targetId) {
        this.blocked.remove(targetId);
    }

    public boolean hasBlocked(UUID targetId) {
        return this.blocked.contains(targetId);
    }
}
