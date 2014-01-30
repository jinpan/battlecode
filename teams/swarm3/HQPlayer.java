package swarm3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {

	static final double FARMVILLE_SPAWN_THRESHOLD = 3000;
	int last_pester_time;
	
	double enemyPrevMilk = 0; 
	double myPrevMilk = 0;
	
	Navigation navigator;

	boolean attack = false; 
	
	boolean offensive;
	int attackDist;

	double totalSpawn;
	double max_spawn = 0;
	double[][] spawn_rates;
	double[][] groupSpawnRates;

	Direction toEnemy;
	int distToEnemy;
	MapLocation defaultSpawnLoc;
	MapLocation rally_location;
	LinkedList<MapLocation> high_spawn_locs;

	int pastrCount = 0;
	boolean pastrBuilt = false;
	int defeatCount = 0; //how many times we made a PASTR and it got blown up

	MapLocation noise_loc, pastr_loc, spawn_loc;
	Direction noiseDir = null;

	int numRobots;
	public static final double EPSILON = 0.001;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		spawn();

		navigator = new Navigation(myRC);

		spawn_rates = this.myRC.senseCowGrowth();
		groupSpawnRates = new double[this.map_width][this.map_height];
		strategy = chooseStrategy();
		System.out.println("STRATEGY: " + strategy);
		myRC.broadcast(STRATEGY_CHANNEL, strategy.ordinal() + 1);
		
		switch (strategy){
			case COWVERT: {
				findBestSpawnLoc();
				setLocsCOWVERT();
				getRallyLocation();
				ActionMessage msg = new ActionMessage(State.DEFEND, 0, rally_location);
				myRC.broadcast(HQ_BROADCAST_CHANNEL, msg.encode());
				break;
			}
			default: {
				spawn_loc = findBestSpawnLoc();
				defaultSpawnLoc = my_hq_loc.add(my_hq_loc.directionTo(spawn_loc));
				
				setLocs();
			}
		}
		
		System.out.println(navigator.findPath(my_hq_loc, new MapLocation(2, 16)));
	}
	
	
	public Strategy chooseStrategy() throws GameActionException {
		// check if a noise tower centered at our HQ can reach > 50% of the map
		// if so, engage in operation COWVERT
		// TODO: fix for the hyperfine map
		int valid_count = 0;
		if (my_hq_loc.x - 17 < 0 || my_hq_loc.x + 17 > map_width || my_hq_loc.y - 17 < 0 || my_hq_loc.y + 17 > map_height){
			MapLocation[] locs = MapLocation.getAllMapLocationsWithinRadiusSq(my_hq_loc, 300);
			for (MapLocation loc: locs){
				if (loc.x >= 0 && loc.x < map_width && loc.y >= 0 && loc.y < map_height){
					++valid_count;
				}
			}
		}
		else {
			valid_count = 949;
		}
		if (valid_count > map_width * map_height / 2){
			return Strategy.COWVERT;
		}
		
		// check if the total spawn rate is more than 3k
		// if so, operation FARMVILLE
		high_spawn_locs = new LinkedList<MapLocation>();
		max_spawn = 0;
		for (int i=2; i<map_width-2; ++i){
			for (int j=2; j<map_height-2; ++j){
				if (spawn_rates[i][j] == 0){
					continue;
				}
				totalSpawn += spawn_rates[i][j];
				if (spawn_rates[i][j] > max_spawn){
					high_spawn_locs = new LinkedList<MapLocation>();
					max_spawn = spawn_rates[i][j];
				}
				if (spawn_rates[i][j] == max_spawn){
					high_spawn_locs.add(new MapLocation(i, j));
				}
			}
			if (myRC.isActive()){
				spawn();
			}
		}
		
		if (totalSpawn > FARMVILLE_SPAWN_THRESHOLD){
			return Strategy.FARMVILLE;
		}
		
		return Strategy.DEFAULT;
	}
	
	protected void setLocs() throws GameActionException {
		System.out.println("SPAWN_LOC " + spawn_loc);
		LinkedList<MapLocation> path = navigator.findPath(spawn_loc, my_hq_loc);
		noise_loc = spawn_loc;
		pastr_loc = navigator.getLoc(path, 20);
		if (navigator.isGood(pastr_loc.add(pastr_loc.directionTo(noise_loc)))){
			pastr_loc = pastr_loc.add(pastr_loc.directionTo(noise_loc));
		}
		else {
			for (Direction dir: dirs){
				if (navigator.isGood(pastr_loc.add(dir))){
					pastr_loc = pastr_loc.add(dir);
					break;
				}
			}
		}
		System.out.println("NOISE_LOC: " + noise_loc + "; PASTR_LOC: " + pastr_loc);
		
		LocationMessage locMsg = new LocationMessage(noise_loc, pastr_loc);
		this.myRC.broadcast(LOC_CHANNEL, locMsg.encode());
	}
	
	void setLocsCOWVERT() throws GameActionException{
		// TODO: broken for maps where the HQ is boxed in
		// TODO: VERY BROKEN FOR BOXED IN MAPS
		MapLocation pastr1 = my_hq_loc.add(Direction.NORTH_EAST);
		MapLocation pastr2 = my_hq_loc.add(Direction.SOUTH_WEST);
		MapLocation noise1 = my_hq_loc.add(Direction.NORTH_WEST);
		MapLocation noise2 = my_hq_loc.add(Direction.SOUTH_EAST);
		
		System.out.println(pastr1 + " " + pastr2 + " " + noise1 + " " + noise2);
		LocationMessage locMsg0 = new LocationMessage(noise1, pastr1);
		LocationMessage locMsg1 = new LocationMessage(noise2, pastr2);
		myRC.broadcast(LOC_CHANNELS[0], locMsg0.encode());
		myRC.broadcast(LOC_CHANNELS[1], locMsg1.encode());
	}
	
	void getRallyLocation() throws GameActionException {
		// computes a location for robots to wait at before further action
		Direction dir = my_hq_loc.directionTo(enemy_hq_loc);
		rally_location = my_hq_loc.add(dir, 2);
	}
	
	protected void step() throws GameActionException {
		if (this.myRC.senseRobotCount()< 1) {
			this.spawn();
			++this.numRobots;
		} else {
			return;
		}
	}
	
	/*@Override
	protected void step() throws GameActionException {

		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemy_team);
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}

		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
			++this.numRobots;
		}
		
		switch (strategy){
			case COWVERT: {
				MapLocation[] enemy_pastr_locs = myRC.sensePastrLocations(enemy_team);
				if (myRC.readBroadcast(COWVERT_OPERATIONS_CHANNEL) == 1){
					// self pester successful
					last_pester_time = Clock.getRoundNum();
					myRC.broadcast(COWVERT_OPERATIONS_CHANNEL, 0);
					ActionMessage msg = new ActionMessage(State.DEFEND, 0, rally_location);
					myRC.broadcast(HQ_BROADCAST_CHANNEL, msg.encode());
				}
				else if (myRC.readBroadcast(COWVERT_OPERATIONS_CHANNEL) == -1){
					// pester instruction given
				}
				else if (Clock.getRoundNum() > last_pester_time + 100){
					// it's been 100 turns since we last pestered
					if (enemy_pastr_locs.length == 1){
						ActionMessage msg = new ActionMessage(State.ATTACK, 1, enemy_pastr_locs[0]);
						myRC.broadcast(HQ_BROADCAST_CHANNEL, msg.encode());
					}
				}
				if (enemy_pastr_locs.length > 1){
					// destroy them
					LinkedList<MapLocation> path = navigator.findPath(rally_location, enemy_pastr_locs[0]);
					int min_distance = navigator.pathDist(path), dist;
					MapLocation best_target = enemy_pastr_locs[0];
					
					for (int i=1; i<enemy_pastr_locs.length; ++i){
						path = navigator.findPath(rally_location, enemy_pastr_locs[i]);
						dist = navigator.pathDist(path);
						if (dist < min_distance){
							best_target = enemy_pastr_locs[i];
							min_distance = dist;
						}
					}
					ActionMessage msg = ActionMessage.decode(myRC.readBroadcast(HQ_BROADCAST_CHANNEL));
					if (msg.state != State.ATTACK || msg.targetID != 0 || !msg.targetLocation.equals(best_target)){
						msg = new ActionMessage(State.ATTACK, 0, best_target);
						myRC.broadcast(HQ_BROADCAST_CHANNEL, msg.encode());
					}
				}
				break;
			}
			case FARMVILLE: {
				break;
			}
			default: {
				int dist = max_dist;
				MapLocation closestTarget = null;

				//finds closest enemy pasture
				MapLocation[] targets = this.myRC.sensePastrLocations(this.enemy_team);
				for (int i=0; i<targets.length; ++i){
					if (targets[i].distanceSquaredTo(this.enemy_hq_loc) > 15){
						if ((targets[i].distanceSquaredTo(this.my_hq_loc) < dist)){
							closestTarget = targets[i];
							dist = targets[i].distanceSquaredTo(this.my_hq_loc);
						}
					}
				}

				MapLocation[] pastrs = this.myRC.sensePastrLocations(my_team);
				pastrCount = pastrs.length;
				pastrBuilt = (pastrCount > 0);

				if(pastrBuilt && pastrCount == 0){
					//if we got blown up, might as well make pasture somewhere else
					defeatCount++;
				}

				Robot[] allies = this.myRC.senseNearbyGameObjects(Robot.class, 100000, this.my_team);
				int totalAllies = allies.length;
				this.myRC.broadcast(ALLY_NUMBERS, totalAllies - pastrCount*2);

				int nearEnemies = this.myRC.readBroadcast(PASTR_DISTRESS_CHANNEL);

				ActionMessage action = null;

				//the original strategy.
				//don't leave pastures alone if they're built and have incoming threats
				//if we don't have a pasture, attack if we have a big enough squad
				
				attack = false;
				if(pastrBuilt){
					if(closestTarget != null && nearEnemies == 0)
						attack = true;
				} else {
					if(closestTarget != null && totalAllies > 8)
						attack = true;
				}

				if(attack)
					action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
				else
					action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastr_loc);
				if (action!= null) {
					this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
				}
				
				int caution = this.myRC.readBroadcast(CAUTION_CHANNEL);
				if (Clock.getRoundNum() > caution + 2){
					this.myRC.broadcast(CAUTION_CHANNEL, 0);
				}
			}
		}
	}
*/
	private boolean spawn() throws GameActionException {
		if (this.defaultSpawnLoc != null && this.myRC.senseObjectAtLocation(this.defaultSpawnLoc) == null
				&& this.myRC.senseTerrainTile(this.defaultSpawnLoc).ordinal() < 2){
			this.myRC.spawn(this.my_hq_loc.directionTo(this.defaultSpawnLoc));
			return true;
		}
		else {
			MapLocation candidate;
			for (Direction dir: BaseRobot.dirs){
				candidate = this.my_hq_loc.add(dir);
				if (dir != this.toEnemy
						&& this.myRC.senseObjectAtLocation(candidate) == null
						&& this.myRC.senseTerrainTile(candidate).ordinal() < 2){
					this.myRC.spawn(dir);
					return true;
				}
			}
		}
		return false;
	}

	private boolean shoot(Robot[] nearbyEnemies) throws GameActionException{
		MapLocation curloc;

		for(Robot r : nearbyEnemies){ //try to hit something directly
			curloc = this.myRC.senseRobotInfo(r).location;
			if(this.myRC.canAttackSquare(curloc)){
				this.myRC.attackSquare(curloc);
				return true;
			}
		}

		for(Robot r : nearbyEnemies){ //try to hit something for splash damage
			curloc = this.myRC.senseRobotInfo(r).location;
			curloc = curloc.add(curloc.directionTo(this.my_hq_loc));
			if(this.myRC.canAttackSquare(curloc)){
				this.myRC.attackSquare(curloc);
				return true;
			}
		}

		return false; //give up    	
	}

	private MapLocation reflect(MapLocation loc){
		return new MapLocation(map_width - loc.x - 1, map_height - loc.y - 1);
	}

	private MapLocation findRandomPastureLoc() {
		// finds a random location.  returns a random location that is at least 2 units
		// away from the edges.
		int x, y;
		
		x = (int) (Math.random() * (this.map_width - 4)) + 2;
		y = (int) (Math.random() * (this.map_height - 4)) + 2;
		
		return new MapLocation(x, y);
	}
	
	private boolean acceptPastrLoc(MapLocation candidate, int idx){
		if (this.myRC.senseTerrainTile(candidate).ordinal() > 1){
			return false;
		}
		int standard = (idx < 100) ? 3 : (idx < 200) ? 2: 1;
		
		double spawnRate = this.spawn_rates[candidate.x][candidate.y];
		
		if (spawnRate >= standard){
			return true;
		}
		return false;
	}

	public MapLocation findBestSpawnLoc() throws GameActionException{
		// TODO: make this function repeatable, in the sense that we can efficiently run
		// it subsequently and expect different results.
		
		if (high_spawn_locs == null){
			// this loop should only be called in the case of small maps and will be fast
			high_spawn_locs = new LinkedList<MapLocation>();
			for (int i=0; i<map_width; ++i){
				for (int j=0; j<map_height; ++j){
					if (spawn_rates[i][j] > max_spawn){
						high_spawn_locs = new LinkedList<MapLocation>();
						max_spawn = spawn_rates[i][j];
					}
					if (spawn_rates[i][j] == max_spawn){
						high_spawn_locs.add(new MapLocation(i, j));
					}
				}
			}
		}
		
		LinkedList<MapLocation> new_high_spawn_locs = null;
		MapLocation candidate;
		while (true){
			for (MapLocation loc: high_spawn_locs){
				spawn_rates[loc.x][loc.y] += EPSILON;
			}
			max_spawn += EPSILON;
			
			int high_spawn_neighbors;
			new_high_spawn_locs  = new LinkedList<MapLocation>();
			for (MapLocation loc: high_spawn_locs){
				high_spawn_neighbors = 0;
				for (Direction dir: dirs){
					candidate = loc.add(dir);
					if (candidate.x < 0 || candidate.x >= map_width || candidate.y < 0 || candidate.y >= map_height){
						continue;
					}
					if (spawn_rates[candidate.x][candidate.y] >= max_spawn){
						++high_spawn_neighbors;
					}
				}
				if (high_spawn_neighbors > 4){
					new_high_spawn_locs.add(loc);
				}
			}
			
			if (new_high_spawn_locs.isEmpty()){
				break;
			}
			else {
				System.out.println("GOOD SITES: " + high_spawn_locs.size());
				high_spawn_locs = new_high_spawn_locs;
			}
		}
		
		System.out.println("GOOD SITES: " + high_spawn_locs.size());
		
		MapLocation result = high_spawn_locs.getFirst();
		
		LinkedList<MapLocation> pivots1 = navigator.findPath(my_hq_loc, result);
		LinkedList<MapLocation> pivots2 = navigator.findPath(enemy_hq_loc, result);
	
		if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
			return result;
		}
		else {
			MapLocation reflection = new MapLocation(map_width - 1 - result.x, map_height - 1 - result.y);
			if (groupSpawnRates[reflection.x][reflection.y] == max_spawn){
				pivots1 = navigator.findPath(my_hq_loc, reflection);
				pivots2 = navigator.findPath(enemy_hq_loc, reflection);
				if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
					return reflection;
				}
			}

			MapLocation xFlip = new MapLocation(result.x, map_height-result.y);
			if (groupSpawnRates[xFlip.x][xFlip.y] == max_spawn){
				pivots1 = navigator.findPath(my_hq_loc, xFlip);
				pivots2 = navigator.findPath(enemy_hq_loc, xFlip);
				if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
					return xFlip;
				}
			}
			MapLocation yFlip = new MapLocation(map_width-result.x, result.y);
			if (groupSpawnRates[yFlip.x][yFlip.y] == max_spawn){
				pivots1 = navigator.findPath(my_hq_loc, yFlip);
				pivots2 = navigator.findPath(enemy_hq_loc, yFlip);
				if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
					return yFlip;
				}
			}
		}
		
		throw new GameActionException(null, "Could not find a spawn site");
	}

	public MapLocation findBestPastureLoc(){
		MapLocation candidate;

		for (int i=0; ; ++i){
			candidate = findRandomPastureLoc();

			if (acceptPastrLoc(candidate, i)){
				// high spawn rate and not a void square
				if (candidate.distanceSquaredTo(this.enemy_hq_loc) > candidate.distanceSquaredTo(this.my_hq_loc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.SOUTH_EAST, 2), i)){
				candidate = candidate.add(Direction.SOUTH_EAST, 2);
				if (candidate.distanceSquaredTo(this.enemy_hq_loc) > candidate.distanceSquaredTo(this.my_hq_loc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.NORTH_EAST, 2), i)){
				candidate = candidate.add(Direction.NORTH_EAST, 2);
				if (candidate.distanceSquaredTo(this.enemy_hq_loc) > candidate.distanceSquaredTo(this.my_hq_loc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.NORTH_WEST, 2), i)){
				candidate = candidate.add(Direction.NORTH_WEST, 2);
				if (candidate.distanceSquaredTo(this.enemy_hq_loc) > candidate.distanceSquaredTo(this.my_hq_loc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.SOUTH_WEST, 2), i)){
				candidate = candidate.add(Direction.SOUTH_WEST, 2);
				if (candidate.distanceSquaredTo(this.enemy_hq_loc) > candidate.distanceSquaredTo(this.my_hq_loc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
		}

	}

}
