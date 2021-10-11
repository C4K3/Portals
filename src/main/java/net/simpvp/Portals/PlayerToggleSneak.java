package net.simpvp.Portals;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerToggleSneak implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {

		Player player = event.getPlayer();
		Block block = player.getLocation().getBlock();

		/* Check if player starts sneaking on top of obsidian */
		if (event.isSneaking() && PortalCheck.is_valid_portal(block)) {
			PortalUtils.teleport(player);
		}
	}
}

