package net.simpvp.Portals;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteract implements Listener {

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		
		if (event.getAction() == Action.PHYSICAL) {
			
			Player player = event.getPlayer();	
			Block block = event.getClickedBlock();
			
			/* Other pressure plates seem to send multiple events when stepped on so only use wood and stone */
			if (block.getType() != Material.STONE_PLATE && block.getType() != Material.WOOD_PLATE) {
				return;
			}
			
			if (PortalCheck.is_valid_portal(block)) {
				Portals.instance.getLogger().info(player.getName() + " tried to use a portal by pressure plate at"
						+ " " + block.getWorld().getName()
						+ " " + block.getX()
						+ " " + block.getY()
						+ " " + block.getZ()
						+ ".");
				PortalUtils.teleport(player, block.getLocation());
			}
		}
	}

}
