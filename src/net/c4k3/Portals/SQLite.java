package net.c4k3.Portals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class SQLite {

	private static Connection conn = null;

	/**
	 * Opens the SQLite connection.
	 */
	public static void connect() {

		/* Where the last part is the name of the database file */
		String database = "jdbc:sqlite:plugins/Portals/Portals.sqlite";

		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(database);

			Statement st = conn.createStatement();

			/* Get database version */
			ResultSet rs = st.executeQuery("PRAGMA user_version;");

			rs.next();
			int user_version = rs.getInt("user_version");

			rs.close();

			switch (user_version) {

			/* Database is brand new. Create tables */
			case 0: {
				Portals.instance.getLogger().info("Database not yet created. Creating ...");
				String query =  "CREATE TABLE portal_pairs "
						+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "x INT,"
						+ "y INT,"
						+ "z INT,"
						+ "world TEXT,"
						+ "pair INTEGER);" // foreign key

						+ "CREATE TABLE unset_portals "
						+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "x INT,"
						+ "y INT,"
						+ "z INT,"
						+ "world TEXT,"
						+ "uuid BLOB);" // of the creator

						+ "CREATE TABLE purchased_portals "
						+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ "uuid BLOB,"
						+ "amount INT);";
				st.executeUpdate(query);
				query = "PRAGMA user_version = 1;";
				st.executeUpdate(query);
				break;
			}

			}

			st.close();

		} catch ( Exception e ) {
			Portals.instance.getLogger().info(e.getMessage());
			return;
		}

	}

	/**
	 * Closes the database connection.
	 */
	public static void close() {
		try {
			conn.close();
		} catch(Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Inserts a portal pair into the portal_pairs table.
	 * @param portal1 Location of the first portal.
	 * @param portal2 Location of the second portal.
	 */
	public static void insert_portal_pair(Location portal1, Location portal2) {
		try {
			Statement st = conn.createStatement();

			int x1 = portal1.getBlockX();
			int y1 = portal1.getBlockY();
			int z1 = portal1.getBlockZ();
			String world1 = portal1.getWorld().getName();
			int x2 = portal2.getBlockX();
			int y2 = portal2.getBlockY();
			int z2 = portal2.getBlockZ();
			String world2 = portal2.getWorld().getName();

			String query = "INSERT INTO portal_pairs (x, y, z, world, pair) VALUES "
					+ "('" + x1 + "', '" + y1 + "', '" + z1 + "', '" + world1 + "', '-1');"
					+ "UPDATE portal_pairs SET pair = ((SELECT last_insert_rowid()) + 1) "
					+ "WHERE id = (SELECT last_insert_rowid());"
					+ "INSERT INTO portal_pairs (x, y, z, world, pair) VALUES "
					+ "('" + x2 + "', '" + y2 + "', '" + z2 + "', '" + world2 + "', (SELECT last_insert_rowid()));";

			st.executeUpdate(query);
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Delete a portal pair by id.
	 * @param id id (PK) of one of the portals.
	 */
	public static void delete_portal_pair(int id) {
		try {
			Statement st = conn.createStatement();

			String query = "DELETE FROM portal_pairs WHERE id IN "
					+ "(SELECT DISTINCT pair FROM portal_pairs WHERE x = '" + id + "');"
					+ "DELETE FROM portal_pairs WHERE id = '" + id + "';";

			st.executeUpdate(query);
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Gets the destination of a possible portal-pair. Returns a null location if there is no
	 * portal at the specified location.
	 * @param location Location of the player
	 * @return Location if it is a valid portal-pair, else null
	 */
	static Location get_other_portal(Location from) {
		Location destination = null;

		int x = from.getBlockX();
		int y = from.getBlockY();
		int z = from.getBlockZ();
		String world = from.getWorld().getName();

		try {
			Statement st = conn.createStatement();

			String query = "SELECT * FROM portal_pairs WHERE id IN "
					+ "(SELECT DISTINCT pair FROM portal_pairs WHERE x = '" + x + "' AND y = '" + y + "' AND z = '" + z + "' AND world = '" + world + "');";

			ResultSet rs = st.executeQuery(query);
			while (rs.next()) {
				destination = new Location(Portals.instance.getServer().getWorld(
						rs.getString("world")), (double) rs.getInt("x") + 0.5,
						(double) rs.getInt("y"), (double) rs.getInt("z") + 0.5);
			}

			rs.close();			
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return destination;
	}

	/**
	 * Get how many portals a player has purchased.
	 * @param uuid UUID of player.
	 * @return Amount of portals player has purchased.
	 */
	public static int get_purchased_portals(UUID uuid) {
		int amount = 0;

		try {
			Statement st = conn.createStatement();

			String query = "SELECT * FROM purchased_portals WHERE uuid = '" + uuid + "';";
			ResultSet rs = st.executeQuery(query);

			while (rs.next())
				amount = rs.getInt("amount");

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return amount;
	}

	/**
	 * Increments the player's puchased_portals by 1.
	 * @param uuid UUID of player
	 */
	public static void increment_purchased_portals(UUID uuid) {
		int amount = -1;

		try {
			Statement st = conn.createStatement();

			String query = "SELECT * FROM purchased_portals WHERE uuid = '" + uuid + "';";
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				amount = rs.getInt("amount");
			}

			rs.close();

			if (amount == -1) {
				query = "INSERT INTO purchased_portals (uuid, amount) VALUES ('" + uuid + "', 1);";
			} else {
				query = "UPDATE purchased_portals SET amount = '" + (amount + 1) + "' WHERE uuid = '" + uuid + "';";
			}

			st.executeUpdate(query);
			st.close();

		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Decrements the player's puchased_portals by 1.
	 * @param uuid UUID of player
	 */
	public static void decrement_purchased_portals(UUID uuid) {
		int amount = -1;

		try {
			Statement st = conn.createStatement();

			String query = "SELECT * FROM purchased_portals WHERE uuid = '" + uuid + "';";
			ResultSet rs = st.executeQuery(query);

			while (rs.next())
				amount = rs.getInt("amount");

			rs.close();

			if (amount <= 1) {
				query = "DELETE FROM purchased_portals WHERE uuid = '" + uuid + "';";
			} else {
				query = "UPDATE purchased_portals SET amount = '" + (amount - 1) + "' WHERE uuid = '" + uuid + "';";
			}

			st.executeUpdate(query);
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Gets the unset portal location of a given player, if such exists.
	 * @param uuid UUID of the player whose portal is to be retrieved.
	 * @return The location of the unset portal if it exists, null if it does not exist.
	 */
	public static Location get_unset_portal(UUID uuid) {
		Location portal = null;

		int x = 0;
		int y = 0;
		int z = 0;
		String world = "";
		int count = 0;

		try {
			Statement st = conn.createStatement();

			String query = "SELECT * FROM unset_portals WHERE uuid = '" + uuid + "';";
			ResultSet rs = st.executeQuery(query);

			while (rs.next()) {
				count++;
				x = rs.getInt("x");
				y = rs.getInt("y");
				z = rs.getInt("z");
				world = rs.getString("world");
			}

			rs.close();
			st.close();

			if (count > 0)			
				portal = new Location(Portals.instance.getServer().getWorld(world), x, y, z);

		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}

		return portal;
	}

	/**
	 * Inserts the location into the unset_portals table for player with UUID.
	 * @param uuid The player's UUID
	 * @param location Location
	 */
	public static void insert_unset_portal(UUID uuid, Location location) {
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		String world = location.getWorld().getName();

		try {
			Statement st = conn.createStatement();

			String query = "INSERT INTO unset_portals (x, y, z, world, uuid)"
					+ "VALUES ('" + x + "', '" + y + "', '" + z + "', '" + world + "', '" + uuid + "');";
			st.executeUpdate(query);

			st.close();

		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Remove a player's entries in the unset_portals table.
	 * @param uuid The player's UUID
	 */
	public static void delete_unset_portal(UUID uuid) {
		try {
			Statement st = conn.createStatement();

			String query = "DELETE FROM unset_portals WHERE uuid = '" + uuid + "';";
			st.executeUpdate(query);

			st.close();

		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * For checking whether there is a portal at the given location.
	 * 
	 * Called whenever an obsidian block is destroyed, this checks whther
	 * that block was part of a portal here.
	 * @param block The block that was broken.
	 * @return The id (PK) of the portal that this block is a part of.
	 */
	public static int obsidian_checker(Block block) {
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		String world = block.getWorld().getName();

		int id = -1;
		try {
			Statement st = conn.createStatement();

			String query = "SELECT id FROM portal_pairs WHERE "
					+ "world = '" + world + "' AND z = '" + z + "' AND "
					+ "x BETWEEN '" + (x - 1) + "' AND '" + (x + 1) + "' AND "
					+ "y BETWEEN '" + (y - 2) + "' AND '" + (y + 1) + "';";

			ResultSet rs = st.executeQuery(query);

			while (rs.next())
				id = rs.getInt("id");

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}

		return id;
	}

}
