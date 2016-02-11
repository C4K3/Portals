package net.c4k3.Portals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class Portals extends JavaPlugin {

	public static JavaPlugin instance;
	
	public static List<Entity> justTeleportedEntities = new ArrayList<Entity>();

	public void onEnable() {
		instance = this;

		/* Check if this plugin's directory exists, if not create it */
		File dir = new File("plugins/Portals");
		if ( !dir.exists() ) {
			dir.mkdir();
		}

		getServer().getPluginManager().registerEvents(new BlockBreak(), this);
		getServer().getPluginManager().registerEvents(new BlockPlace(), this);
		getServer().getPluginManager().registerEvents(new PlayerDeath(), this);
		getServer().getPluginManager().registerEvents(new PlayerToggleSneak(), this);
		
		/* Remove comment to enable minecart detection through portals */
		/* getServer().getPluginManager().registerEvents(new BlockRedstone(), this); */
		
		SQLite.connect();
	}

	public void onDisable() {
		SQLite.close();
	}


}

