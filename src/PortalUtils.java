package net.simpvp.Portals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PortalUtils {

	/**
	 * For teleporting the given player using a specific portal
	 * @param player player who is trying to teleport
	 * @param portal block to check for portal location at
	 */
	@SuppressWarnings("deprecation")
	public static void teleport(Player player, Location portal) {

		/* Check if the player is trying to teleport again too fast */
		if (Portals.justTeleportedEntities.contains(player.getUniqueId())) {
			return;
		}
		
		Location location = player.getLocation();
		Location destination = SQLite.get_other_portal(portal);

		/* If this portal is not a Portals portal */
		if (destination == null) {
			Portals.instance.getLogger().info(player.getName() + " destination was null.");
			return;
		}
		
		/* Stop Yaw and Pitch from changing if portal location is not directly from player */
		destination.setYaw(location.getYaw());
		destination.setPitch(location.getPitch());

		/* Make sure a valid portal is at destination */
		if (!PortalCheck.is_valid_portal(destination.getBlock())) {
			Portals.instance.getLogger().info(player.getName() + " destination portal frame is missing.");
			return;
		}

		Portals.instance.getLogger().info("Teleporting "
				+ player.getName() + " to " + destination.getWorld().getName()
				+ " " + destination.getBlockX() + " " + destination.getBlockY() 
				+ " " + destination.getBlockZ());

		/* workaround for laggy teleports, see
		 * https://bukkit.org/threads/workaround-for-playing-falling-after-teleport-when-lagging.293035/ */
		Location fLoc = new Location(destination.getWorld(),
				destination.getBlockX(), destination.getBlockY() - 1,
				destination.getBlockZ());
		player.sendBlockChange(fLoc, fLoc.getBlock().getType(), fLoc.getBlock().getData());
		
		player.teleport(destination);		
		teleportNearby(portal, destination);	
	
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
	 */
	public static void teleportNearby(Location from, final Location destination) {

		Collection<Entity> nearby = from.getWorld().getNearbyEntities(from, 2, 2, 2);
		
		for (Entity entity : nearby) {
			if (!teleportable_entities.contains(entity.getType()))
				continue;

			if (Portals.justTeleportedEntities.contains(entity.getUniqueId()))
				continue;
			
			setTeleported(entity);
			
			/* Make sure mobs don't despawn because of players being too far away while teleporting */
			boolean isPersistant = false;
			if (entity instanceof LivingEntity) {
				isPersistant = ((LivingEntity) entity).getRemoveWhenFarAway();
				((LivingEntity) entity).setRemoveWhenFarAway(false);
			}			
			
			
			/* Delay teleport so the entity doesn't go invisible */
			final Entity teleportEntity = entity;
			final boolean isPersistantFinal = isPersistant;
			
			Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
				public void run() {
					teleportEntity.teleport(destination);
				
					if (teleportEntity instanceof LivingEntity) {
						((LivingEntity) teleportEntity).setRemoveWhenFarAway(isPersistantFinal);
					}
				}
			}, 20L);
		}
	}

	private static final HashSet<EntityType> teleportable_entities = new HashSet<EntityType>(Arrays.asList(
			// EntityType.AREA_EFFECT_CLOUD,
			// EntityType.ARMOR_STAND,
			EntityType.ARROW,
			EntityType.BAT,
			EntityType.BLAZE, 
			EntityType.BOAT,
			EntityType.CAVE_SPIDER, 
			EntityType.CHICKEN, 
			// EntityType.COMPLEX_PART, 
			EntityType.COW, 
			EntityType.CREEPER, 
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
			EntityType.GHAST, 
			// EntityType.GIANT, 
			EntityType.GUARDIAN, 
			EntityType.HORSE, 
			EntityType.HUSK, 
			EntityType.IRON_GOLEM, 
			// EntityType.ITEM_FRAME,
			// EntityType.LEASH_HITCH,
			// EntityType.LIGHTNING,
			EntityType.LINGERING_POTION,
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
			EntityType.PIG, 
			EntityType.PIG_ZOMBIE, 
			// EntityType.PLAYER, 
			EntityType.POLAR_BEAR, 
			EntityType.PRIMED_TNT,
			EntityType.RABBIT, 
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
			// EntityType.THROWN_EXP_BOTTLE,
			EntityType.TIPPED_ARROW,
			// EntityType.UNKNOWN,
			EntityType.VEX, 
			EntityType.VILLAGER, 
			EntityType.VINDICATOR, 
			// EntityType.WEATHER, 
			EntityType.WITCH, 
			// EntityType.WITHER, 
			EntityType.WITHER_SKELETON, 
			// EntityType.WITHER_SKULL,
			EntityType.WOLF, 
			EntityType.ZOMBIE, 
			EntityType.ZOMBIE_HORSE, 
			EntityType.ZOMBIE_VILLAGER 		
					));

}

