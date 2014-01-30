package swarm3;

import battlecode.common.MapLocation;

public class LocationMessage {

	MapLocation noise_loc;
	MapLocation pastr_loc;
	
	public LocationMessage(MapLocation noise_loc, MapLocation pastr_loc){
		this.noise_loc = noise_loc;
		this.pastr_loc = pastr_loc;
	}
	
	public int encode(){
		int result = 0;
		result += noise_loc.x; result <<= 8;
		result += noise_loc.y; result <<= 8;
		result += pastr_loc.x; result <<= 8;
		result += pastr_loc.y;
		
		return result;
	}
	
	public static LocationMessage decode(int msg){
		if (msg == 0) return null;
		
		int y2 = msg % 0x100; msg >>>= 8;
		int x2 = msg % 0x100; msg >>>= 8;
		int y1 = msg % 0x100; msg >>>= 8;
		int x1 = msg % 0x100;
		
		return new LocationMessage(new MapLocation(x1, y1), new MapLocation(x2, y2));
	}
	
}
