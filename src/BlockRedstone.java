package net.simpvp.Portals;

import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class BlockRedstone implements Listener {


	/* FIXME: Needs to be re-written to work with latest PortalUtils update */
	@EventHandler
	public void onBlockRedstone(BlockRedstoneEvent event) {

		Block block = event.getBlock();

		/* Check for detector rail that just turned on */
		if (block.getType() != Material.DETECTOR_RAIL || event.getNewCurrent() == 15) {
			return;
		}

		if (!PortalCheck.is_valid_portal(block)) {
			return;
		}

		Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1);

		for (Entity entity : entities) {

			if (entity.getType() != EntityType.MINECART) {
				continue;
			}

			if (Portals.justTeleportedEntities.contains(entity.getUniqueId())) {
				continue;
			}

			/* Fix for being unable to teleport an entity riding another */
			if (entity.getPassenger() == null) {
				return;
			}

			Entity passenger = entity.getPassenger();
			passenger.leaveVehicle();

			//PortalUtils.teleport(entity, block);

			if (passenger instanceof Player) {
				Portals.instance.getLogger().info("Teleporting "
						+ passenger.getName() + " to " + entity.getLocation().getWorld().getName()
						+ " " + entity.getLocation().getBlockX() + " " + entity.getLocation().getBlockY() 
						+ " " + entity.getLocation().getBlockZ());
			}

			entity.setPassenger(passenger);
		}	
	}
}

