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
			if (is_invalid_pressure_plate(block.getType())) {
				return;
			}

			if (!PortalCheck.is_valid_portal(block)) {
				return;
			}

			/* Short delay to stop players from teleporting to the wrong loation */
			Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
				public void run() {
					Portals.instance.getLogger().info(String.format("%s tried to use a portal by pressure plate at '%d %d %d %s'.",
								player.getName(), block.getX(), block.getY(), block.getZ(), block.getWorld().getName()));
					PortalUtils.teleport(player, block.getLocation());
				}
			}, 1L);
		}
	}

	/** Returns true if it's not a stone or wooden pressure plate */
	private boolean is_invalid_pressure_plate(Material block) {
		if (
		block == Material.ACACIA_PRESSURE_PLATE
		|| block == Material.BIRCH_PRESSURE_PLATE
		|| block == Material.DARK_OAK_PRESSURE_PLATE
		|| block == Material.JUNGLE_PRESSURE_PLATE
		|| block == Material.OAK_PRESSURE_PLATE
		|| block == Material.SPRUCE_PRESSURE_PLATE
		|| block == Material.STONE_PRESSURE_PLATE
				) {
			return false;
		} else {
			return true;
		}
	}
}
