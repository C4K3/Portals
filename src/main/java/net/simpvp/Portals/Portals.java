package net.simpvp.Portals;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class Portals extends JavaPlugin {

	public static JavaPlugin instance;

	/* Materials that a portal frame can use interchangeably */
	public static final HashSet<Material> FRAMES = new HashSet<>(Arrays.asList(
			Material.OBSIDIAN,
			Material.CRYING_OBSIDIAN
	));

	public static HashSet<UUID> justTeleportedEntities = new HashSet<UUID>();

	public void onEnable() {
		instance = this;

		/* Check if this plugin's directory exists, if not create it */
		File dir = new File("plugins/Portals");
		if ( !dir.exists() ) {
			dir.mkdir();
		}

		getCommand("portallog").setExecutor(new PortalLogCommand());
		
		getServer().getPluginManager().registerEvents(new BlockBreak(), this);
		getServer().getPluginManager().registerEvents(new BlockPlace(), this);
		getServer().getPluginManager().registerEvents(new PlayerDeath(), this);
		getServer().getPluginManager().registerEvents(new PlayerToggleSneak(), this);
		getServer().getPluginManager().registerEvents(new PlayerInteract(), this);

		SQLite.connect();
		PortalUtils.loadTeleportable();
	}

	public void onDisable() {
		SQLite.close();
	}


}

