package net.c4k3.Portals;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerDeathEvent(PlayerDeathEvent event) {

		Player player = event.getEntity();
		String world = player.getLocation().getWorld().getName();

		if (!(world.equals("world") || world.equals("world_nether") || world.equals("world_the_end") || world.equals("pvp")))
			return;

		SQLite.delete_unset_portal(event.getEntity().getUniqueId());
	}
}
