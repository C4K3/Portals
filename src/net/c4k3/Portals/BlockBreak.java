package net.c4k3.Portals;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreak implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerDeath(BlockBreakEvent event) {
		if (event.getBlock().getType() != Material.OBSIDIAN)
			return;

		Block block = event.getBlock();
		int id = SQLite.obsidian_checker(block);

		if (id <= 0)
			return;

		Portals.instance.getLogger().info("Query returned positive. "
				+ "Disabling this portal at "
				+ block.getWorld().getName() + " " + block.getX() 
				+ " " + block.getY() + " " + block.getZ());

		SQLite.delete_portal_pair(id);
	}

}
