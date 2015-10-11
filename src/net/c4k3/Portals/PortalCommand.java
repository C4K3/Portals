package net.c4k3.Portals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class PortalCommand implements CommandExecutor {

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
			PortalUtils.teleport(player);
			return true;
		}

		String arg = args[0].toLowerCase();

		if (arg.startsWith("buy")) {
			PortalUtils.outdated_command(player);
			return true;
		} else if (arg.startsWith("set")) {
			PortalUtils.outdated_command(player);
			return true;
		} else if (arg.startsWith("info")) {
			PortalUtils.show_info(player);
			return true;
		}

		player.sendMessage(ChatColor.RED + "Invalid arguments. Correct usage is /portal [info]");
		Portals.instance.getLogger().info(player.getName() + " entered invalid arguments.");
		return true;
	}
	
}
