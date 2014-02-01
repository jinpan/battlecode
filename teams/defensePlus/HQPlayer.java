package defensePlus;

import java.util.ArrayList;
import java.util.LinkedList;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	int strategy;
	State state = State.WAIT;
	boolean blewUpAnEnemyPastr = false;
	int[][] map;

	double enemyPrevMilk = 0; 
	double myPrevMilk = 0;

	boolean attack = false; 

	boolean offensive;
	int attackDist;

	double[][] spawnRates;
	double[][] groupSpawnRates;
	int mapWidth; int mapHeight;
	double bestGrowth = 0;
	double nextBestGrowth = 0; double numBest = 0;
	MapLocation defaultSpawnLoc; 
	double threshRatio;

	Direction toEnemy;
	int distToEnemy;

	int openDirs = 0;
	int pastrCount = 0;
	boolean pastrBuilt = false;
	int defeatCount = 0; //how many times we made a pastr and it got blown up

	MapLocation pastrLoc;
	Direction noiseDir = null;

	ArrayList<PastureBlock> pastrBlocks = new ArrayList<PastureBlock>();
	ArrayList<MapLocation> checkedPastures = new ArrayList<MapLocation>();

	int numRobots;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		this.spawn();
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

		for(int i = 0; i < 8; i++){
			int res = this.myRC.senseTerrainTile(this.myHQLoc.add(dirs[i])).ordinal();
			if(res == 0 || res == 2)
				openDirs++;
		}
		
		this.myRC.broadcast(274, openDirs);
		
		strategy = get_strategy();

		checkCowGrowth();
		checkPastures();
		MapLocation loc = find_pastr_loc();

		if (loc== null) {
			Direction dir = toEnemy;
			while (!isGood(this.myHQLoc.add(dir))) {
				dir = dir.rotateRight();
			} loc = this.myHQLoc.add(dir);
		}
		set_pastr_loc(loc);
	}

	protected void checkCowGrowth() {
		for (int i=0; i<spawnRates.length; ++i) {
			for (int j=0; j<spawnRates[0].length; ++j) {
				if (spawnRates[i][j]>bestGrowth) {
					bestGrowth = spawnRates[i][j];
					numBest = 0;
				} if (spawnRates[i][j]==bestGrowth) {
					++numBest;
				}
			}
		}
		numBest/= this.mapHeight*this.mapWidth;

		for (int i=0; i<spawnRates.length; ++i) {
			for (int j=0; j<spawnRates[0].length; ++j) {
				if (spawnRates[i][j]==bestGrowth) {
					continue;
				} if (spawnRates[i][j]>nextBestGrowth) {
					nextBestGrowth = spawnRates[i][j];
				}
			}
		}		
		if (bestGrowth!=0 && (numBest > 0.05 || nextBestGrowth/bestGrowth < 0.6)) {
			this.threshRatio = 1;
		} else {
			this.threshRatio = nextBestGrowth/bestGrowth - 0.1;
		}
		System.out.println("thresh ratio: " + threshRatio);
	}

	protected MapLocation find_pastr_loc() throws GameActionException{
		System.out.println("number of checked pastures: " + checkedPastures.size());
		int minHeuristic = 51; 
		int pasturePicked = 0;
		MapLocation bestPasture = this.myHQLoc.add(toEnemy, 2);
		for (int i=0; i<checkedPastures.size(); ++i) {
			int nextHeuristic = pastureVoids(checkedPastures.get(i));
			if (nextHeuristic > 40) {
				checkedPastures.remove(i); i--;
				System.out.println(checkedPastures.size()+" "+nextHeuristic);
			} else if (nextHeuristic<=5) {
				bestPasture = checkedPastures.remove(i); 
				return bestPasture;
			} else if (nextHeuristic < minHeuristic) {
				minHeuristic = nextHeuristic;
				bestPasture = checkedPastures.get(i);
				pasturePicked = i;
			} 
			if (this.myRC.isActive()) {
				spawn();
			}
		} 
		if (bestPasture.equals(this.myHQLoc.add(toEnemy, 2))) {
			threshRatio-= 0.2;
			if (threshRatio>0){
				checkPastures();
				bestPasture = find_pastr_loc();
			}
		} else {
			checkedPastures.remove(pasturePicked);
		}
		
		System.out.println(bestPasture.equals(this.myHQLoc.add(toEnemy, 2)));
		return bestPasture;
	}

	protected void set_pastr_loc(MapLocation loc) throws GameActionException{
		pastrLoc = loc;
		System.out.println("pasture location: "+pastrLoc);

		if (pastrLoc==null) {
			this.defaultSpawnLoc = this.myHQLoc.add(toEnemy);
			pastrLoc = this.myHQLoc.add(toEnemy, 2);
		} else {
			this.defaultSpawnLoc = this.myHQLoc.add(this.myHQLoc.directionTo(this.pastrLoc));
		}
		ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
		this.myRC.broadcast(PASTR_LOC_CHANNEL, (int)action.encode());
		ActionMessage action2 = null;
		for (int i=0; i<8; ++i){
			if (this.myRC.senseTerrainTile(pastrLoc.add(dirs[i])).ordinal() < 2){
				action2 = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc.add(dirs[i]));
				i=8;
			}
		}
		if (action2!= null) {
			this.myRC.broadcast(NOISE_LOC_CHANNEL, (int)action2.encode());
		}
	}
