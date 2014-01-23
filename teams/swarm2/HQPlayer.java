package swarm2;

import java.util.ArrayList;
import java.util.LinkedList;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {

	int maxPastures;
	int attackDist;
	double[][] spawnRates;
	int mapHeight; int mapWidth;

	Direction toEnemy;
	int distToEnemy;
	MapLocation defaultSpawnLoc;

	int pastrCount = 0;

	MapLocation pastrLoc;
	LinkedList<MapLocation> pathToEnemy = new LinkedList<MapLocation>();
	Direction noiseDir = null;

	boolean offensive = true; //adjust this smart

	int numRobots;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.spawnRates = this.myRC.senseCowGrowth();
		this.mapHeight = this.myRC.getMapHeight();
		this.mapWidth = this.myRC.getMapWidth();

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.distToEnemy = this.myHQLoc.distanceSquaredTo(this.enemyHQLoc);
		
		if (distToEnemy>50*50){
			offensive = false;
		}
		
		if (offensive) {
			maxPastures = 1;
			attackDist = maxDist;
		} else {
			maxPastures = 2;
			attackDist = maxDist/2;
		}
		
		this.numRobots = 1;

		// Randomly try to find a pastr location.  If we find one that does not have a
		// location for a noise tower, we retry.
		findPastr: {
			pastrLoc = this.findBestPastureLoc();
			for (Direction dir: BaseRobot.dirs){
				if (this.myRC.senseTerrainTile(pastrLoc.add(dir)).ordinal() < 2){
					this.noiseDir = dir;
					break;
				}
			}
			if (noiseDir == null){
				break findPastr;
			}
		}
		this.pathToEnemy = Navigation.pathFind(pastrLoc, enemyHQLoc, this);
		this.defaultSpawnLoc = this.myHQLoc.add(this.myHQLoc.directionTo(this.pastrLoc));
		
        ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
        this.myRC.broadcast(PASTR_LOC_CHANNEL, (int)action.encode());
        ActionMessage action2 = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc.add(pastrLoc.directionTo(this.enemyHQLoc)));
        this.myRC.broadcast(NOISE_LOC_CHANNEL, (int)action2.encode());
	}

	@Override
	protected void step() throws GameActionException {
		pastrCount = this.myRC.sensePastrLocations(myTeam).length;
		
		if (Clock.getRoundNum()%5==0){
			this.myRC.broadcast(SOLDIER_COM_CHANNEL, 0);
			this.myRC.broadcast(SOLDIER_INPUTS, 0);
			this.myRC.broadcast(ALLY_NUMBERS, 0);
		}

		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}    	

		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
			++this.numRobots;
		}

		int dist = attackDist;
		MapLocation closestTarget = null;

		//finds closest enemy pasture
		MapLocation[] targets = this.myRC.sensePastrLocations(this.enemyTeam);
		for (int i=0; i<targets.length; ++i){
			if (targets[i].distanceSquaredTo(this.enemyHQLoc) > 15){
				if ((targets[i].distanceSquaredTo(this.myHQLoc) < dist)){
					closestTarget = targets[i];
				}
			}
		}
		
		Robot[] allies = this.myRC.senseNearbyGameObjects(Robot.class, 100000, this.myTeam);
		int totalAllies = allies.length;
		this.myRC.broadcast(ALLY_NUMBERS, totalAllies - pastrCount*2);

		boolean pastrBuilt = (pastrCount >= maxPastures);
		int nearEnemies = this.myRC.readBroadcast(PASTR_DISTRESS_CHANNEL);
		
		MapLocation rallypoint = this.pathToEnemy.get(3);
		
		/*MapLocation regroup = this.myHQLoc.add(toEnemy, 5);
		Robot[] liveBots = this.myRC.senseBroadcastingRobots(this.myTeam);
		if (Clock.getRoundNum()>80 && liveBots.length < 5) {
			ActionMessage action = new ActionMessage(BaseRobot.State.DEFAULT, 0, regroup);
			this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
			return;
		}*/

		if(pastrBuilt){
			//if we already built a pasture, don't leave it if enemies nearby
			if(closestTarget != null && nearEnemies == 0){
				ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());
			} else {
				ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, rallypoint);
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());            
			}
		} else {
			if(closestTarget != null && totalAllies > 8){
				ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());
			} else {
				ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());
			}
		}
	}

	private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.defaultSpawnLoc) == null
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
		int midX2 = (this.myHQLoc.x + this.enemyHQLoc.x), midY2 = (this.myHQLoc.y + this.enemyHQLoc.y);
		return new MapLocation(midX2 - loc.x, midY2 - loc.y);
	}

	private MapLocation findRandomPastureLoc(int standard){
		// finds a random location.  returns a random location that is at least 2 units
		// away from the edges.
		int x, y;
		x = (int) (this.random() * (this.mapWidth - 4)) + 2;
		y = (int) (this.random() * (this.mapHeight - 4)) + 2;

		if (standard >= 2){
			if (this.myHQLoc.x <= this.enemyHQLoc.x){
				x = x/3 + 2;
			}
			else {
				x = this.mapWidth - x/3 - 3;
			}
			if (this.myHQLoc.y <= this.enemyHQLoc.y){
				y = y/3 + 2;
			}
			else {
				y = this.mapHeight - y/3 - 3;
			}
		}

		return new MapLocation(x, y);
	}

	public MapLocation findBestPastureLoc(){
		MapLocation candidate;

		for (int i=0; ; ++i){
			int standard = 3 - i/10;
			candidate = findRandomPastureLoc(standard);
			System.out.println(standard);

			if (this.spawnRates[candidate.x][candidate.y] >= standard && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				// high spawn rate and not a void square
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}

			else if (this.spawnRates[candidate.x+2][candidate.y+2] >= standard && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x+2, candidate.y+2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x+2][candidate.y-2] >= standard && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x+2, candidate.y-2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x-2][candidate.y-2] >= standard && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x-2, candidate.y-2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x-2][candidate.y+2] >= standard && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x-2, candidate.y+2);
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
