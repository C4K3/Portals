package net.c4k3.Portals;

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
	

	@EventHandler
	public void onBlockRedstone(BlockRedstoneEvent event) {
		
		Block block = event.getBlock();
		
		/* Check for detector rail that just turned on */
		if (block.getType() == Material.DETECTOR_RAIL && event.getNewCurrent() == 15) {
			
			if (PortalCheck.is_valid_portal(block)) {

				Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1);
				
				for (Entity entity : entities) {
					
					if (entity.getType() != EntityType.MINECART) {
						continue;
					}
					
					if (Portals.justTeleportedEntities.contains(entity)) {
						continue;
					}
					
					if (entity.getPassenger() != null) {
						
						/* Fix for being unable to teleport an entity riding another */
						Entity passenger = entity.getPassenger();
						passenger.leaveVehicle();
						
						PortalUtils.teleport(entity, block);
						
						if (passenger instanceof Player) {
							Portals.instance.getLogger().info("Teleporting 2"
									+ passenger.getName() + " to " + entity.getLocation().getWorld().getName()
									+ " " + entity.getLocation().getBlockX() + " " + entity.getLocation().getBlockY() 
									+ " " + entity.getLocation().getBlockZ());
						}
						
						entity.setPassenger(passenger);
					}	
				}
			}	
		}
	}

}