/*	
	@Override
	protected void step() throws GameActionException {
		if (this.myRC.isActive() && this.myRC.senseRobotCount()< GameConstants.MAX_ROBOTS) {
			this.spawn();
		}
		ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
		if (action!= null) {
			this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
		}
	}
	
*/	
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
 
		if(pastrBuilt && pastrCount == 0){
			//if we got blown up, might as well make pasture somewhere else
			defeatCount++;
			set_pastr_loc(find_pastr_loc());
		}
 
		pastrBuilt = (pastrCount > 0);
		
		Robot[] allies = this.myRC.senseNearbyGameObjects(Robot.class, 100000, this.myTeam);
		int totalAllies = allies.length;
		
		Robot[] nearAllies = this.myRC.senseNearbyGameObjects(Robot.class, 100, this.myTeam);
		int nearTotAllies = nearAllies.length;
		
		this.myRC.broadcast(ALLY_NUMBERS, totalAllies - pastrCount*2);
 
		int nearEnemies = this.myRC.readBroadcast(PASTR_DISTRESS_CHANNEL);
 
		ActionMessage action = null;
 
		if(strategy == 1){
			double enemyMilk = this.myRC.senseTeamMilkQuantity(enemyTeam);
			double myMilk = this.myRC.senseTeamMilkQuantity(myTeam);
 
			if(state == State.WAIT){
				boolean gameBegin = (!pastrBuilt && defeatCount == 0);
				
				if(gameBegin){
					if(totalAllies > 5){
						state = State.DEFEND;
					}
				} else {
					if(pastrBuilt && (totalAllies > 5 || nearTotAllies > 2)){
						state = State.DEFEND;
					} else if(closestTarget != null && totalAllies > 7){
						state = State.ATTACK;
					} else if(!pastrBuilt && (totalAllies > 10 || nearTotAllies > 4)){
						state = State.DEFEND;
					}
				}
			}
			
			if(state == State.ATTACK){
				if(closestTarget == null)
					state = State.DEFEND;
				if(pastrBuilt && nearEnemies > 0)
					state = State.DEFEND;
				
				if(totalAllies - 2*pastrCount < 3)
					state = State.WAIT;
			}
			
			if(state == State.DEFEND){
				if(closestTarget != null && nearEnemies == 0)
					state = State.ATTACK;
				
				if(totalAllies < 5 && nearTotAllies < 2)
					state = State.WAIT;
			}
 
			if(state == State.ATTACK){
				action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
			} if(state == State.DEFEND){
				action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
			} if(state == State.WAIT){
				action = new ActionMessage(BaseRobot.State.WAIT, 0, this.myHQLoc);
			}
		}
 
		if(strategy == 2){
			//the extreme defensive strategy; make pasture right next to our HQ
			//make a noisetower in some random place and have it attack a pasture
			//when group is big enough, go attack
			//based on the anim0rphs strategy; abandoned because it wasn't really working
 
			MapLocation pastr = this.myHQLoc.add(toEnemy, 2);			
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
			}
		}
 
		if(strategy == 4){
			if(closestTarget != null){
				action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
				blewUpAnEnemyPastr = true;
			} else if(!blewUpAnEnemyPastr || allies.length < 4){
				action = new ActionMessage(BaseRobot.State.WAIT, 0, this.myHQLoc);
			} else {
				action = new ActionMessage(BaseRobot.State.WAIT, 0, this.enemyHQLoc);
			}
 
		}
 
		if (action!= null) {
			this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
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
				if (this.myRC.senseObjectAtLocation(candidate) == null
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

		for(int i=0; i<nearbyEnemies.length; ++i){ //try to hit something directly
			Robot r = nearbyEnemies[i];
			curloc = this.myRC.senseRobotInfo(r).location;
			if(this.myRC.canAttackSquare(curloc)){
				this.myRC.attackSquare(curloc);
				return true;
			}
		}

		for(int i=0; i<nearbyEnemies.length; ++i){ //try to hit something for splash damage
			Robot r = nearbyEnemies[i];
			curloc = this.myRC.senseRobotInfo(r).location;
			curloc = curloc.add(curloc.directionTo(this.myHQLoc));
			if(this.myRC.canAttackSquare(curloc)){
				this.myRC.attackSquare(curloc);
				return true;
			}
		}

		return false; //give up    	
	}

	public boolean isGoodLoc(int x, int y){
		boolean xokay= x>=0 && x<this.mapWidth;
		boolean yokay= y>=0 && y<this.mapHeight;
		boolean inEnemySquare= (x-this.enemyHQLoc.x)*(x-this.enemyHQLoc.x) + (y-this.enemyHQLoc.y)*(y-this.enemyHQLoc.y) <= 
				(x-this.myHQLoc.x)*(x-this.myHQLoc.x) + (y-this.myHQLoc.y)*(y-this.myHQLoc.y);
		return xokay && yokay && !inEnemySquare;
	}
	
	private MapLocation reflect(MapLocation loc){
		int midX2 = (this.myHQLoc.x + this.enemyHQLoc.x), midY2 = (this.myHQLoc.y + this.enemyHQLoc.y);
		return new MapLocation(midX2 - loc.x, midY2 - loc.y);
	}

	public void checkPastures() throws GameActionException {
		double thresh = this.bestGrowth*this.threshRatio;
		
		for (int i=0; i<spawnRates.length; ++i) {
			for (int j=0; j<spawnRates[0].length; ++j) {
				if (spawnRates[i][j]>=thresh) {
					MapLocation candidate = new MapLocation(i, j);
					if (candidate.distanceSquaredTo(this.myHQLoc)>= candidate.distanceSquaredTo(this.enemyHQLoc)){
						candidate = reflect(candidate);
					}
					checkedPastures.add(candidate);
				}
			}
		} System.out.println("done checking pastures");

	}

	public int pastureVoids(MapLocation loc) {
		int voidCount = 0;
		for (int i= -2; i<3; ++i) {
			for (int j= -2; j<3; ++j) {
				if (!isGoodLoc(loc) || spawnRates[loc.x+i][loc.y+j]==0) {
					voidCount+= 2;
				} else if (spawnRates[loc.x+i][loc.y+j]<= (threshRatio-0.2)*bestGrowth) {
					voidCount++;
				}
			}
		} return voidCount;
	}
	
	public boolean isGood(MapLocation loc) {
		if (loc.x >= map.length || loc.y >= map[0].length) return false;
		if (loc.distanceSquaredTo(this.enemyHQLoc) <= 25) return false;
		int ans = map[loc.x][loc.y];
		if (ans < 0) return false;
		if (ans > 0) return true;
		
		TerrainTile tile = myRC.senseTerrainTile(loc);
		if (tile == TerrainTile.NORMAL){
			map[loc.x][loc.y] = 1;
			return true;
		}
		if (tile == TerrainTile.ROAD){
			map[loc.x][loc.y] = 2;
			return true;
		}
		else {
			map[loc.x][loc.y] = -1;
			return false;
		}
	}
}
