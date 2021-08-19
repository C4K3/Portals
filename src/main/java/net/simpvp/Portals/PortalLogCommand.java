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
			return false;
		}
		
		Player player = (Player) sender;
		
		if (!player.isOp()) {
			player.sendMessage(ChatColor.RED + "You must be an admin to use this command!");
			return false;
		}
		
		if (args.length != 1) {
			player.sendMessage(ChatColor.RED + "Correct usage: /portallog <true/false>");
			return true;
		}
		
		Portals.instance.getLogger().info(args[0]);
		
		if (args[0].toLowerCase().equals("true")) {
			SQLite.set_portallog_boolean(1, player.getUniqueId().toString());
			player.sendMessage(ChatColor.GREEN + "First time portal logging on!");
		} else {
			SQLite.set_portallog_boolean(0, player.getUniqueId().toString());
			player.sendMessage(ChatColor.RED + "First time portal logging off!");
		}
		
		
		
		return false;

	}
}
