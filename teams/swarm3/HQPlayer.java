package swarm3;

import java.util.LinkedList;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	int strategy = 1; 

	double enemyPrevMilk = 0; 
	double myPrevMilk = 0;

	boolean attack = false; 
	
	boolean offensive;
	int attackDist;

	double[][] spawnRates;
	double[][] groupSpawnRates;
	int mapWidth; int mapHeight;

	Direction toEnemy;
	int distToEnemy;
	MapLocation defaultSpawnLoc;

	int pastrCount = 0;
	boolean pastrBuilt = false;
	int defeatCount = 0; //how many times we made a pastr and it got blown up

	MapLocation pastrLoc;
	Direction noiseDir = null;

	int numRobots;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.mapWidth = this.myRC.getMapWidth();
		this.mapHeight = this.myRC.getMapHeight();
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

		//System.out.println(offensive);

		set_pastr_loc(find_pastr_loc());
		findBestPastureLoc2();

		//change one of these to let us play against ourselves
		if(this.myTeam == Team.A)
			strategy = 1;
		else
			strategy = 3;
	}

	protected MapLocation find_pastr_loc() throws GameActionException{
		// Randomly try to find a pastr location.  If we find one that does not have a
		// location for a noise tower, we retry.
		MapLocation loc;

		findPastr: {
			loc = this.findBestPastureLoc();
			for (Direction dir: BaseRobot.dirs){
				if (this.myRC.senseTerrainTile(loc.add(dir)).ordinal() < 2){
					this.noiseDir = dir;
					break;
				}
			}
			if (noiseDir == null){
				break findPastr;
			}
		}
		
		return loc;
	}

	protected void set_pastr_loc(MapLocation loc) throws GameActionException{
		System.out.println(loc);
		pastrLoc = loc;

		this.defaultSpawnLoc = this.myHQLoc.add(this.myHQLoc.directionTo(this.pastrLoc));

		ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
		this.myRC.broadcast(PASTR_LOC_CHANNEL, (int)action.encode());
		ActionMessage action2 = null;
		for (Direction dir: dirs){
			if (this.myRC.senseTerrainTile(pastrLoc.add(dir)).ordinal() < 2){
				action2 = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc.add(dir));
				break;
			}
		}
		this.myRC.broadcast(NOISE_LOC_CHANNEL, (int)action2.encode());
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
			set_pastr_loc(find_pastr_loc());
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
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
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
			
			set_pastr_loc(pastr);
						
			attack = false;
			
			if(totalAllies > 6 && closestTarget != null)
				attack = true;
			if(attack && totalAllies < 3)
				attack = false;
			
			if(attack)
				action = new ActionMessage(BaseRobot.State.ATTACK, 2, closestTarget);
			else {
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
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
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
			if (action!= null) {
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
			}
		}
		
		if(strategy == 4){
			// modification of original strategy - don't necessarily attack enemy pastr
			if (defeatCount >=3)
				strategy = 2;
			
			attack = false;
			if(pastrBuilt && targets.length > 1){
				if(closestTarget != null && nearEnemies == 0)
					attack = true;
			} else {
				if(closestTarget != null && totalAllies > 8)
					attack = true;
			}

			if (attack)
				action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
			else
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
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
	
	
	private double distToMidline(MapLocation loc){
		int x1 = (this.myHQLoc.x + this.enemyHQLoc.x) / 2, y1 = (this.myHQLoc.y + this.enemyHQLoc.y)/ 2;
		int x2 = this.myHQLoc.y - y1, y2 = this.myHQLoc.x - x1;
		
		int num = abs((x2 - x1) * (y1 - loc.y) - (x1 - loc.x)* (y2 - y1));
		double denom = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		
		return num / denom;
	}
	

	private MapLocation findRandomPastureLoc() {
		// finds a random location.  returns a random location that is at least 2 units
		// away from the edges.
		int x, y;
		
		x = (int) (this.random() * (this.mapWidth - 4)) + 2;
		y = (int) (this.random() * (this.mapHeight - 4)) + 2;
		
		return new MapLocation(x, y);
	}
	
	private boolean acceptPastrLoc(MapLocation candidate, int idx){
		if (this.myRC.senseTerrainTile(candidate).ordinal() > 1){
			return false;
		}
		int standard = (idx < 100) ? 3 : (idx < 200) ? 2: 1;
		
		double spawnRate = this.spawnRates[candidate.x][candidate.y];
		double distToMid = distToMidline(candidate);
		
		if (spawnRate >= standard && distToMid > standard * (this.mapHeight + this.mapWidth) / 30){
			return true;
		}
		return false;
	}
	

	public MapLocation findBestPastureLoc2(){
		
		double totalSpawn = 0;
		int maxSpawn = 0;
		for (int i=0; i<mapWidth; ++i){
			for (int j=0; j<=mapHeight/2; ++j){
				totalSpawn += spawnRates[i][j];
				maxSpawn = max(maxSpawn, (int) spawnRates[i][j]);
			}
		}
		
		System.out.println("TOTAL SPAWN " + totalSpawn);
		
		LinkedList<MapLocation> candidatesUL = new LinkedList<MapLocation>();
		LinkedList<MapLocation> candidatesBR = new LinkedList<MapLocation>();
		
		for (int i=1; i<mapWidth-1; ++i){
			for (int j=1; j<mapHeight-1; ++j){
				if (spawnRates[i][j] >= maxSpawn){
					if (spawnRates[i-1][j] < maxSpawn && spawnRates[i][j-1] < maxSpawn){
						candidatesUL.add(new MapLocation(i, j));
					}
					if (spawnRates[i+1][j] < maxSpawn && spawnRates[i][j+1] < maxSpawn){
						candidatesBR.add(new MapLocation(i, j));
					}
				}
			}
		}
		System.out.println("SIZE " + candidatesUL.size() + " " + candidatesBR.size());
		return null;
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
