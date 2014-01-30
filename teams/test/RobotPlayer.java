package test;

import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController rc) throws GameActionException {

		if (rc.getTeam() == Team.A)
			System.out.println("RANDOM " + classifyRandom(rc));
		else
			System.out.println("RANDOM " + classifyRandomFast(rc));
		
		while (true){
			rc.yield();
		}
	}
	
	static boolean classifyRandom(RobotController rc){
		double[][] map = rc.senseCowGrowth();
		int void_count = 0;
		int void_count2 = 0;
		MapLocation loc, loc2;
		for (int i=0; i<rc.getMapWidth(); ++i){
			for (int j=0; j<rc.getMapHeight(); ++j){
				loc = new MapLocation(i, j);
				if (map[i][j] == 0 && rc.senseTerrainTile(loc) == TerrainTile.VOID){
					++void_count;
					int neighbor_count = 0;
					for (int k=0; k<8; k+=2){
						Direction dir = Direction.values()[k];
						loc2 = loc.add(dir);
						if (loc2 != null && loc2.x >= 0 && loc2.y >= 0 && loc2.x < rc.getMapWidth() && loc2.y < rc.getMapHeight()){
							if (map[loc2.x][loc2.y] == 0 && rc.senseTerrainTile(loc2) == TerrainTile.VOID){
								++neighbor_count;
							}
						}
					}
					if (neighbor_count > 0){
						++void_count2;
					}
				}
			}
		}
		
		System.out.println("VOID_COUNT1 " + void_count);
		System.out.println("VOID_COUNT2 " + void_count2);
		
		double percent1 = ((float) (void_count)) / (rc.getMapWidth() * rc.getMapHeight());
		double percent2 = ((float) (void_count2)) / (rc.getMapWidth() * rc.getMapHeight());
		
		return percent1 > 0.1 &&  1.25 * percent2 < percent1 && percent2 > 0.01;
	}
	
	static boolean classifyRandomFast(RobotController rc){
		double[][] map = rc.senseCowGrowth();
		int void_count = 0;
		int void_count2 = 0;
		MapLocation loc, loc2;
		for (int i=1; i<rc.getMapWidth()-1; ++i){
			for (int j=1; j<rc.getMapHeight()-1; ++j){
				loc = new MapLocation(i, j);
				if (map[i][j] == 0 && rc.senseTerrainTile(loc) == TerrainTile.VOID){
					++void_count;
					boolean has_neighbor = false;
					for (int k=0; k<8; k+=2){
						Direction dir = Direction.values()[k];
						loc2 = loc.add(dir);
						if (map[loc2.x][loc2.y] == 0 && rc.senseTerrainTile(loc2) == TerrainTile.VOID){
							has_neighbor = true;
							break;
						}
					}
					if (has_neighbor){
						++void_count2;
					}
				}
			}
		}
		
		System.out.println("VOID_COUNT1 " + void_count);
		System.out.println("VOID_COUNT2 " + void_count2);
		
		double percent1 = ((float) (void_count)) / (rc.getMapWidth() * rc.getMapHeight());
		double percent2 = ((float) (void_count2)) / (rc.getMapWidth() * rc.getMapHeight());
		
		return percent1 > 0.1 &&  1.25 * percent2 < percent1 && percent2 > 0.01;
	}
}
