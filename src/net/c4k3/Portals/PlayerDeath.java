package net.c4k3.Portals;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		SQLite.delete_unset_portal(event.getEntity().getUniqueId());
	}
}
