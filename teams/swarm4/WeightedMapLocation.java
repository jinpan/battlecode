package swarm4;

import battlecode.common.MapLocation;

public class WeightedMapLocation {
	
	int totalX, totalY;
	
	int weight;
	
	public WeightedMapLocation(MapLocation loc){
		this.totalX = loc.x;
		this.totalY = loc.y;
		this.weight = 1;
	}
	
	public void add(MapLocation loc){
		this.totalX += loc.x;
		this.totalY += loc.y;
		++weight;
	}
	
	public double manhattanDistTo(MapLocation loc){
		return Util.abs(totalX/weight - loc.x) + Util.abs(totalY/weight - loc.y);
	}
	
	public MapLocation toMapLocation(){
		return new MapLocation(totalX/weight, totalY/weight);
	}

}
