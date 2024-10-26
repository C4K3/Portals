package net.simpvp.Portals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.util.BoundingBox;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class PortalUtils {

	/**
	 * For teleporting the given player using a specific portal
	 * @param player player who is trying to teleport
	 * @param portal block to check for portal location at
	 */
	public static void teleport(Player player, Location portal) {

		/* Check if the player is trying to teleport again too fast */
		if (Portals.justTeleportedEntities.contains(player.getUniqueId())) {
			return;
		}

		Location location = player.getLocation();
		SQLite.PortalLookup lookup = SQLite.get_other_portal(portal);

		/* If this portal is not a Portals portal */
		if (lookup == null) {
			Portals.instance.getLogger().info(String.format("%s destination was null at %d %d %d %s", player.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName()));
			return;
		}

		/* Stop Yaw and Pitch from changing if portal location is not directly from player */
		lookup.destination.setYaw(location.getYaw());
		lookup.destination.setPitch(location.getPitch());

		/* Make sure a valid portal is at destination */
		if (!PortalCheck.is_valid_portal(lookup.destination.getBlock())) {
			Portals.instance.getLogger().info(player.getName() + " destination portal frame is missing.");

			SQLite.delete_portal_pair(lookup);

			return;
		}

		ArrayList<UUID> portal_user_list = SQLite.get_portal_users(lookup.a);

		// Check if this is a players first time using this portal
		if (!portal_user_list.contains(player.getUniqueId()) && player.getGameMode() == GameMode.SURVIVAL) {
			SQLite.add_portal_user(lookup.a, lookup.b, player.getUniqueId());

			int played_ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
			int played_minutes = played_ticks / (20 * 60);
			double played_hours = played_minutes / 60.0;

			int x = player.getLocation().getBlockX();
			int y = player.getLocation().getBlockY();
			int z = player.getLocation().getBlockZ();
			String world = player.getLocation().getWorld().getName();

			Portals.instance.getLogger().info(String.format("%s just used a portal for the first time at '%d %d %d %s'", player.getName(), x, y, z, world));

			TextComponent msg = new TextComponent(player.getName() + " just used a portal for the first time");
			msg.setColor(ChatColor.RED);
			ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tpc %d %d %d %s", x, y, z, world));
			msg.setClickEvent(click);
			HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Teleport to portal coordinates").create());
			msg.setHoverEvent(hover);

			for (Player p : Portals.instance.getServer().getOnlinePlayers()) {
				if (!p.isOp()) {
					continue;
				}

				if (played_hours < SQLite.get_playtime_constraint(p.getUniqueId().toString())) {
					p.spigot().sendMessage(msg);
				}
			}
		}

		Portals.instance.getLogger().info(String.format("Portal teleporting %s from '%d %d %d %s' to '%d %d %d %s'",
				player.getName(),
				portal.getBlockX(),
				portal.getBlockY(),
				portal.getBlockZ(),
				portal.getWorld().getName(),
				lookup.destination.getBlockX(),
				lookup.destination.getBlockY(),
				lookup.destination.getBlockZ(),
				lookup.destination.getWorld().getName()
		));

		boolean differentWorld = !portal.getWorld().getName().equals(lookup.destination.getWorld().getName());

		// Find a safe location for the player to teleport to
		Location safeLocation = differentWorld ? findSafeLocation(lookup.destination, player) : lookup.destination;
		if (safeLocation == null) {
			player.sendMessage(ChatColor.RED + "No safe location found near the portal destination!");
			return;
		}

		// Teleport the player to the safe location
		safeLocation.setYaw(location.getYaw());
		safeLocation.setPitch(location.getPitch());
		player.teleport(safeLocation);
		teleportNearby(portal, lookup.destination, safeLocation, player);

		/* Fix players from being stuck sneaking after a teleport*/
		unsneak(player);

		setTeleported(player);
	}

	/**
	 * Finds a safe location for teleporting a player.
	 * 
	 * @param destination The initial destination location.
	 * @param player      The player to use for hitbox checks.
	 * @return A safe location above the destination.
	 */
	private static Location findSafeLocation(Location destination, Player player) {
		World world = destination.getWorld();
		int startX = destination.getBlockX();
		int startY = destination.getBlockY();
		int startZ = destination.getBlockZ();

		BoundingBox playerBB = player.getBoundingBox();
		double widthX = playerBB.getWidthX() / 2;
		double widthZ = playerBB.getWidthZ() / 2;
		double height = playerBB.getHeight(); // Changes depending on standing/crouching/crawling

		// Create a player sized bounding box at the portal destination.
		BoundingBox simulatedBB = new BoundingBox(
				startX + 0.5 + widthX,
				startY + height,
				startZ + 0.5 + widthZ,
				startX + 0.5 - widthX,
				startY,
				startZ + 0.5 - widthZ);

		// The vertical gap found must be >=height for it to be a valid location.
		double verticalGap = 0;

		for (int y = startY; y < world.getMaxHeight(); y++) {
			Location checkLocation = new Location(world, startX, y, startZ);
			Block block = world.getBlockAt(checkLocation);

			if (!block.isPassable()) {
				final double blockY = block.getY();
				OptionalDouble optMaxY = block.getCollisionShape().getBoundingBoxes().stream()
						.map(bb -> bb.shift(startX, blockY, startZ))
						.filter(bb -> simulatedBB.overlaps(bb))
						.mapToDouble(BoundingBox::getMaxY)
						.max();

				// The simulation collides with the block.
				if (optMaxY.isPresent()) {
					verticalGap = 0;
					simulatedBB.shift(0, optMaxY.getAsDouble() - simulatedBB.getMinY(), 0);
					continue;
				}
			}

			// No collision.
			verticalGap += 1;

			if (verticalGap >= height) {
				break;
			}
		}

		return new Location(world, startX + 0.5, simulatedBB.getMinY(), startZ + 0.5);
	}

	/**
	 * For teleporting the given player using their current location
	 * @param player player who is trying to teleport
	 */
	public static void teleport(Player player) {
		Location location = player.getLocation();
		teleport(player, location);
	}

	/**
	 * For limiting portal attempts
	 * Allows two way minecart travel and maybe fixes the instant death bug
	 * @param entity entity to stop from teleporting again too soon
	 */
	public static void setTeleported(Entity entity) {
		final UUID uuid = entity.getUniqueId();
		Portals.justTeleportedEntities.add(uuid);

		Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
			public void run() {
				Portals.justTeleportedEntities.remove(uuid);
			}
		}, 20L);
	}

	/**
	 * For stopping a player from being locked sneaking after switching worlds with a Portal
	 * @param player player to unsneak
	 */
	public static void unsneak(final Player player) {
		Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
			public void run() {
				player.setSneaking(false);
			}
		}, 1L);
	}

	/**
	 * Teleports nearby non-player entities through a nearby portal
	 * @param location Location from which someone is being teleported
	 * @param entityDest Real location to teleport entities to (May be unsafe)
	 * @param playerDest Safe location to teleport the player to
	 * @param player player who used the portal and may have to be teleported again to stop visual bug
	 */
	public static void teleportNearby(Location from, final Location entityDest, final Location playerDest, Player player) {
		/* First teleport all entities at the same time as the player
		 * The delay in previous versions seems to cause the duplication bug */
		Collection<Entity> nearby = from.getWorld().getNearbyEntities(from, 2, 2, 2);
		boolean somethingTeleported = false;
		boolean differentWorld = !from.getWorld().getName().equals(entityDest.getWorld().getName());
		for (Entity entity : nearby) {
			if (!teleportableEntities.contains(entity.getType())) {
				continue;
			}

			if (entity instanceof Sittable) {
				if (((Sittable) entity).isSitting()) {
					continue;
				}
			}

			/* Double teleports are required when changing worlds, otherwise non-player entities get stuck in the corner
			 * of a block and suffocate (destination offsets do not fix this for some reason...) */
			if (differentWorld) {
				entity.teleport(entityDest);
			}
			entity.teleport(entityDest);
			somethingTeleported = true;
		}

		/* A bug seems to make all the entities invisible for just the player who used the portal.
		 * Teleporting the player to the original location and then back seems to fix this. */
		if (somethingTeleported) { // Only bother reteleporting players if an entity came with them
			Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
				public void run() {
					player.teleport(from);
				}
			}, 1L);
			Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
				public void run() {
					player.teleport(playerDest);
				}
			}, 2L);
		}
	}

	private static HashSet<EntityType> teleportableEntities;

	/**
	 * Loads the list of teleportable entities, saving any others to a separate unused config key.
	 */
	public static void loadTeleportable() {	
		FileConfiguration config = Portals.instance.getConfig();
		List<String> configList = config.getStringList("EnabledEntities");
		HashSet<EntityType> teleportable = new HashSet<>();
		List<String> disabled = new ArrayList<>();

		for (String name : configList) {
			try {
				teleportable.add(EntityType.valueOf(name));
			} catch (IllegalArgumentException e) {
				Portals.instance.getLogger().warning("Invalid entity name in config: " + name);
			}
		}
		
		for (EntityType entity : EntityType.values()) {
			if (!teleportable.contains(entity)) disabled.add(entity.toString());	
		}

		Collections.sort(disabled);
		config.set("DisabledEntitiesAutomatic", disabled);
		Portals.instance.saveConfig();
		Portals.instance.getLogger().info("Loaded " + teleportable.size() + " teleportable entities.");
		teleportableEntities = teleportable;
	}
}
