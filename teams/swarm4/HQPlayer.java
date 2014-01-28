package swarm4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	int strategy = 4;

	double enemyPrevMilk = 0; 
	double myPrevMilk = 0;
	
	Navigation navigator;

	boolean attack = false; 
	
	boolean offensive;
	int attackDist;

	double totalSpawn;
	double[][] spawnRates;
	double[][] groupSpawnRates;

	Direction toEnemy;
	int distToEnemy;
	MapLocation defaultSpawnLoc;

	int pastrCount = 0;
	boolean pastrBuilt = false;
	int defeatCount = 0; //how many times we made a pastr and it got blown up

	MapLocation noise_loc, pastr_loc, spawn_loc;
	Direction noiseDir = null;

	int numRobots;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		spawn();

		navigator = new Navigation(myRC);

		this.spawnRates = this.myRC.senseCowGrowth();
		this.groupSpawnRates = new double[this.mapWidth][this.mapHeight];

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.distToEnemy = this.myHQLoc.distanceSquaredTo(this.enemyHQLoc);
		this.numRobots = 1;

		if (distToEnemy> 30*30){
			offensive = false;
		} else {
			offensive = true;
		}
		
		spawn_loc = findBestSpawnLoc();
		defaultSpawnLoc = myHQLoc.add(myHQLoc.directionTo(spawn_loc));
		
		setLocs();
		//change one of these to let us play against ourselves
		if(this.myTeam == Team.A)
			strategy = 4;
		else
			strategy = 3;
	}
	
	protected void setLocs() throws GameActionException {
		System.out.println("SPAWN_LOC " + spawn_loc);
		LinkedList<MapLocation> path = navigator.pathFind(spawn_loc, myHQLoc);
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
	
	
	@Override
	protected void step() throws GameActionException {

		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}

		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
			++this.numRobots;
		}

		int dist = maxDist;
		MapLocation closestTarget = null;

		//finds closest enemy pasture
		MapLocation[] targets = this.myRC.sensePastrLocations(this.enemyTeam);
		for (int i=0; i<targets.length; ++i){
			if (targets[i].distanceSquaredTo(this.enemyHQLoc) > 15){
				if ((targets[i].distanceSquaredTo(this.myHQLoc) < dist)){
					closestTarget = targets[i];
					dist = targets[i].distanceSquaredTo(this.myHQLoc);
				}
			}
		}

		MapLocation[] pastrs = this.myRC.sensePastrLocations(myTeam);
		pastrCount = pastrs.length;
		pastrBuilt = (pastrCount > 0);

		if(pastrBuilt && pastrCount == 0){
			//if we got blown up, might as well make pasture somewhere else
			defeatCount++;
		}

		Robot[] allies = this.myRC.senseNearbyGameObjects(Robot.class, 100000, this.myTeam);
		int totalAllies = allies.length;
		this.myRC.broadcast(ALLY_NUMBERS, totalAllies - pastrCount*2);

		int nearEnemies = this.myRC.readBroadcast(PASTR_DISTRESS_CHANNEL);

		ActionMessage action = null;

		if(strategy == 1){
			//the original strategy.
			//don't leave pastures alone if they're built and have incoming threats
			//if we don't have a pasture, attack if we have a big enough squad
			if(defeatCount >=3)
				strategy = 2;
			
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
		}

		if(strategy == 2){
			//the extreme defensive strategy; make pasture right next to our HQ
			//make a noisetower in some random place and have it attack a pasture
			//when group is big enough, go attack
			
			MapLocation pastr = this.myHQLoc.add(toEnemy, 2);
//			if (!isGoodLoc(pastr)) {
//				pastr = this.myHQLoc.add(toEnemy, 2);
//			} if (!isGoodLoc(pastr)) {
//				pastr = this.myHQLoc.add(toEnemy);
//			}
			
			setLocs();
						
			attack = false;
			
			if(totalAllies > 6 && closestTarget != null)
				attack = true;
			if(attack && totalAllies < 3)
				attack = false;
			
			if(attack)
				action = new ActionMessage(BaseRobot.State.ATTACK, 2, closestTarget);
			else {
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastr_loc);
				//System.out.println("Strategy 2 broadcasted " + pastrLoc);
			}
			if (action!= null) {
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
			}
		}
		
		if(strategy == 3){
			
			if (defeatCount>=3){
				strategy = 2;
			}
			
			double enemyMilk = this.myRC.senseTeamMilkQuantity(enemyTeam);
			double myMilk = this.myRC.senseTeamMilkQuantity(myTeam);
			double enemyMilkChange = enemyMilk - enemyPrevMilk;
			double myMilkChange = myMilk- myPrevMilk;
			double myExpectedWin = (GameConstants.WIN_QTY - myMilk)/myMilkChange;
			double enemyExpectedWin = (GameConstants.WIN_QTY - enemyMilk)/enemyMilkChange;
			boolean losing = (enemyMilk - myMilk) > 10000;
			
			myPrevMilk = myMilk;
			enemyPrevMilk = enemyMilk;
			
			attack = false;
			if(pastrBuilt){
				if(closestTarget != null && nearEnemies == 0 && losing)
					attack = true;
			} else {
				if(closestTarget != null && totalAllies > 8 && losing)
					attack = true;
			}

			if(attack)
				action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
			else
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastr_loc);
			if (action!= null) {
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
			}
		}
		
		if(strategy == 4){
			// modification of original strategy - don't necessarily attack enemy pastr
			if (defeatCount >=3)
				strategy = 2;
			
			attack = false;
			if (targets.length > 1){
				if(pastrBuilt){
					if(closestTarget != null && nearEnemies == 0)
						attack = true;
				} else {
					if(closestTarget != null && totalAllies > 8)
						attack = true;
				}
			}

			if (attack)
				action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
			else
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastr_loc);
			if (action!= null) {
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
			}
		}
		
		int caution = this.myRC.readBroadcast(CAUTION_CHANNEL);
		if (Clock.getRoundNum() > caution + 2){
			this.myRC.broadcast(CAUTION_CHANNEL, 0);
		}
		
		
	}

	private boolean spawn() throws GameActionException {
		if (this.defaultSpawnLoc != null && this.myRC.senseObjectAtLocation(this.defaultSpawnLoc) == null
				&& this.myRC.senseTerrainTile(this.defaultSpawnLoc).ordinal() < 2){
			this.myRC.spawn(this.myHQLoc.directionTo(this.defaultSpawnLoc));
			return true;
		}
		else {
			MapLocation candidate;
			for (Direction dir: BaseRobot.dirs){
				candidate = this.myHQLoc.add(dir);
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
			curloc = curloc.add(curloc.directionTo(this.myHQLoc));
			if(this.myRC.canAttackSquare(curloc)){
				this.myRC.attackSquare(curloc);
				return true;
			}
		}

		return false; //give up    	
	}

	private MapLocation reflect(MapLocation loc){
		return new MapLocation(mapWidth - loc.x, mapHeight - loc.y);
	}

	private MapLocation findRandomPastureLoc() {
		// finds a random location.  returns a random location that is at least 2 units
		// away from the edges.
		int x, y;
		
		x = (int) (Math.random() * (this.mapWidth - 4)) + 2;
		y = (int) (Math.random() * (this.mapHeight - 4)) + 2;
		
		return new MapLocation(x, y);
	}
	
	private boolean acceptPastrLoc(MapLocation candidate, int idx){
		if (this.myRC.senseTerrainTile(candidate).ordinal() > 1){
			return false;
		}
		int standard = (idx < 100) ? 3 : (idx < 200) ? 2: 1;
		
		double spawnRate = this.spawnRates[candidate.x][candidate.y];
		
		if (spawnRate >= standard){
			return true;
		}
		return false;
	}
	

	public MapLocation findBestSpawnLoc() throws GameActionException{
		// TODO: make this function repeatable, in the sense that we can efficiently run
		// it subsequently and expect different results.
		
		LinkedList<MapLocation> highSpawnLocs = new LinkedList<MapLocation>();
		double maxSpawn = 0;
		int x, y;
		for (int i=2; i<mapWidth-2; ++i){
			for (int j=2; j<mapHeight-2; ++j){
				totalSpawn += spawnRates[i][j];
				if (spawnRates[i][j] == 0 && myRC.senseTerrainTile(new MapLocation(i, j)) == TerrainTile.VOID){
					continue;
				}
				for (int a=-2; a<3; ++a){
					x = i + a;
					for (int b=-2; b<3; ++b){
						y = j + b;
						groupSpawnRates[i][j] += spawnRates[x][y];
					}
				}
				if (groupSpawnRates[i][j] > maxSpawn){
					highSpawnLocs = new LinkedList<MapLocation>();
					maxSpawn = groupSpawnRates[i][j];
				}
				if (groupSpawnRates[i][j] == maxSpawn){
					highSpawnLocs.add(new MapLocation(i, j));
				}
			}
			if (myRC.isActive()){
				spawn();
			}
		}
		MapLocation result = highSpawnLocs.getFirst();
		
		LinkedList<MapLocation> pivots1 = navigator.pathFind(myHQLoc, result);
		LinkedList<MapLocation> pivots2 = navigator.pathFind(enemyHQLoc, result);
	
		if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
			return result;
		}
		else {
			MapLocation reflection = new MapLocation(mapWidth - 1 - result.x, mapHeight - 1 - result.y);
			if (groupSpawnRates[reflection.x][reflection.y] == maxSpawn){
				pivots1 = navigator.pathFind(myHQLoc, reflection);
				pivots2 = navigator.pathFind(enemyHQLoc, reflection);
				if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
					return reflection;
				}
			}

			MapLocation xFlip = new MapLocation(result.x, mapHeight-result.y);
			if (groupSpawnRates[xFlip.x][xFlip.y] == maxSpawn){
				pivots1 = navigator.pathFind(myHQLoc, xFlip);
				pivots2 = navigator.pathFind(enemyHQLoc, xFlip);
				if (navigator.pathDist(pivots1) <= navigator.pathDist(pivots2)){
					return xFlip;
				}
			}
			MapLocation yFlip = new MapLocation(mapWidth-result.x, result.y);
			if (groupSpawnRates[yFlip.x][yFlip.y] == maxSpawn){
				pivots1 = navigator.pathFind(myHQLoc, yFlip);
				pivots2 = navigator.pathFind(enemyHQLoc, yFlip);
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
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.SOUTH_EAST, 2), i)){
				candidate = candidate.add(Direction.SOUTH_EAST, 2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.NORTH_EAST, 2), i)){
				candidate = candidate.add(Direction.NORTH_EAST, 2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.NORTH_WEST, 2), i)){
				candidate = candidate.add(Direction.NORTH_WEST, 2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (acceptPastrLoc(candidate.add(Direction.SOUTH_WEST, 2), i)){
				candidate = candidate.add(Direction.SOUTH_WEST, 2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
		}

	}

}
