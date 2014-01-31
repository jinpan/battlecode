package swarm3;

import battlecode.common.MapLocation;

public class WeightedMapLocation {

	double x;
	double y;
	int count;
	
	public WeightedMapLocation(MapLocation loc){
		this.x = loc.x;
		this.y = loc.y;
		this.count = 1;
	}
	
	public void add(MapLocation loc){
		x = x * count + loc.x;
		y = y * count + loc.y;
		++ count;
		x /= count;
		y /= count;
	}
}
