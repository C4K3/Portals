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
				case 3: {
					Portals.instance.getLogger().info("Migrating to version 4 ...");
					String query = ""
							+ "CREATE TABLE portal_users "
							+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ "portal INTEGER,"
							+ "uuid BLOB,"
							+ "time INTEGER);"
							+ "CREATE INDEX idx_portal_users_portal ON portal_users (portal);"
							
							+ "CREATE TABLE portal_log "
							+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ "uuid BLOB UNIQUE,"
							+ "play_time INT);"
							+ "CREATE INDEX idx_portal_log_uuid ON portal_log (uuid);"

							+ "PRAGMA user_version = 4;";
					st = conn.createStatement();
					st.executeUpdate(query);
					st.close();
				}
			}
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
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
			Portals.instance.getLogger().severe(e.getMessage());
		}
	}

	/**
	 * Inserts a portal pair into the portal_pairs table.
	 * @param portal1 Location of the first portal.
	 * @param portal2 Location of the second portal.
	 * @param player The UUID of the player who set the portal.
	 */
	public static void insert_portal_pair(Block portal1, Block portal2, UUID player) {
		try {
			// https://github.com/xerial/sqlite-jdbc/issues/613
			// means we can't insert both rows at the same time.
			// But when we update to 3.35 we should be able to use
			// RETURNING. In the future if
			// https://sqlite.org/forum/forumpost/bd948b3b89
			// is implemented we could do that too.
			String query = "INSERT INTO portal_pairs (x, y, z, world, pair) VALUES (?, ?, ?, ?, '-1')";
			PreparedStatement st = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			st.setInt(1, portal1.getX());
			st.setInt(2, portal1.getY());
			st.setInt(3, portal1.getZ());
			st.setString(4, portal1.getWorld().getName());
			st.executeUpdate();
			ResultSet rs = st.getGeneratedKeys();
			rs.next();
			// Primary key of first portal
			int a = rs.getInt(1);
			rs.close();

			st.setInt(1, portal2.getX());
			st.setInt(2, portal2.getY());
			st.setInt(3, portal2.getZ());
			st.setString(4, portal2.getWorld().getName());
			st.executeUpdate();
			rs = st.getGeneratedKeys();
			rs.next();
			// Primary key of second portal
			int b = rs.getInt(1);
			rs.close();
			st.close();

			// Update the two rows to reference each other's primary key
			query = "WITH const(id, pair) AS (VALUES (?, ?), (?, ?)) UPDATE portal_pairs SET pair = (SELECT pair FROM const WHERE portal_pairs.id = const.id) WHERE id IN (SELECT id FROM const)";
			st = conn.prepareStatement(query);
			st.setInt(1, a);
			st.setInt(2, b);
			st.setInt(3, b);
			st.setInt(4, a);
			st.executeUpdate();
			st.close();

			// The portal's creator is considered to know where the portal is.
			add_portal_user(a, b, player);
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
	}

	/**
	 * Delete a portal pair by id.
	 * @param id id (PK) of one of the portals.
	 */
	private static void delete_portal_pair(int id) {
		try {
			PreparedStatement st;
			String query;

			query = "DELETE FROM portal_users WHERE portal IN (?, (SELECT DISTINCT pair FROM portal_pairs WHERE id = ?))";
			st = conn.prepareStatement(query);
			st.setInt(1, id);
			st.setInt(2, id);
			st.executeUpdate();
			st.close();


			query = "DELETE FROM portal_pairs WHERE id IN (?, (SELECT DISTINCT pair FROM portal_pairs WHERE id = ?))";
			st = conn.prepareStatement(query);
			st.setInt(1, id);
			st.setInt(2, id);
			st.executeUpdate();
			st.close();

		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
	}

	public static void delete_portal_pair(PortalLocation portal) {
		Portals.instance.getLogger().info(String.format("Disabling this portal at '%d %d %d %s'",
					portal.block.getX(),
					portal.block.getY(),
					portal.block.getZ(),
					portal.block.getWorld().getName()
					));

		delete_portal_pair(portal.id);
	}

	public static void delete_portal_pair(PortalLookup portal) {
		Portals.instance.getLogger().info(String.format("Disabling the portal going to '%d %d %d %s'",
					portal.destination.getBlockX(),
					portal.destination.getBlockY(),
					portal.destination.getBlockZ(),
					portal.destination.getWorld().getName()));

		delete_portal_pair(portal.a);
	}

	public static class PortalLookup {
		// Primary key of first portal
		int a;
		// Primary key of second portal
		int b;

		// Location of second portal
		Location destination;

		public PortalLookup(int a, int b, Location destination) {
			this.a = a;
			this.b = b;
			this.destination = destination;
		}
	}

	/**
	 * Gets the destination of a possible portal-pair. Returns a null location if there is no
	 * portal at the specified location.
	 * @param location Location of the player
	 * @return Location if it is a valid portal-pair, else null
	 */
	public static PortalLookup get_other_portal(Location from) {
		PortalLookup ret = null;

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
				Location destination = new Location(
						Portals.instance.getServer().getWorld(rs.getString("world")),
						(double) rs.getInt("x") + 0.5,
						(double) rs.getInt("y"),
						(double) rs.getInt("z") + 0.5,
						from.getYaw(), from.getPitch());
				int a = rs.getInt("id");
				int b = rs.getInt("pair");
				ret = new PortalLookup(a, b, destination);
			}

			rs.close();			
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
		return ret;
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
			Portals.instance.getLogger().severe(e.getMessage());
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
			Portals.instance.getLogger().severe(e.getMessage());
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
			Portals.instance.getLogger().severe(e.getMessage());
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
			Portals.instance.getLogger().severe(e.getMessage());
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
			Portals.instance.getLogger().severe(e.getMessage());
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
			Portals.instance.getLogger().severe(e.getMessage());
		}

		return locations;
	}
	
	/**
	 * Adds a player to the list of players that have used this portal.
	 * Called when a player teleports using a portal and when a player creates a portal.
	 * @param a The primary key of one of the portal pairs
	 * @param b The primary key of the other of the portal pairs
	 * @param uuid The player who used the portal's uuid
	 */
	public static void add_portal_user(int a, int b, UUID uuid) {
		try {
			String query = "INSERT INTO portal_users (portal, uuid, time) VALUES (?, ?, ?)";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(2, uuid.toString());
			st.setLong(3, System.currentTimeMillis() / 1000L);

			st.setInt(1, a);
			st.executeUpdate();

			st.setInt(1, b);
			st.executeUpdate();

			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
	}
	
	/**
	 * Gets a list of all players who have used this portal.
	 * Called whenever a player teleports using a portal.
	 * @param portal ID.
	 * @return An arraylist of players who have used the portal.
	 */
	public static ArrayList<UUID> get_portal_users(int id) {
		
		ArrayList<UUID> UserList = new ArrayList<>();
		
		try {
			String query = "SELECT uuid FROM portal_users WHERE portal = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setInt(1, id);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				String user_uuid = rs.getString("uuid");
				UUID uuid;
				try {
					uuid = UUID.fromString(user_uuid);
				} catch (Exception e) {
					Portals.instance.getLogger().warning("Invalid uuid from portal_users: " + user_uuid);
					continue;
				}
				UserList.add(uuid);
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
		return UserList;
	}
	
	/**
	 * Gets a constraint that limits which players alert admins when they use portal use.
	 * Called when a player teleports using a portal.
	 * @param uuid of the admin.
	 * @return An integer. All players with less hours than this integer
	 * will be logged
	 */
	public static int get_playtime_constraint(String uuid) {
		int portallog_int = 0;
		try {
			String query = "SELECT * FROM portal_log WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid);

			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				portallog_int = rs.getInt("play_time");
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
		return portallog_int;
	}
	/**
	 * Sets whether or not admins want to be alerted of portal logs
	 * Called when an admin uses /portallog
	 * @param uuid of the admin, max hours integer.
	 */
	public static void set_portallog(String uuid, Integer playtime) {
		try {
			String query = "INSERT OR REPLACE INTO portal_log (uuid, play_time) VALUES (?, ?)";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid);
			st.setInt(2, playtime);
			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			Portals.instance.getLogger().severe(e.getMessage());
		}
	}	
}

