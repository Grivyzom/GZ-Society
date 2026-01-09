package gc.grivyzom.gZSociety.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import gc.grivyzom.gZSociety.manager.PlayerManager;

/**
 * Listener for player connection events (join, leave).
 */
public class PlayerConnectionListener {

    private final PlayerManager playerManager;

    public PlayerConnectionListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        playerManager.handlePlayerJoin(event.getPlayer());
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        playerManager.handlePlayerLeave(event.getPlayer());
    }
}
