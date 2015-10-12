package net.c4k3.Portals;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
			Portals.instance.getLogger().info(player.getName() + " destination was null, showing info.");
			show_info(player);
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
	
	/**
	 * Show info about portals to the player
	 * @param player
	 */
	public static void show_info(Player player) {
		String message = ChatColor.GREEN + "Type /portal to use a portal when "
				+ "standing inside one. If you are getting this message while "
				+ "trying to use a portal, then the portal you are trying to use has been destroyed.";

		String log_message = null;
		Block unset = SQLite.get_unset_portal(player.getUniqueId());
		if (unset != null) {
			message += "\nYou are currently placing a portal set. Take care not to die!";
			log_message = player.getName() + " is currently placing a portal set.";
		} else {
			log_message = player.getName() + " is not currently placing a portal set.";
		}

		player.sendMessage(message);
		Portals.instance.getLogger().info(log_message);
	}

	/**
	 * Tells the player that they're trying to use an oudated command.
	 * @param player The player entering the command.
	 */
	public static void outdated_command(Player player) {
		player.sendMessage(ChatColor.RED + "The command you are trying to use "
				+ "is no longer available. The portals plugin has been updated, "
				+ "please see " + ChatColor.AQUA + "http://simpvp.net/w/Portals" 
				+ ChatColor.RED + " for updated information on how to use portals.");
		Portals.instance.getLogger().info(player.getName() + " entered an outdated command.");
	}

}
