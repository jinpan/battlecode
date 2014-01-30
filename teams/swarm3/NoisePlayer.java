package swarm3;

import java.util.LinkedList;

import swarm3.BaseRobot.Strategy;
import battlecode.common.*;

public class NoisePlayer extends BaseRobot{
	
	double health;
	double[][] grouped_spawn_rates;
	double[][] spawn_rates;
	Direction[] direction_order = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
	public static Direction[] direction_order_ext = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
			Direction.SOUTH_EAST, Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST};
	MapLocation my_loc;
	
	boolean clumping;
	MapLocation[] clumps;
	MapLocation[] clump_plan;
	int clump_idx, clump_plan_idx;
	
	boolean herding;
	MapLocation[] herd_path;
	int herd_idx;
	MapLocation[] pastr_locs;
	Navigation navigator;
	
	int noise_order = 0;
	int ATTACK_CHANNEL;
	

	public NoisePlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		
		RobotInfo info;
		for (Robot robot: myRC.senseNearbyGameObjects(Robot.class, 20000, my_team)){
			info = myRC.senseRobotInfo(robot);
			if (info.type == RobotType.NOISETOWER){
				++noise_order;
			}
		}
		if (strategy == null){
			int strategy_ord = myRC.readBroadcast(STRATEGY_CHANNEL);
			if (strategy_ord != 0){
				strategy = Strategy.values()[strategy_ord - 1];
			}
		}
		
		ATTACK_CHANNEL = NOISE_ATTACK_CHANNELS[noise_order];
		
		health = myRC.getHealth();
		my_loc = myRC.getLocation();
		navigator = new Navigation(myRC);
		
		spawn_rates = myRC.senseCowGrowth();
		clumps = new MapLocation[3];
		clumps[0] = new MapLocation(0, 0);
		clumps[1] = new MapLocation(0, map_height - 1);
		clumps[2] = myRC.getLocation();
		clump_idx = myRC.getRobot().getID() % clumps.length;
		
		switch (strategy){
		case COWVERT: {
			pastr_locs = new MapLocation[2];
			LocationMessage locMsg = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNELS[0]));
			if (locMsg != null){
				pastr_locs[0] = locMsg.pastr_loc;
			}
			LocationMessage locMsg1 = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNELS[1]));
			if (locMsg1 != null){
				pastr_locs[1] = locMsg1.pastr_loc;
			}
			break;
		}
		case FARMVILLE: {
			break;
		}
		default: {
			
		}
		}
	}

	protected void step() throws GameActionException{
		sense_enemies();
		
		if (myRC.isActive()){
			if (!clumping && !herding){
				clumping = true;
				clump_plan_idx = 0;
				clump_plan = makeClump(clumps[clump_idx]);
			}
			if (clumping){
				if (clump_plan_idx < clump_plan.length){
					while (!myRC.canAttackSquare(clump_plan[clump_plan_idx])){
						++clump_plan_idx;
					}
					if (attackLoc(clump_plan[clump_plan_idx])){
						++clump_plan_idx;
					}
				}
				else {
					clumping = false; herding = true;
					herd_path = herd(clumps[clump_idx], pastr_locs);
					herd_idx = 0;
					clump_idx = (clump_idx + 1) % clumps.length;
				}
			}
			if (herding){
				if (herd_idx < herd_path.length){
					if (myRC.canAttackSquare(herd_path[herd_idx])){
						if (attackLoc(herd_path[herd_idx])){
							++herd_idx;
						}
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
	
	boolean attackLoc(MapLocation loc) throws GameActionException{
		ActionMessage msg;
		for (int i=0; i<noise_order; ++i){
			msg = ActionMessage.decode(myRC.readBroadcast(NOISE_ATTACK_CHANNELS[i]));
			if (loc.distanceSquaredTo(msg.targetLocation) <= GameConstants.NOISE_SCARE_RANGE_LARGE){
				for (MapLocation loc2: myRC.sensePastrLocations(enemy_team)){
					if (myRC.canAttackSquare(loc2)){
						myRC.attackSquare(loc2);
						return false;
					}
				}
				return false;
			}
		}
		myRC.attackSquareLight(loc);
		myRC.broadcast(ATTACK_CHANNEL, new ActionMessage(State.ATTACK, 0, loc).encode());
		return true;
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
		if (loc.x < 0 || loc.x >= map_width || loc.y < 0 || loc.y >= map_height){
			return false;
		}
		return navigator.isGood(loc);
	}
	
	protected void sense_enemies() throws GameActionException{
		if (myRC.getHealth() < health){
			Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class, 35, this.enemy_team);
			myRC.broadcast(PASTR_DISTRESS_CHANNEL, enemies.length);
			health = myRC.getHealth();
		}
	}
	
	MapLocation[] herd(MapLocation source, MapLocation[] targets) throws GameActionException{
		MapLocation[] candidates = new MapLocation[9 * targets.length];
		MapLocation target;
		for (int i=0, j=0; i<targets.length; ++i){
			target = targets[i];
			
			candidates[j++] = target;
			candidates[j++] = new MapLocation(target.x-2, target.y-1);
			candidates[j++] = new MapLocation(target.x-2, target.y+1);
			candidates[j++] = new MapLocation(target.x-1, target.y+2);
			candidates[j++] = new MapLocation(target.x+1, target.y+2);
			candidates[j++] = new MapLocation(target.x+2, target.y+1);
			candidates[j++] = new MapLocation(target.x+2, target.y-1);
			candidates[j++] = new MapLocation(target.x+1, target.y-2);
			candidates[j++] = new MapLocation(target.x-1, target.y-2);
		};
		MapLocation best_target = targets[0];
		LinkedList<MapLocation> best_path = navigator.findPath(source, targets[0]);
		int best_distance = navigator.pathDist(best_path);
		int distance;
		LinkedList<MapLocation> path;
		
		for (MapLocation candidate: candidates){
			if (isGood(candidate)){
				path = navigator.findPath(source, candidate);
				distance = navigator.pathDist(path);
				if (distance < best_distance){
					best_path = path;
					best_distance = distance;
					best_target = candidate;
				}
			}
		}
		
		System.out.println("BEST LOCATION " + best_target);

		LinkedList<MapLocation> result = new LinkedList<MapLocation>();
		
		MapLocation current = source;
		Direction move_dir;
		best_path.removeFirst();
		while (true){
			if (current.equals(best_target)){
				break;
			}
			if (best_path.getFirst().equals(current)){
				best_path.removeFirst();
			}
			move_dir = navigator.directionTo(current, best_path.getFirst());
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
						result[counter] = center.add(dir, j);
					}
					else {
						result[counter] = center.add(dir, 4);
					}
				}
			}
			else {
				for (int j=extrema[i]; j>1; --j, ++counter){
					if (j>4){
						result[counter] = center.add(dir, j);
					}
					else {
						result[counter] = center.add(dir, 3);
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
			if (loc.x >= 3 && loc.x < map_width - 3 && loc.y >= 3 && loc.y < map_height - 3){
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
