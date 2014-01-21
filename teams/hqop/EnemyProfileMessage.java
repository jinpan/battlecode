package hqop;

import battlecode.common.MapLocation;

/*
 * Class for encoding enemy profile information inside messages.
 * 
 * 16 bytes.
 * Bytes 1-3: enemy ID
 * Bytes 4-5: enemy health
 * Bytes 6-9: enemy location
 * Bytes 10-12: enemy last seen time
 */

public class EnemyProfileMessage implements Message {
	

	protected int id;
	public int health;
	public MapLocation lastSeenLoc;
	public int lastSeenTime;

	public EnemyProfileMessage(int id, int health, MapLocation loc, int time) {
		this.id = id;
		this.health = health;
		this.lastSeenLoc = loc;
		this.lastSeenTime = time;

	}

	@Override
	public long encode() {
		long result = 0;
		result += this.id; result <<= 8;
		result += this.health; result <<= 8;
		result += this.lastSeenLoc.x; result <<= 8;
		result += this.lastSeenLoc.y; result <<= 12;
		result += this.lastSeenTime;
		
		return result;
	}
	
	public static EnemyProfileMessage decode(long message) {
		int time = (int) (message % 0x1000); message >>= 12;
		int y = (int) (message % 0x100); message >>= 8;
		int x = (int) (message % 0x100); message >>= 8;
		int health = (int) (message % 0x100); message >>= 8;
		int enemyID = (int) (message % 0x1000); assert(enemyID == message);
		MapLocation loc = new MapLocation(x, y);
		
		return new EnemyProfileMessage(enemyID, health, loc, time);
	}

}
