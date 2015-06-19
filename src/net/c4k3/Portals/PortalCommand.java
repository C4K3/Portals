package net.c4k3.Portals;

import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;

public class PortalCommand implements CommandExecutor {

	private BlockFace[] path = {BlockFace.DOWN, BlockFace.EAST, BlockFace.UP,
			BlockFace.UP, BlockFace.UP, BlockFace.WEST, BlockFace.WEST,
			BlockFace.DOWN, BlockFace.DOWN, BlockFace.DOWN};

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}

		if (player == null) {
			Portals.instance.getLogger().info("You must be a player to use this command.");
			return true;
		}

		if (args.length > 1) {
			player.sendMessage(ChatColor.RED + "Invalid arguments. Correct usage is /portal [buy/set]");
			Portals.instance.getLogger().info(player.getName() + " entered invalid arguments.");
			return true;
		}

		if (args.length == 0) {
			teleport(player);
			return true;
		}

		String arg = args[0].toLowerCase();

		if (arg.startsWith("buy")) {
			purchase(player);
			return true;
		} else if (arg.startsWith("set")) {
			set(player);
			return true;
		} else if (arg.startsWith("info")) {
			show_info(player);
			return true;
		}

		player.sendMessage(ChatColor.RED + "Invalid arguments. Correct usage is /portal [buy/set]");
		Portals.instance.getLogger().info(player.getName() + " entered invalid arguments.");
		return true;
	}

	/**
	 * For teleporting the given player
	 * @param player Player who used the command
	 */
	@SuppressWarnings("deprecation")
	private void teleport(Player player) {
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
		Location fLoc = new Location(destination.getWorld(), destination.getBlockX(), destination.getBlockY() - 1, destination.getBlockZ());
		player.sendBlockChange(fLoc, fLoc.getBlock().getType(), fLoc.getBlock().getData());

		player.teleport(destination);
	}

	/**
	 * Show info about portals to the player
	 * @param player
	 */
	private void show_info(Player player) {
		String message = ChatColor.GREEN + "Type " + ChatColor.AQUA + "/portal buy"
				+ ChatColor.GREEN + " to purchase a portal set."
				+ "\n Then type " + ChatColor.AQUA + "/portal set"
				+ ChatColor.GREEN + " to set the portals.";

		UUID uuid = player.getUniqueId();
		int portal_count = SQLite.get_purchased_portals(uuid);
		message += "\nYou have purchased " + ChatColor.AQUA + portal_count
				+ ChatColor.GREEN + " portals.";

		String log_message = "Showing info to " + player.getName() + ": " + portal_count + " portals available.";

		Location unset = SQLite.get_unset_portal(uuid);
		if (unset != null) {
			message += "\nYou are currently placing a portal set. Take care not to die!";
			log_message += "Player is currently placing a portal set.";
		} else {
			log_message += "Player is not currently placing a portal set.";
		}

		player.sendMessage(message);
		Portals.instance.getLogger().info(log_message);
	}

	/**
	 * To be called when a player tries to set a portal,
	 * whether it's a new portal pair or the second portal.
	 * @param player Player setting the portal.
	 */
	private void set(Player player) {
		UUID uuid = player.getUniqueId();

		Location location = player.getLocation();
		String world = location.getWorld().getName();
		if (!(world.equals("world") || world.equals("world_nether") || world.equals("world_the_end"))) {
			player.sendMessage(ChatColor.RED + "You cannot place portals in this world.");
			Portals.instance.getLogger().info(player.getName() + " is unable to place portals in this world.");
			return;
		}

		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();

		if ((Math.abs(x) < 40 && Math.abs(z) < 40 && world.equals("world") && (!player.isOp())) 
				|| y >= 250 || y <= 5) {
			player.sendMessage(ChatColor.RED + "You cannot place place portals at this location.");
			Portals.instance.getLogger().info(player.getName() + " is unable to place portals at this location.");
			return;
		}

		Block block = location.getBlock();
		for (int i = 0; i < path.length; i++) {
			block = block.getRelative(path[i]);
			if (block.getType() != Material.OBSIDIAN) {
				player.sendMessage(ChatColor.RED + "A portal is not yet ready here. "
						+ "Please build a 4 blocks high 3 blocks wide east-west portal, "
						+ "making the inner size 2x1, barely enough for a single player.");
				Portals.instance.getLogger().info(player.getName() + " did not have a ready portal.");
				return;
			}
		}

		Location unset = SQLite.get_unset_portal(uuid);

		if (unset == null) {
			set_no_unset(player);
		} else {
			set_has_unset(player, unset);
		}
	}

	/**
	 * For when the player is using /portal set and does not have an unset portal
	 * @param player Player using the command
	 */
	private void set_no_unset(Player player) {
		UUID uuid = player.getUniqueId();
		Location location = player.getLocation();

		int amount = SQLite.get_purchased_portals(uuid);

		if (amount < 1) {
			player.sendMessage(ChatColor.RED + "You do not have any portals."
					+ " Purchase one with /portal buy, the price is 1 diamond block and 5 lapis blocks.");
			Portals.instance.getLogger().info(player.getName() + " did not have any purchased portals.");
			return;
		}

		SQLite.decrement_purchased_portals(uuid);
		SQLite.insert_unset_portal(uuid, location);
		player.sendMessage(ChatColor.GREEN + "You have successfully set one end of the portal here."
				+ " Go to where you want the other end to be at, and do /portal set again."
				+ " Be sure not to die or place any blocks here before you set the other end,"
				+ " because in that case this location will be forgotten,"
				+ " and you'll have to do everything all over again.");
		Portals.instance.getLogger().info(player.getName() + " successfully created an unset portal.");
	}

	/**
	 * For when the player is using /portal set and already has an unset portal
	 * @param player Player using the command
	 * @param unset Location of the already set unset portal
	 */
	private void set_has_unset(Player player, Location unset) {
		Location location = player.getLocation();

		UUID uuid = player.getUniqueId();
		SQLite.delete_unset_portal(uuid);

		Block block = unset.getBlock();
		for (int i = 0; i < path.length; i++) {
			block = block.getRelative(path[i]);
			if (block.getType() != Material.OBSIDIAN) {
				player.sendMessage(ChatColor.RED + "Someone has broken the other end of the portal. "
						+ "This portal has been lost.");
				Portals.instance.getLogger().info(player.getName() + "'s other portal was broken.");
				return;
			}
		}

		SQLite.insert_portal_pair(unset, location);

		player.sendMessage(ChatColor.GREEN + "Your portals have successfully been set up.");
		Portals.instance.getLogger().info(player.getName() + " successfully created a portal set.");
	}

	/**
	 * To be called when a player purchases a portal-pair
	 * @param player Player who is purchasing the portals
	 */
	private void purchase(Player player) {

		ItemStack diamond = new ItemStack(Material.DIAMOND_BLOCK);
		ItemStack lapis = new ItemStack(Material.LAPIS_BLOCK);

		/* First check that the player has the required items */
		if (player.getInventory().containsAtLeast(diamond, 1) == false) {
			player.sendMessage(ChatColor.RED + "You do not have enough "
					+ "diamond blocks to purchase a portal. It costs 1 "
					+ "diamond block and 5 lapis lazuli blocks to purchase a portal.");
			Portals.instance.getLogger().info(player.getName() + " did not have enough diamond blocks.");
			return;
		}

		if (player.getInventory().containsAtLeast(lapis, 5) == false) {
			player.sendMessage(ChatColor.RED + "You do not have enough lapis "
					+ "lazuli blocks to purchase a portal. It costs 1 diamond "
					+ "block and 5 lapis lazuli blocks to purchase a portal.");
			Portals.instance.getLogger().info(player.getName() + " did not have enough lapis blocks.");
			return;
		}

		/* Now remove the correct amount of items */
		int diamonds_left = 1;
		int lapis_left = 5;

		for (int i = 0; i < player.getInventory().getSize(); i++) {
			ItemStack item = player.getInventory().getItem(i);

			if (diamonds_left == 0 && lapis_left == 0)
				break;

			if (item == null)
				continue;

			if (item.getType() != Material.DIAMOND_BLOCK && item.getType() != Material.LAPIS_BLOCK)
				continue;

			if (item.getType() == Material.DIAMOND_BLOCK) {
				if (diamonds_left == 0)
					continue;

				if (item.getAmount() == 1) {
					player.getInventory().clear(i);
				} else {
					item.setAmount(item.getAmount() - 1);
				}
				diamonds_left = 0;
			} else {
				if (lapis_left == 0)
					continue;

				if (item.getAmount() <= lapis_left) {
					lapis_left -= item.getAmount();
					player.getInventory().clear(i);
				} else {
					item.setAmount(item.getAmount() - lapis_left);
					lapis_left = 0;
				}

			}

		}

		SQLite.increment_purchased_portals(player.getUniqueId());
		player.sendMessage(ChatColor.GREEN + "Congratulations, you have purchased a portal.");
		Portals.instance.getLogger().info(player.getName() + " purchased 1 portal.");
	}

}
