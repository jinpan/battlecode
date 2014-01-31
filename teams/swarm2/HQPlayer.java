package swarm2;

import java.util.LinkedList;
import java.util.ArrayList;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	int strategy = 1; 
	boolean attack = false; 
	int attackDist;
	Direction toEnemy;
	int distToEnemy;
	
	double[][] spawnRates;
	int mapWidth; int mapHeight;
	double bestGrowth = 0;
	double nextBestGrowth = 0; double numBest = 0;
	MapLocation defaultSpawnLoc; 
	double threshRatio;
	
	int numPastures = 1;
	int pastrCount = 0;
	boolean pastrBuilt = false;
	int defeatCount = 0; //how many times we made a pastr and it got blown up
	MapLocation pastrLoc;
	Direction noiseDir = null;
	
	ArrayList<PastureBlock> pastrBlocks = new ArrayList<PastureBlock>();
	ArrayList<MapLocation> pastrLocs = new ArrayList<MapLocation>();
	ArrayList<MapLocation> checkedPastures = new ArrayList<MapLocation>();

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		
		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.distToEnemy = this.myHQLoc.distanceSquaredTo(this.enemyHQLoc);
		this.mapWidth = this.myRC.getMapWidth();
		this.mapHeight = this.myRC.getMapHeight();
		this.spawnRates = this.myRC.senseCowGrowth();
		checkCowGrowth();
		checkPastures();
		MapLocation loc = find_pastr_loc();
//		findPastureBlock();
//		MapLocation loc = this.myHQLoc.add(toEnemy, 2);
//		if (pastrLocs.size()>0) {
//			loc = pastrLocs.get(0);
//		}
		set_pastr_loc(loc);
		setStrategy();
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
	
	protected void setStrategy() {
		if (this.myTeam == Team.A){
			strategy = 1;
		} else {
			strategy = 3;
		}
	}
	
	protected MapLocation find_pastr_loc() throws GameActionException{
		if (checkedPastures.size()==0) {
			System.out.println("something's up with checked pastures");
			threshRatio-= 0.3;
			threshRatio = MAX(threshRatio, 0);
			if (threshRatio == 0) {
				this.numPastures = 0;
				return null;
			} checkPastures();
		} int minHeuristic = 100; 
		MapLocation bestPasture = this.myHQLoc.add(toEnemy, 2);
		for (int i=0; i<checkedPastures.size(); ++i) {
			int nextHeuristic = pastureVoids(checkedPastures.get(i));
			if (nextHeuristic < minHeuristic) {
				minHeuristic = nextHeuristic;
				bestPasture = checkedPastures.get(i);
			} if (nextHeuristic> 0.8) {
				checkedPastures.remove(i); i--;
			}
		} 
		System.out.println("best found pasture: " + bestPasture);
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
	
	protected void step() throws GameActionException {
		if (this.myRC.isActive() && this.myRC.senseRobotCount()< GameConstants.MAX_ROBOTS) {
			this.spawn();
		}
		ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
		if (action!= null) {
			this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int) action.encode());
		}
	}
	
