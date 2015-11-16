package net.c4k3.Portals;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
//FIXME: Do it so that you can't place a portal where there already is a portal, also checkingu nplaced portals
public class BlockPlace implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Material material = event.getBlock().getType();
		if (material != Material.DIAMOND_BLOCK && material != Material.LAPIS_BLOCK)
			return;

		String world = event.getBlock().getWorld().getName();
		if (!(world.equals("world") || world.equals("world_nether") || world.equals("world_the_end")))
			return;

		Block location = event.getBlock();
		if (location.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN)
			location = event.getBlock().getRelative(BlockFace.DOWN);

		if ((location.getType() != Material.DIAMOND_BLOCK 
				|| location.getRelative(BlockFace.UP).getType() != Material.LAPIS_BLOCK)
				&& (location.getType() != Material.LAPIS_BLOCK 
				|| location.getRelative(BlockFace.UP).getType() != Material.DIAMOND_BLOCK))
			return;

		if (PortalCheck.is_valid_portal(location) == false)
			return;

		if (SQLite.get_portal_by_location(location) > 0)
			return;

		if (SQLite.get_unset_by_location(location) > 0)
			return;

		/* all checks finished, setting portal */
		set_portal(location, event.getPlayer());
	}

	/**
	 * Responsible for actually setting the new portal,
	 * after it has been determined that it can be placed.
	 * @param location Location where the portal is being set
	 * @param player Player who is setting the portal
	 */
	private void set_portal(Block location, Player player) {
		UUID uuid = player.getUniqueId();
		Block unset = SQLite.get_unset_portal(uuid);
		if (unset == null) {
			SQLite.insert_unset_portal(uuid, location);
			Portals.instance.getLogger().info(player.getName() + " successfully created an unset portal at"
					+ " " + location.getWorld().getName()
					+ " " + location.getX()
					+ " " + location.getY()
					+ " " + location.getZ()
					+ ".");
			location.setType(Material.AIR);
			location.getRelative(BlockFace.UP).setType(Material.AIR);

		} else {
			SQLite.delete_unset_portal(uuid);

			if (PortalCheck.is_valid_portal(unset) == false) {
				Portals.instance.getLogger().info(player.getName() + "'s other portal was broken at"
						+ " " + location.getWorld().getName()
						+ " " + location.getX()
						+ " " + location.getY()
						+ " " + location.getZ()
						+ ".");
				return;
			}

			SQLite.insert_portal_pair(location, unset);
			Portals.instance.getLogger().info(player.getName() + " successfully created a portal set to"
					+ " " + unset.getWorld().getName()
					+ " " + unset.getX()
					+ " " + unset.getY()
					+ " " + unset.getZ()
					+ " by placing a portal at"
					+ " " + location.getWorld().getName()
					+ " " + location.getX()
					+ " " + location.getY()
					+ " " + location.getZ()
					+ ".");

			location.setType(Material.AIR);
			location.getRelative(BlockFace.UP).setType(Material.AIR);	
		}
	}
}

