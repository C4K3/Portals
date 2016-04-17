package net.simpvp.Portals;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkUnload implements Listener {

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		
		/* Do not let chunks containing entities about to be teleported unload */
		for (Entity entity : event.getChunk().getEntities()) {
			if (Portals.justTeleportedEntities.contains(entity.getUniqueId())) {
				event.setCancelled(true);
			}	
		}
		
	}
}
