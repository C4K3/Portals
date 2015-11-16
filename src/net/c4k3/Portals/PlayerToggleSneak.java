package net.c4k3.Portals;

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
		if ( event.isSneaking() && PortalCheck.is_valid_portal(block) ) {
			
			Portals.instance.getLogger().info(player.getName() + " tried to use a portal by sneaking at"
					+ " " + block.getWorld().getName()
					+ " " + block.getX()
					+ " " + block.getY()
					+ " " + block.getZ()
					+ ".");
			PortalUtils.teleport(player);
			
		}
	}
	
}

