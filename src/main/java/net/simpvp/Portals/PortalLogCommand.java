package net.simpvp.Portals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class PortalLogCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to use this command.");
			return false;
		}

		Player player = (Player) sender;

		if (!player.isOp()) {
			player.sendMessage(ChatColor.RED + "You must be an admin to use this command!");
			return false;
		}

		if (args.length != 1) {
			player.sendMessage(ChatColor.RED + "Correct usage: /portallog <playtime>");
			return false;
		}

		int playtime = 0;

		try {
			playtime = Integer.parseInt(args[0]);
		} catch(Exception e) {
			player.sendMessage(ChatColor.RED + "Invalid integer was given");
			return false;
		}

		if (playtime > 0) {
			SQLite.set_portallog(player.getUniqueId().toString(), playtime);
			player.sendMessage(ChatColor.GREEN + "Portal logging turned on for players with " + playtime + " hours or less");
			Portals.instance.getLogger().info(player.getName() + " turned portal logging on for players with " + playtime + " hours or less");
		} else {
			SQLite.set_portallog(player.getUniqueId().toString(), playtime);
			player.sendMessage(ChatColor.RED + "Portal logging turned off");
			Portals.instance.getLogger().info(player.getName() + " turned portal logging off");
		}
		return true;
	}
}
