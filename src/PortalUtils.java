package net.simpvp.Portals;

import java.util.Collection;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class PortalUtils {

	/**
	 * For teleporting the given entity using a specific portal
	 * @param entity entity who is trying to teleport
	 * @param portal block to check for portal location at
	 */
	@SuppressWarnings("deprecation")
	public static void teleport(Player player) {

		/* Check if the player is trying to teleport again too fast */
		if (Portals.justTeleportedEntities.contains(player.getUniqueId())) {
			return;
		}

		Location location = player.getLocation();

		Location destination = SQLite.get_other_portal(location);

		/* If this portal is not a Portals portal */
		if (destination == null) {
			Portals.instance.getLogger().info(player.getName() + " destination was null.");
			return;
		}

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
		teleportNearby(location, destination);
		
		//reloadEntities(player, location, destination);
		
		/* Fix players from being stuck sneaking after a teleport*/
		player.setSneaking(false);

		setTeleported(player);
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
		}, 2 * 20L);
	}

	/**
	 * Teleports nearby non-player entities through a nearby portal
	 * @param location Location from which someone is being teleported
	 * @param destination Location of the portal to which things are to be teleported
	 */
	public static void teleportNearby(Location from, final Location destination) {

		Collection<Entity> nearby = from.getWorld().getNearbyEntities(from, 2, 2, 2);

		if (nearby.isEmpty())
			return;

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
	
	/**
	 * Teleports the player away and then back so that invisible entities are loaded
	 * @param player Player to reload entities for
	 */
	public static void reloadEntities(final Player player, final Location location, final Location destination) {
		
		player.teleport(location);
		
		Portals.instance.getServer().getScheduler().runTaskLater(Portals.instance, new Runnable() {
			public void run() {	
				player.teleport(destination);
			}
		}, 1L);
	}

	private static final HashSet<EntityType> teleportable_entities = new HashSet<EntityType>(Arrays.asList(
				EntityType.ARMOR_STAND,
				EntityType.ARROW,
				EntityType.BAT,
				EntityType.BLAZE,
				EntityType.BOAT,
				EntityType.CAVE_SPIDER,
				EntityType.CHICKEN,
				EntityType.COW,
				EntityType.CREEPER,
				EntityType.DROPPED_ITEM,
				EntityType.EGG,
				EntityType.ENDERMAN,
				EntityType.ENDERMITE,
				EntityType.EXPERIENCE_ORB,
				EntityType.FALLING_BLOCK,
				EntityType.FIREBALL,
				EntityType.FIREWORK,
				EntityType.GHAST,
				EntityType.GUARDIAN,
				EntityType.HORSE,
				EntityType.IRON_GOLEM,
				EntityType.MAGMA_CUBE,
				EntityType.MINECART,
				EntityType.MINECART_CHEST,
				EntityType.MINECART_FURNACE,
				EntityType.MINECART_HOPPER,
				EntityType.MINECART_TNT,
				EntityType.MUSHROOM_COW,
				EntityType.OCELOT,
				EntityType.PIG,
				EntityType.PIG_ZOMBIE,
				EntityType.PRIMED_TNT,
				EntityType.RABBIT,
				EntityType.SHEEP,
				EntityType.SILVERFISH,
				EntityType.SKELETON,
				EntityType.SLIME,
				EntityType.SMALL_FIREBALL,
				EntityType.SNOWBALL,
				EntityType.SNOWMAN,
				EntityType.SPIDER,
				EntityType.SPLASH_POTION,
				EntityType.SQUID,
				EntityType.VILLAGER,
				EntityType.WITCH,
				EntityType.WOLF,
				EntityType.ZOMBIE
					));

}

