package net.c4k3.Portals;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PortalUtils {
	
	/**
	 * For teleporting the given player
	 * @param player Player who used the command
	 */
	@SuppressWarnings("deprecation")
	public static void teleport(Player player) {
		Location location = player.getLocation();

		Location destination = SQLite.get_other_portal(location);

		/* If this portal is not a Portals portal */
		if (destination == null) {
			Portals.instance.getLogger().info(player.getName() + " destination was null.");
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
		
		player.teleport(destination);
	}
}
	
