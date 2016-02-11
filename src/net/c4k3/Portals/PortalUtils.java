package net.c4k3.Portals;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PortalUtils {
	
	/**
	 * For teleporting the given entity using a portal found at it's location
	 * @param entity entity who is trying to teleport
	 */
	public static void teleport(Entity entity) {
		teleport(entity, null);
	}
	
	/**
	 * For teleporting the given entity using a specific portal
	 * @param entity entity who is trying to teleport
	 * @param portal block to check for portal location at
	 */
	@SuppressWarnings("deprecation")
	public static void teleport(Entity entity, Block portal) {
		
		/* Check if the entity is trying to teleport again too fast */
		if (Portals.justTeleportedEntities.contains(entity)) {
			return;
		}
		
		Location location = (portal == null) ? entity.getLocation() : portal.getLocation();

		Location destination = SQLite.get_other_portal(location);

		/* If this portal is not a Portals portal */
		if (destination == null) {
			Portals.instance.getLogger().info(entity.getName() + " destination was null.");
			return;
		}
		
		/* Make sure a valid portal is at destination */
		if (!PortalCheck.is_valid_portal(destination.getBlock())) {
			Portals.instance.getLogger().info(entity.getName() + " destination portal frame is missing.");
			return;
		}
		
		
		if (entity instanceof Player) {
			
			Portals.instance.getLogger().info("Teleporting "
					+ entity.getName() + " to " + destination.getWorld().getName()
					+ " " + destination.getBlockX() + " " + destination.getBlockY() 
					+ " " + destination.getBlockZ());
			
			/* workaround for laggy teleports, see
			 * https://bukkit.org/threads/workaround-for-playing-falling-after-teleport-when-lagging.293035/ */
			
			Location fLoc = new Location(destination.getWorld(),
					destination.getBlockX(), destination.getBlockY() - 1,
					destination.getBlockZ());
			((Player) entity).sendBlockChange(fLoc, fLoc.getBlock().getType(), fLoc.getBlock().getData());
			
			teleportNearby(location);
		}
		
		entity.teleport(destination);
		
		/* Fix players from being stuck sneaking after a teleport*/
		if (entity instanceof Player) {
			((Player) entity).setSneaking(false);
		}
		
		setTeleported(entity);
	}
	
	/**
	 * For limiting portal attempts
	 * Allows two way minecart travel and maybe fixes the instant death bug
	 * @param entity entity to stop from teleporting again too soon
	 */
	
	public static void setTeleported(final Entity entity) {
		Portals.justTeleportedEntities.add(entity);
		
		Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
			public void run() {
				Portals.justTeleportedEntities.remove(entity);
			}
		}, 5L);
	}
	
	/**
	 * Teleports nearby non-player entities through a nearby portal
	 * @param location location to check for a portal at
	 */
	public static void teleportNearby(Location location) {
		
		Collection<Entity> nearby = location.getWorld().getNearbyEntities(location, 2, 2, 2);
		
		if (nearby.isEmpty())
			return;
		
		for (Entity entity : nearby) {
			
			if (entity instanceof Player)
				continue;
			
			teleport(entity, location.getBlock());	
		}
	}
	
}
	
