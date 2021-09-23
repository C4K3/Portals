package net.simpvp.Portals;

import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Checks all broken obsidian blocks, checking that they were not part of
 * a portal that now will have to be deactivated. If they were, deactivate
 * said portal(s).
 */
public class BlockBreak implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!Portals.FRAMES.contains(event.getBlock().getType()))
			return;

		Block block = event.getBlock();
		ArrayList<PortalLocation> portals = SQLite.obsidian_checker(block);

		for (PortalLocation portal : portals) {
			if (PortalCheck.is_valid_portal(portal.block, block) == false) {
				Portals.instance.getLogger().info("Query returned positive by " + event.getPlayer().getName());
				SQLite.delete_portal_pair(portal);
			}
		}
	}
}

