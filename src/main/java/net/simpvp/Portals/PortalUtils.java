package net.simpvp.Portals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Statistic;
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
			Portals.instance.getLogger().info(player.getName() + " destination was null.");
			return;
		}

		/* Stop Yaw and Pitch from changing if portal location is not directly from player */
		lookup.destination.setYaw(location.getYaw());
		lookup.destination.setPitch(location.getPitch());

		/* Make sure a valid portal is at destination */
		if (!PortalCheck.is_valid_portal(lookup.destination.getBlock())) {
			Portals.instance.getLogger().info(player.getName() + " destination portal frame is missing.");

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

		/* workaround for laggy teleports, see
		 * https://bukkit.org/threads/workaround-for-playing-falling-after-teleport-when-lagging.293035/ */
		Location fLoc = new Location(lookup.destination.getWorld(),
				lookup.destination.getBlockX(),
				lookup.destination.getBlockY() - 1,
				lookup.destination.getBlockZ());
		player.sendBlockChange(fLoc, fLoc.getBlock().getBlockData());
		fLoc = new Location(lookup.destination.getWorld(),
				lookup.destination.getBlockX(),
				lookup.destination.getBlockY(),
				lookup.destination.getBlockZ());
		player.sendBlockChange(fLoc, fLoc.getBlock().getBlockData());

		player.teleport(lookup.destination);
		teleportNearby(portal, lookup.destination, player);

		/* Fix players from being stuck sneaking after a teleport*/
		unsneak(player);

		setTeleported(player);
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
		for (Entity entity : nearby) {
			if (!TELEPORTABLE_ENTITIES.contains(entity.getType())) {
				continue;
			}

			if (entity instanceof Sittable) {
				if (((Sittable) entity).isSitting()) {
					continue;
				}
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
			// EntityType.AREA_EFFECT_CLOUD,
			// EntityType.ARMOR_STAND,
			//EntityType.ARROW,
			EntityType.AXOLOTL,
			EntityType.BAT,
			EntityType.BLAZE,
			EntityType.BOAT,
			EntityType.CAT,
			EntityType.CAVE_SPIDER,
			EntityType.CHICKEN,
			EntityType.COW,
			EntityType.CREEPER,
			EntityType.DOLPHIN,
			EntityType.DONKEY,
			// EntityType.DRAGON_FIREBALL,
			EntityType.DROPPED_ITEM,
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
			EntityType.GHAST,
			EntityType.GLOW_SQUID,
			EntityType.GOAT,
			// EntityType.GIANT,
			EntityType.GUARDIAN,
			EntityType.HOGLIN,
			EntityType.HORSE,
			EntityType.HUSK,
			EntityType.ILLUSIONER,
			EntityType.IRON_GOLEM,
			// EntityType.ITEM_FRAME,
			// EntityType.LEASH_HITCH,
			// EntityType.LIGHTNING,
			EntityType.LLAMA,
			// EntityType.LLAMA_SPIT,
			EntityType.MAGMA_CUBE,
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
			// EntityType.SNOWBALL,
			EntityType.SNOWMAN,
			// EntityType.SPECTRAL_ARROW,
			EntityType.SPIDER,
			// EntityType.SPLASH_POTION,
			EntityType.SQUID,
			EntityType.STRAY,
			EntityType.STRIDER,
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

