package swarm4;

import java.util.LinkedList;

import battlecode.common.*;

public class NoisePlayer extends BaseRobot{
	
	double health;
	double[][] grouped_spawn_rates;
	double[][] spawn_rates;
	Direction[] direction_order = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
	Direction[] direction_order_ext = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
			Direction.SOUTH_EAST, Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST};
	MapLocation my_loc;
	
	boolean clumping;
	MapLocation[] clumps;
	MapLocation[] clump_plan;
	int clump_idx, clump_plan_idx;
	
	boolean herding;
	MapLocation[] herd_path;
	int herd_idx;
	MapLocation pastr_loc;
	Navigation navigator;
	

	public NoisePlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		health = myRC.getHealth();
		myRC.broadcast(NOISE_DIBS_CHANNEL, 2);
		my_loc = myRC.getLocation();
		navigator = new Navigation(myRC);
		navigator.map[my_loc.x][my_loc.y] = -2;
		
		grouped_spawn_rates = new double[mapWidth][mapHeight];
		spawn_rates = myRC.senseCowGrowth();
		clump_idx = 0;
		
		pastr_loc = LocationMessage.decode(myRC.readBroadcast(LOC_CHANNEL)).pastr_loc;
	}

	protected void step() throws GameActionException{
		sense_enemies();
		
		if (myRC.isActive()){
			if (!clumping && !herding){
				clumping = true;
				clump_plan_idx = 0;
				clump_plan = makeClump();
			}
			if (clumping){
				if (clump_plan_idx < clump_plan.length){
					myRC.attackSquareLight(clump_plan[clump_plan_idx]);
					++clump_plan_idx;
				}
				else {
					clumping = false; herding = true;
					herd_path = herd(my_loc, pastr_loc);
					herd_idx = 0;
				}
			}
			if (herding){
				if (herd_idx < herd_path.length){
					if (myRC.canAttackSquare(herd_path[herd_idx])){
						myRC.attackSquareLight(herd_path[herd_idx]);
						++herd_idx;
					}
					else {
						herding = false; clumping = true;
						clump_plan_idx = 0;
					}
				}
				else {
					herding = false; clumping = true;
					clump_plan_idx = 0;
				}
			}
		}
	}
	
	int[] getExtrema(){
		return getExtrema(my_loc);
	}
	
	int[] getExtrema(MapLocation loc){
		int[] result = new int[8];
		Direction dir;
		for (int i=0; i<direction_order_ext.length; ++i){
			dir = direction_order_ext[i];
			while (isGood(loc.add(dir, ++result[i])));
		}
		return result;
	}
	
	boolean isGood(MapLocation loc){
		if (loc.x < 0 || loc.x >= mapWidth || loc.y < 0 || loc.y >= mapHeight){
			return false;
		}
		return navigator.isGood(loc);
	}
	
	protected void sense_enemies() throws GameActionException{
		if (myRC.getHealth() < health){
			Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class, 35, this.enemyTeam);
			myRC.broadcast(PASTR_DISTRESS_CHANNEL, enemies.length);
			health = myRC.getHealth();
		}
	}
	
	MapLocation[] herd(MapLocation source, MapLocation target) throws GameActionException{
		LinkedList<MapLocation> path = navigator.pathFind(source, target);
		LinkedList<MapLocation> result = new LinkedList<MapLocation>();
		
		MapLocation current = source;
		Direction move_dir;
		path.removeFirst();
		while (true){
			if (current.equals(target)){
				break;
			}
			if (path.getFirst().equals(current)){
				path.removeFirst();
			}
			move_dir = navigator.directionTo(current, path.getFirst());
			result.add(current.add(move_dir.opposite()));
			current = current.add(move_dir);
		}
		return result.toArray(new MapLocation[0]);
	}
	
	MapLocation[] makeClump(){
		return makeClump(my_loc);
	}
	
	MapLocation[] makeClump(MapLocation center){
		int[] extrema = getExtrema(center);
		int total = 0;
		for (int length: extrema){ total += length; }
		total -= 8;
		
		MapLocation[] result = new MapLocation[total];
		Direction dir;
		boolean primary_direction;
		for (int i=0, counter=0; i<direction_order_ext.length; ++i){
			dir = direction_order_ext[i];
			primary_direction =  (dir == Direction.NORTH || dir == Direction.EAST || dir == Direction.SOUTH || dir == Direction.WEST);

			if (primary_direction){
				for (int j=extrema[i]; j>1; --j, ++counter){
					if (j>5){
						result[counter] = my_loc.add(dir, j);
					}
					else {
						result[counter] = my_loc.add(dir, 4);
					}
				}
			}
			else {
				for (int j=extrema[i]; j>1; --j, ++counter){
					if (j>4){
						result[counter] = my_loc.add(dir, j);
					}
					else {
						result[counter] = my_loc.add(dir, 3);
					}
				}
			}
		}
		return result;
	}
	
	protected MapLocation[] findClumps(){
		double maxSpawn = 0;
		int x,y;
		LinkedList<MapLocation> result = new LinkedList<MapLocation>();
		MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(my_loc, 150);
		
		for (MapLocation loc: locs){
			if (loc.x >= 3 && loc.x < mapWidth - 3 && loc.y >= 3 && loc.y < mapHeight - 3){
				if (navigator.isGood(loc)){
					for (int a=-3; a<4; ++a){
						x = loc.x + a;
						for (int b=-3; b<4; ++b){
							y = loc.y + b;
							grouped_spawn_rates[loc.x][loc.y] += spawn_rates[x][y];
						}
					}
					if (grouped_spawn_rates[loc.x][loc.y] > maxSpawn){
						result = new LinkedList<MapLocation>();
						maxSpawn = grouped_spawn_rates[loc.x][loc.y];
					}
					if (grouped_spawn_rates[loc.x][loc.y] == maxSpawn){
						result.add(loc);
					}
				}
			}
		}
		
		return result.toArray(new MapLocation[0]);
	}
}
