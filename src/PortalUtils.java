package net.simpvp.Portals;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PortalUtils {

	/**
	 * For teleporting the given entity using a specific portal
	 * @param entity entity who is trying to teleport
	 * @param portal block to check for portal location at
	 */
	@SuppressWarnings("deprecation")
	public static void teleport(Player player) {

		/* Check if the player is trying to teleport again too fast */
		if (Portals.justTeleportedEntities.contains(player.getUniqueId())) {
			return;
		}

		Location location = player.getLocation();

		Location destination = SQLite.get_other_portal(location);

		/* If this portal is not a Portals portal */
		if (destination == null) {
			Portals.instance.getLogger().info(player.getName() + " destination was null.");
			return;
		}

		/* Make sure a valid portal is at destination */
		if (!PortalCheck.is_valid_portal(destination.getBlock())) {
			Portals.instance.getLogger().info(player.getName() + " destination portal frame is missing.");
			return;
		}

		Portals.instance.getLogger().info("Teleporting "
				+ player.getName() + " to " + destination.getWorld().getName()
				+ " " + destination.getBlockX() + " " + destination.getBlockY() 
				+ " " + destination.getBlockZ());

		/* workaround for laggy teleports, see
		 * https://bukkit.org/threads/workaround-for-playing-falling-after-teleport-when-lagging.293035/ */
		Location fLoc = new Location(destination.getWorld(),
				destination.getBlockX(), destination.getBlockY() - 1,
				destination.getBlockZ());
		player.sendBlockChange(fLoc, fLoc.getBlock().getType(), fLoc.getBlock().getData());

		teleportNearby(location, destination);

		player.teleport(destination);

		/* Fix players from being stuck sneaking after a teleport*/
		player.setSneaking(false);

		setTeleported(player);
	}

	/**
	 * For limiting portal attempts
	 * Allows two way minecart travel and maybe fixes the instant death bug
	 * @param entity entity to stop from teleporting again too soon
	 */
	public static void setTeleported(Entity entity) {
		final UUID uuid = entity.getUniqueId();
		Portals.justTeleportedEntities.add(uuid);

		Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
			public void run() {
				Portals.justTeleportedEntities.remove(uuid);
			}
		}, 2 * 20L);
	}

	/**
	 * Teleports nearby non-player entities through a nearby portal
	 * @param location location to check for a portal at
	 */
	public static void teleportNearby(Location from, Location destination) {

		Collection<Entity> nearby = from.getWorld().getNearbyEntities(from, 2, 2, 2);

		if (nearby.isEmpty())
			return;

		for (Entity entity : nearby) {
			if (entity instanceof Player)
				continue;

			if (Portals.justTeleportedEntities.contains(entity.getUniqueId()))
				continue;

			entity.teleport(destination);
			setTeleported(entity);
		}

	}

}

