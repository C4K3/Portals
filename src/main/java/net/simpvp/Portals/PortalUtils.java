package net.simpvp.Portals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;

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
		Location safeLocation = differentWorld ? findSafeLocation(lookup.destination) : lookup.destination;
		if (safeLocation == null) {
			player.sendMessage(ChatColor.RED + "No safe location found near the portal destination!");
			return;
		}

		// Teleport the player to the safe location
		safeLocation.setYaw(location.getYaw());
		safeLocation.setPitch(location.getPitch());
		player.teleport(safeLocation);
		teleportNearby(portal, safeLocation, player);

		/* Fix players from being stuck sneaking after a teleport*/
		unsneak(player);

		setTeleported(player);
	}

	/**
	 * Finds a safe location for teleporting a player.
	 * @param destination The initial destination location.
	 * @return A safe location near the destination or null if no safe location is found.
	 */
	private static Location findSafeLocation(Location destination) {
		World world = destination.getWorld();
		int startX = destination.getBlockX();
		int startY = destination.getBlockY();
		int startZ = destination.getBlockZ();

		for (int y = startY; y < world.getMaxHeight(); y++) {
			Location checkLocation = new Location(world, startX, y, startZ);
			Block block1 = world.getBlockAt(checkLocation);
			Block block2 = world.getBlockAt(checkLocation.add(0, 1, 0));

			if (block1.isPassable() && block2.isPassable()) {
				return new Location(world, startX + 0.5, y, startZ + 0.5);
			}
		}
		return null;
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
	 * @param destination Location of the portal to which things are to be teleported
	 * @param player player who used the portal and may have to be teleported again to stop visual bug
	 */
	public static void teleportNearby(Location from, final Location destination, Player player) {
		/* First teleport all entities at the same time as the player
		 * The delay in previous versions seems to cause the duplication bug */
		Collection<Entity> nearby = from.getWorld().getNearbyEntities(from, 2, 2, 2);
		boolean somethingTeleported = false;
		boolean differentWorld = !from.getWorld().getName().equals(destination.getWorld().getName());
		for (Entity entity : nearby) {
			if (!TELEPORTABLE_ENTITIES.contains(entity.getType())) {
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
				entity.teleport(destination);
			}
			entity.teleport(destination);
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
					player.teleport(destination);
				}
			}, 2L);
		}
	}

	private static final HashSet<EntityType> TELEPORTABLE_ENTITIES = new HashSet<EntityType>(Arrays.asList(
			EntityType.ALLAY,
			// EntityType.AREA_EFFECT_CLOUD,
			// EntityType.ARMOR_STAND,
			// EntityType.ARROW,
			EntityType.AXOLOTL,
			EntityType.BAT,
			EntityType.BLAZE,
			// EntityType.BLOCK_DISPLAY,
			EntityType.BOAT,
			EntityType.CAMEL,
			EntityType.CAT,
			EntityType.CAVE_SPIDER,
			EntityType.CHEST_BOAT,
			EntityType.CHICKEN,
			EntityType.COD,
			EntityType.COW,
			EntityType.CREEPER,
			EntityType.DOLPHIN,
			EntityType.DONKEY,
			// EntityType.DRAGON_FIREBALL,
			EntityType.DROPPED_ITEM,
			EntityType.DROWNED,
			EntityType.EGG,
			EntityType.ELDER_GUARDIAN,
			// EntityType.ENDER_CRYSTAL,
			// EntityType.ENDER_DRAGON,
			EntityType.ENDER_PEARL,
			// EntityType.ENDER_SIGNAL,
			EntityType.ENDERMAN,
			EntityType.ENDERMITE,
			EntityType.EVOKER,
			// EntityType.EVOKER_FANGS,
			EntityType.EXPERIENCE_ORB,
			// EntityType.FALLING_BLOCK,
			EntityType.FIREBALL,
			// EntityType.FIREWORK,
			// EntityType.FISHING_HOOK,
			EntityType.FOX,
			EntityType.FROG,
			EntityType.GHAST,
			// EntityType.GIANT,
			// EntityType.GLOW_ITEM_FRAME,
			EntityType.GLOW_SQUID,
			EntityType.GOAT,
			EntityType.GUARDIAN,
			EntityType.HOGLIN,
			EntityType.HORSE,
			EntityType.HUSK,
			EntityType.ILLUSIONER,
			// EntityType.INTERACTION,
			EntityType.IRON_GOLEM,
			// EntityType.ITEM_DISPLAY,
			// EntityType.ITEM_FRAME,
			// EntityType.LEASH_HITCH,
			// EntityType.LIGHTNING,
			EntityType.LLAMA,
			// EntityType.LLAMA_SPIT,
			EntityType.MAGMA_CUBE,
			// EntityType.MARKER,
			EntityType.MINECART,
			EntityType.MINECART_CHEST,
			// EntityType.MINECART_COMMAND,
			EntityType.MINECART_FURNACE,
			EntityType.MINECART_HOPPER,
			// EntityType.MINECART_MOB_SPAWNER,
			EntityType.MINECART_TNT,
			EntityType.MULE,
			EntityType.MUSHROOM_COW,
			EntityType.OCELOT,
			// EntityType.PAINTING,
			EntityType.PANDA,
			EntityType.PARROT,
			// EntityType.PHANTOM,
			EntityType.PIG,
			EntityType.PIGLIN,
			EntityType.PIGLIN_BRUTE,
			EntityType.PILLAGER,
			// EntityType.PLAYER,
			EntityType.POLAR_BEAR,
			EntityType.PRIMED_TNT,
			EntityType.PUFFERFISH,
			EntityType.RABBIT,
			EntityType.RAVAGER,
			EntityType.SALMON,
			EntityType.SHEEP,
			EntityType.SHULKER,
			// EntityType.SHULKER_BULLET,
			EntityType.SILVERFISH,
			EntityType.SKELETON,
			EntityType.SKELETON_HORSE,
			EntityType.SLIME,
			// EntityType.SMALL_FIREBALL,
			EntityType.SNIFFER,
			EntityType.SNOWBALL,
			EntityType.SNOWMAN,
			// EntityType.SPECTRAL_ARROW,
			EntityType.SPIDER,
			// EntityType.SPLASH_POTION,
			EntityType.SQUID,
			EntityType.STRAY,
			EntityType.STRIDER,
			EntityType.TADPOLE,
			// EntityType.TEXT_DISPLAY,
			// EntityType.THROWN_EXP_BOTTLE,
			EntityType.TRADER_LLAMA,
			// EntityType.TRIDENT,
			EntityType.TROPICAL_FISH,
			EntityType.TURTLE,
			// EntityType.UNKNOWN,
			EntityType.VEX,
			EntityType.VILLAGER,
			EntityType.VINDICATOR,
			EntityType.WANDERING_TRADER,
			EntityType.WARDEN,
			EntityType.WITCH,
			// EntityType.WITHER,
			EntityType.WITHER_SKELETON,
			// EntityType.WITHER_SKULL,
			EntityType.WOLF,
			EntityType.ZOGLIN,
			EntityType.ZOMBIE,
			EntityType.ZOMBIE_HORSE,
			EntityType.ZOMBIE_VILLAGER,
			EntityType.ZOMBIFIED_PIGLIN
	));
}
