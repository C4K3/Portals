package net.simpvp.Portals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SQLite {

	private static Connection conn = null;

	/**
	 * Opens the SQLite connection.
	 */
	@SuppressWarnings("fallthrough")
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
			st.close();

			switch (user_version) {

				/* Database is brand new. Create tables */
				case 0: {
					Portals.instance.getLogger().info("Database not yet created. Creating ...");
					String query = ""
						+ "CREATE TABLE portal_pairs "
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
						+ "amount INT);"
						
						+ "CREATE TABLE portal_users "
						+ "(id INTEGER,"
						+ "uuid BLOB);"

						+ "PRAGMA user_version = 1;";
					st = conn.createStatement();
					st.executeUpdate(query);
					st.close();
				}
				case 1: {
					Portals.instance.getLogger().info("Migrating to version 2 ...");
					String query = ""
						+ "DROP TABLE purchased_portals;"
						+ "PRAGMA user_version = 2;";
					st = conn.createStatement();
					st.executeUpdate(query);
					st.close();
				}

				case 2: {
					Portals.instance.getLogger().info("Migrating to version 3 ...");
					String query = ""
						+ "CREATE INDEX idx_portal_pairs_id ON portal_pairs (id);"
						+ "CREATE INDEX idx_portal_pairs_coords ON portal_pairs (world, x, y, z);"
						+ "CREATE INDEX idx_unset_portals_uuid ON unset_portals (uuid);"
						+ "CREATE INDEX idx_unset_portals_coords ON unset_portals (world, x, y, z);"
						+ "PRAGMA user_version = 3;";
					st = conn.createStatement();
					st.executeUpdate(query);
					st.close();
				}
			}
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
	public static void insert_portal_pair(Block portal1, Block portal2) {
		try {
			Statement st = conn.createStatement();

			int x1 = portal1.getX();
			int y1 = portal1.getY();
			int z1 = portal1.getZ();
			String world1 = portal1.getWorld().getName();
			int x2 = portal2.getX();
			int y2 = portal2.getY();
			int z2 = portal2.getZ();
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
	public static void delete_portal_pair(PortalLocation portal) {
		Portals.instance.getLogger().info("Disabling this portal at "
				+ portal.block.getWorld().getName() + " "
				+ portal.block.getX() + " "
				+ portal.block.getY() + " "
				+ portal.block.getZ());

		try {
			String query = "DELETE FROM portal_pairs WHERE id IN "
				+ "(SELECT DISTINCT pair FROM portal_pairs WHERE id = ?);";
			PreparedStatement st = conn.prepareStatement(query);
			st.setInt(1, portal.id);
			st.executeUpdate();

			query = "DELETE FROM portal_pairs WHERE id = ?";
			st = conn.prepareStatement(query);
			st.setInt(1, portal.id);
			st.executeUpdate();
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
	public static Location get_other_portal(Location from) {
		Location destination = null;

		int x = from.getBlockX();
		int y = from.getBlockY();
		int z = from.getBlockZ();
		String world = from.getWorld().getName();

		try {
			String query = "SELECT * FROM portal_pairs WHERE id IN "
				+ "(SELECT DISTINCT pair FROM portal_pairs "
				+ "WHERE x = ? AND y = ? AND z = ? AND world = ?);";
			PreparedStatement st = conn.prepareStatement(query);
			st.setInt(1, x);
			st.setInt(2, y);
			st.setInt(3, z);
			st.setString(4, world);

			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				destination = new Location(Portals.instance.getServer().getWorld(
							rs.getString("world")), (double) rs.getInt("x") + 0.5,
						(double) rs.getInt("y"), (double) rs.getInt("z") + 0.5,
						from.getYaw(), from.getPitch());
			}

			rs.close();			
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return destination;
	}

	/**
	 * Gets a portal by it's exact location.
	 * @param block The block that is the exact block where the portal is.
	 * @return The id of the portal. -1 if no such portal exists.
	 */
	public static int get_portal_by_location(Block block) {
		int id = -1;
		String world = block.getWorld().getName();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		try {
			String query = "SELECT id FROM portal_pairs "
				+ "WHERE world = ? AND x = ? AND y = ? AND z = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, world);
			st.setInt(2, x);
			st.setInt(3, y);
			st.setInt(4, z);

			ResultSet rs = st.executeQuery();

			while (rs.next())
				id = rs.getInt("id");

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return id;
	}

	/**
	 * Gets the unset portal location of a given player, if such exists.
	 * @param uuid UUID of the player whose portal is to be retrieved.
	 * @return The location of the unset portal if it exists, null if it does not exist.
	 */
	public static Block get_unset_portal(UUID uuid) {
		Block portal = null;

		try {
			String query = "SELECT * FROM unset_portals WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				World world = Portals.instance.getServer().getWorld(rs.getString("world"));
				portal = world.getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return portal;
	}

	/**
	 * Gets an unset portal by it's exact location.
	 * @param block The block that is the exact block where the portal is.
	 * @return The id of the portal. -1 if no such portal exists.
	 */
	public static int get_unset_by_location(Block block) {
		int id = -1;
		String world = block.getWorld().getName();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		try {
			String query = "SELECT id FROM unset_portals "
				+ "WHERE world = ? AND x = ? AND y = ? AND z = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, world);
			st.setInt(2, x);
			st.setInt(3, y);
			st.setInt(4, z);

			ResultSet rs = st.executeQuery();

			while (rs.next())
				id = rs.getInt("id");

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return id;
	}

	/**
	 * Inserts the location into the unset_portals table for player with UUID.
	 * @param uuid The player's UUID
	 * @param location Location
	 */
	public static void insert_unset_portal(UUID uuid, Block location) {
		int x = location.getX();
		int y = location.getY();
		int z = location.getZ();
		String world = location.getWorld().getName();

		try {
			String query = "INSERT INTO unset_portals "
				+ "(x, y, z, world, uuid) VALUES (?, ?, ?, ?, ?)";
			PreparedStatement st = conn.prepareStatement(query);
			st.setInt(1, x);
			st.setInt(2, y);
			st.setInt(3, z);
			st.setString(4, world);
			st.setString(5, uuid.toString());

			st.executeUpdate();
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
			String query = "DELETE FROM unset_portals WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());

			st.executeUpdate();
			st.close();

		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
	}

	/**
	 * For checking whether there is a portal at the given location.
	 * 
	 * Called whenever an obsidian block is destroyed, this checks whether
	 * that block was part of a portal here.
	 * @param block The block that was broken.
	 * @return An arraylist of PortalLocations for portals within range of
	 * the broken block.
	 */
	public static ArrayList<PortalLocation> obsidian_checker(Block block) {
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		String world = block.getWorld().getName();

		ArrayList<PortalLocation> locations = new ArrayList<PortalLocation>();

		try {
			String query = "SELECT * FROM portal_pairs WHERE world = ? AND "
				+ "x BETWEEN ? AND ? AND "
				+ "y BETWEEN ? AND ? AND "
				+ "z BETWEEN ? AND ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, world);
			st.setInt(2, x - 1);
			st.setInt(3, x + 1);
			st.setInt(4, y - 2);
			st.setInt(5, y + 1);
			st.setInt(6, z - 1);
			st.setInt(7, z + 1);

			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				int id = rs.getInt("id");
				Block portalblock = block.getWorld().getBlockAt(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
				locations.add(new PortalLocation(portalblock, id));
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}

		return locations;
	}
	
	//add players that use a portal
		public static void add_portal_user(Integer id, String uuid) {
			try {
				String query = "INSERT INTO portal_users "
						+ "(id, uuid) VALUES (?, ?)";
				PreparedStatement st = conn.prepareStatement(query);
				st.setInt(1, id);
				st.setString(2, uuid);

				st.executeUpdate();
				st.close();
			} catch (Exception e) {
				Portals.instance.getLogger().info(e.getMessage());
			}

		}
	
	//Get list of all players that have used a portal
	public static ArrayList<String> get_portal_users(Integer id) {
		
		ArrayList<String> UserList = new ArrayList<String>();
		
		try {
			String query = "SELECT * FROM portal_users WHERE id = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setInt(1, id);

			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				String user_uuid = rs.getString("uuid");
				UserList.add(new String(user_uuid));
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().info(e.getMessage());
		}
		return UserList;
		
	}
	
}

