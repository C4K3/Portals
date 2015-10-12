package net.c4k3.Portals;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class Portals extends JavaPlugin {

	public static JavaPlugin instance;

	public void onEnable() {
		instance = this;

		/* Check if this plugin's directory exists, if not create it */
		File dir = new File("plugins/Portals");
		if ( !dir.exists() ) {
			dir.mkdir();
		}

		getCommand("portal").setExecutor(new PortalCommand());
		getServer().getPluginManager().registerEvents(new BlockBreak(), this);
		getServer().getPluginManager().registerEvents(new BlockPlace(), this);
		getServer().getPluginManager().registerEvents(new PlayerDeath(), this);
		getServer().getPluginManager().registerEvents(new PlayerToggleSneak(), this);


		SQLite.connect();
	}

	public void onDisable() {
		SQLite.close();
	}


}
