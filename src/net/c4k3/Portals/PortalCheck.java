package net.c4k3.Portals;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class PortalCheck {

	public static boolean is_valid_portal(Block portal) {
		return is_valid_portal(portal, null);
	}

	/**
	 * Checks whether the given location contains a valid portal
	 * Meaning whether the location has obsidian setup in such a way that would
	 * create a valid portal, not whether the obsidian there in particular is
	 * active (i.e. no SQL lookups)
	 * @param portal The location of the portal, given the lower block in its center
	 * @param broken A hack for the check used in BlockBreak. The location of a
	 * block that has just been broken, such that it can be counted as air, given
	 * that blocks are not actually broken until the BlockBreakEvent is finished.
	 * @return True if it is a valid portal, else false
	 */
	public static boolean is_valid_portal(Block portal, Block broken) {
		BlockFace[] path_eastwest = {BlockFace.DOWN, BlockFace.EAST, BlockFace.UP,
				BlockFace.UP, BlockFace.UP, BlockFace.WEST, BlockFace.WEST,
				BlockFace.DOWN, BlockFace.DOWN, BlockFace.DOWN};
		BlockFace[] path_northsouth = {BlockFace.DOWN, BlockFace.SOUTH,
				BlockFace.UP, BlockFace.UP, BlockFace.UP, BlockFace.NORTH,
				BlockFace.NORTH, BlockFace.DOWN,BlockFace.DOWN, BlockFace.DOWN};

		Block block = portal;
		boolean eastwest_valid = true;

		for (int i = 0; i < path_eastwest.length; i++) {
			block = block.getRelative(path_eastwest[i]);
			if (block.getType() != Material.OBSIDIAN 
					|| (broken != null &&block.getX() == broken.getX() 
					&& block.getY() == broken.getY() 
					&& block.getZ() == broken.getZ())) {
				eastwest_valid = false;
				break;
			}
		}

		if (eastwest_valid)
			return true;

		block = portal;
		for (int i = 0; i < path_northsouth.length; i++) {
			block = block.getRelative(path_northsouth[i]);
			if (block.getType() != Material.OBSIDIAN 
					|| (broken != null && block.getX() == broken.getX() 
					&& block.getY() == broken.getY() 
					&& block.getZ() == broken.getZ()))
				return false;
		}
		return true;
	}

}

