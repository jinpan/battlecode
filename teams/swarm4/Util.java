package swarm4;

import battlecode.common.MapLocation;

public class Util {

	
	public static int abs(int a){
		if (a > 0) return a;
		else return -a;
	}
	
	public static double abs(double a){
		if (a > 0) return a;
		else return -a;
	}
	
	public static int max(int a, int b){
		return (a > b) ? a : b;
	}
	
	public static int min(int a, int b){
		return (a > b) ? b : a;
	}
	
	public static int manhattanDist(MapLocation a, MapLocation b){
		return abs(a.x - b.x) + abs(a.y - b.y);
	}
}