/*
	@Override
	protected void step() throws GameActionException {

		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}

		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
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

		if(strategy == 3){
			
			double enemyMilk = this.myRC.senseTeamMilkQuantity(enemyTeam);
			double myMilk = this.myRC.senseTeamMilkQuantity(myTeam);
			boolean losing = (enemyMilk - myMilk) > 9000;
			
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
		
		int caution = this.myRC.readBroadcast(CAUTION_CHANNEL);
		if (Clock.getRoundNum() > caution + 2){
			this.myRC.broadcast(CAUTION_CHANNEL, 0);
		}
		
	}
*/
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

		for(int i=0; i<nearbyEnemies.length; ++i){ //try to hit something directly
			Robot r = nearbyEnemies[i];
			curloc = this.myRC.senseRobotInfo(r).location;
			if(this.myRC.canAttackSquare(curloc)){
				this.myRC.attackSquare(curloc);
				return true;
			}
		}

		for(int i=0; i<nearbyEnemies.length; ++i){ //try to hit something for splash damage
			Robot r= nearbyEnemies[i];
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
	    
	 public boolean notOverlapping(MapLocation loc){
	    	for (PastureBlock block: this.pastrBlocks){
	    		if (block.contains(loc)){
	    			return false;
	    		}
	    	} return true;
	    }
	    
	 public PastureBlock findPastureBlock(){
		 double spawnThresh = this.threshRatio*this.bestGrowth;

		 int width=0; int height=0; 
		 int xadd=0; int yadd=0;
		 MapLocation vertex=null;
		 PastureBlock block = null;

		 int i=1; 

		 while (vertex==null && i<(int)this.mapHeight/2){
			 int y1= this.myHQLoc.y-i; 
			 int y2= this.myHQLoc.y+i;
			 int x1= this.myHQLoc.x-i; 
			 int x2= this.myHQLoc.x+i;
			 for (int j=0; j<2*i+1; ++j){
				 if ((width==0&&height==0) && isGoodLoc(x1+j, y1) && spawnRates[x1+j][y1]>=spawnThresh){
					 MapLocation temp = new MapLocation(x1+j, y1);
					 if (notOverlapping(temp)){
						 xadd= 1; yadd= -1; 
						 width=1; height=1;
						 vertex= temp;
					 } else {
						 vertex=null;
					 }
				 } else if ((width==0&&height==0) && isGoodLoc(x1+j, y2) && spawnRates[x1+j][y2]>=spawnThresh){
					 MapLocation temp= new MapLocation(x1+j, y2);
					 if (notOverlapping(temp)){
						 xadd= 1; yadd= 1; 
						 width=1; height=1;
						 vertex= temp;
					 } else {
						 vertex=null;
					 }
				 } else if ((width==0&&height==0) && isGoodLoc(x1, y1+j) && spawnRates[x1][y1+j]>=spawnThresh){
					 MapLocation temp = new MapLocation(x1, y1+j);
					 if (notOverlapping(temp)){
						 xadd= -1; yadd= 1; 
						 width=1; height=1;
						 vertex= temp;
					 } else {
						 vertex=null;
					 }
				 } else if ((width==0&&height==0) && isGoodLoc(x2, y1+j) && spawnRates[x2][y1+j]>=spawnThresh){
					 MapLocation temp= new MapLocation(x2, y1+j);
					 if (notOverlapping(temp)){
						 xadd= 1; yadd= 1; 
						 width=1; height=1;
						 vertex= temp;
					 } else {
						 vertex=null;
					 }
				 }
			 }++i;
		 }
		 if (vertex!= null){
			 int a= vertex.x; int b= vertex.y;
			 for (int j=0; j<this.mapHeight/2; ++j){
				 if (isGoodLoc(a, b+yadd*j) && spawnRates[a][b+yadd*j]>=spawnThresh){
					 ++height;
				 } else {
					 j= this.mapHeight;
				 }
			 } for (int k=0; k<this.mapWidth/2; ++k){
				 if (isGoodLoc(a+ xadd*k, b)&& spawnRates[a+xadd*k][b]>=spawnThresh){
					 ++width;
				 } else{
					 k= this.mapWidth;
				 }
			 }
			 //this.detectedVertices.add(vertex);
			 //System.out.println(this.detectedVertices.get(this.detectedVertices.size()-1));
			 vertex= new MapLocation(a, b);
			 block= new PastureBlock(vertex, width-1, height-1);
			 this.pastrBlocks.add(block);
			 block.pastrLocs(this.pastrLocs);
		 } 
		 
		 if (block==null) {
			 threshRatio-=0.2;
			 System.out.println("new thresh ratio: " + threshRatio);
			 if (threshRatio>0) {
				 findPastureBlock();
			 }
		 }
		 return block;
	 }

	 private MapLocation reflect180(MapLocation loc) throws GameActionException{
		 return new MapLocation(this.mapWidth-1 - loc.x, this.mapHeight-1 - loc.y);
	 }

	 private MapLocation reflectY(MapLocation loc) throws GameActionException{
		 return new MapLocation(loc.x, this.mapHeight -1 - loc.y);
	 }

	 private MapLocation reflectX(MapLocation loc) throws GameActionException {
		 return new MapLocation(this.mapWidth -1 - loc.x, loc.y);
	 }

	 public void checkPastures() throws GameActionException {
		 double thresh = this.bestGrowth*this.threshRatio;
		 for (int i=0; i<spawnRates.length; ++i) {
			 for (int j=0; j<spawnRates[0].length; ++j) {
				 if (spawnRates[i][j]>=thresh) {
					 MapLocation candidate = new MapLocation(i, j);
					 if (candidate.distanceSquaredTo(this.myHQLoc)>= candidate.distanceSquaredTo(this.enemyHQLoc)){
						 candidate = reflect180(candidate);
						 if (spawnRates[candidate.x][candidate.y]<thresh) {
							 System.out.println("whoops 180 reflection");
							 candidate = reflectY(reflect180(candidate));
							 if (spawnRates[candidate.x][candidate.y]<thresh) {
								 System.out.println("whoops y-axis reflection");
								 candidate = reflectX(reflectY(candidate));
							 }
						 }
					 }
					 checkedPastures.add(candidate);
				 }
			 }
		 }
	 }

	public int pastureVoids(MapLocation loc) {
		int voidCount = 0;
		for (int i=-3; i<4; ++i) {
			for (int j=-3; j<6; ++j) {
				if (isGoodLoc(loc.x+i, loc.y+j) && spawnRates[loc.x+i][loc.y+j]< (threshRatio- 0.05)*bestGrowth) {
					++voidCount;
				}
			}
		} return (int)(100.0*voidCount/25.0);
	}
}
