package net.c4k3.Portals;

import org.bukkit.block.Block;

/**
 * Basically just a pair pairing a Block that is the location of the portal,
 * and the portal's ID.
 *
 */
public class PortalLocation {
	public Block block;
	public int id;
	
	public PortalLocation(Block block, int id) {
		this.block = block;
		this.id = id;
	}

}

