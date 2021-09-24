package net.simpvp.Portals;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlace implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Material material = event.getBlock().getType();

		if (Portals.FRAMES.contains(material)) {
			check_obsi_placement(event);
			return;
		}

		if (material != Material.DIAMOND_BLOCK && material != Material.LAPIS_BLOCK)
			return;

		String world = event.getBlock().getWorld().getName();
		if (!(world.equals("world") || world.equals("world_nether") || world.equals("world_the_end")))
			return;

		Block location = event.getBlock();
		if (!Portals.FRAMES.contains(location.getRelative(BlockFace.DOWN).getType()))
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
	 * Check for database consistency when obsidian is placed.
	 *
	 * We previously only checked for removal of portals on BlockBreakEvents.
	 * What some people figured out is that if you can remove the obsidian in
	 * a way that doesn't trigger a BlockBreakEvent, then the database entry
	 * will still be there but there won't be a usable portal.
	 *
	 * That way people could create portals that are unusable until somebody
	 * places obsidian back in just the right places.
	 *
	 * To stop people from doing this, this method checks for such dangling
	 * database entries whenever somebody places obsidian as well. This
	 * theoretically doesn't remove the problem 100%, if somebody can both
	 * break and then later re-place the obsidian without triggering either
	 * a BlockBreakEvent or a BlockPlaceEvent, then they can still trigger
	 * the old behavior. But that is considered to be so impossible, and
	 * practically impossible for the plugin to monitor for, that we let it be.
	 */
	private void check_obsi_placement(BlockPlaceEvent event) {
		Block block = event.getBlock();
		ArrayList<PortalLocation> portals = SQLite.obsidian_checker(block);

		for (PortalLocation portal : portals) {
			// Treat the placed block as the broken block for portal check
			// as we want to check if there was a valid portal _before_
			// the block was placed.
			if (PortalCheck.is_valid_portal(portal.block, block) == false) {
				Portals.instance.getLogger().info("check_obsi_placement found dangling database entry triggered by an obsidian placement by " + event.getPlayer().getName());
				SQLite.delete_portal_pair(portal);
			}
		}
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
			Portals.instance.getLogger().info(String.format("%s successfully created an unset portal at '%d %d %d %s'.",
						player.getName(), location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
			location.setType(Material.AIR);
			location.getRelative(BlockFace.UP).setType(Material.AIR);

		} else {
			SQLite.delete_unset_portal(uuid);

			if (PortalCheck.is_valid_portal(unset) == false) {
				Portals.instance.getLogger().info(String.format("%s's other portal was broken at '%d %d %d %s'.",
							player.getName(), location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
				return;
			}

			SQLite.insert_portal_pair(location, unset, player.getUniqueId());
			Portals.instance.getLogger().info(String.format("%s successfully created a portal set to '%d %d %d %s' by placing a portal at '%d %d %d %s'.",
						player.getName(),
						unset.getX(),
						unset.getY(),
						unset.getZ(),
						unset.getWorld().getName(),
						location.getX(),
						location.getY(),
						location.getZ(),
						location.getWorld().getName()
						));

			location.setType(Material.AIR);
			location.getRelative(BlockFace.UP).setType(Material.AIR);
		}
	}
}

