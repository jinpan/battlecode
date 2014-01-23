package swarm;

import java.util.ArrayList;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	
	public static final int MAX_PASTURES = 1;

	Direction toEnemy;
	int distToEnemy;
	MapLocation[] myCorners;
	ArrayList<MapLocation> pastrLocs0;
	ArrayList<PastureBlock> pastrBlocks;
	double spawnThresh;
	int mapHeight; int mapWidth;
	double[][] spawnRates;
	double bestSpawnRate;
	int numBestSpawn;
	
	int pastrCount = 0;

	MapLocation pastrLoc;

	boolean offensive = false; //adjust this smart

	int numRobots;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.spawnRates = this.myRC.senseCowGrowth();
		this.mapHeight = this.myRC.getMapHeight();
		this.mapWidth = this.myRC.getMapWidth();

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.distToEnemy = this.myHQLoc.distanceSquaredTo(this.enemyHQLoc);
		this.numRobots = 1;
		
		double best= 0;
		int counter=0;

		for (int i=0; i<spawnRates.length; i++){
			for (int j=0; j<spawnRates[i].length; j++){
				if (best<spawnRates[i][j]){
					best= spawnRates[i][j];
					counter=0;
				} if (best==spawnRates[i][j]){
					counter++;
				}
			}
		}
		this.bestSpawnRate= best;
		this.spawnThresh= best*0.6;

		this.numBestSpawn= counter;
		
		this.pastrBlocks= new ArrayList<PastureBlock>();
		this.pastrLocs0= new ArrayList<MapLocation>();

		PastureBlock block = this.findPastureBlock();
		
		System.out.println(block==null);
		pastrLoc = this.pastrLocs0.get(0);
		System.out.println(pastrLoc);

	}

	@Override
	protected void step() throws GameActionException {
		
		pastrCount = this.myRC.sensePastrLocations(myTeam).length;
		
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

		//finds enemy pastures and posts locations on message board
		MapLocation[] targets = this.myRC.sensePastrLocations(this.enemyTeam);
		for (int i=0; i<targets.length; ++i){
			if (targets[i].distanceSquaredTo(this.enemyHQLoc) > 15){
				if ((targets[i].distanceSquaredTo(this.myHQLoc) < dist)){
					closestTarget = targets[i];
				}
			}
		}
		
		Robot[] allies = this.myRC.senseNearbyGameObjects(Robot.class, 100000, this.myTeam);
		Robot[] closeAllies = this.myRC.senseNearbyGameObjects(Robot.class, 10, this.myTeam);
		int totalAllies = allies.length;
		int neighborAllies = closeAllies.length;
		//int alliesInAction = totalAllies - neighborAllies;

		this.myRC.broadcast(ALLY_NUMBERS, totalAllies - pastrCount*2);
		//if there's a pasture to attack, do so
		if(closestTarget != null && (totalAllies - pastrCount*2) >5){
			ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
			this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());
		} else if (closestTarget == null && pastrCount< MAX_PASTURES){ 
			//if there are no pastures to attack, we build our own.
			if(pastrLoc != null){
				ActionMessage action = new ActionMessage(BaseRobot.State.DEFEND, 0, pastrLoc);
				this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());
			}
			
		} else if (pastrCount >= MAX_PASTURES){ //rally at some point between our HQ and enemy HQ
			//MapLocation rallypoint= this.myHQLoc.add(toEnemy, 10);
			ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, pastrLoc.add(pastrLoc.directionTo(enemyHQLoc), 2));
			this.myRC.broadcast(HQ_BROADCAST_CHANNEL, (int)action.encode());	 
		}
	}

	private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(this.toEnemy)) == null){
			this.myRC.spawn(this.toEnemy);
			return true;
		}
		else {
			for (Direction dir: BaseRobot.dirs){
				if (dir != this.toEnemy
						&& this.myRC.senseObjectAtLocation(this.myHQLoc.add(dir)) == null){
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

	public int numPastures(){
		return this.numBestSpawn/200;
	}

	public boolean isGoodLoc(int x, int y){
		boolean xokay= x>=0 && x<this.mapWidth;
		boolean yokay= y>=0 && y<this.mapHeight;
		boolean inEnemySquare= (x-myHQLoc.x)*(x-myHQLoc.x)+ (y-myHQLoc.y)*(y- myHQLoc.y) 
				> (x-enemyHQLoc.x)*(x-enemyHQLoc.x)+(y-enemyHQLoc.y)*(y-enemyHQLoc.y);
		return xokay && yokay && !inEnemySquare;
	}

	public boolean notOverlapping(MapLocation loc){
		for (PastureBlock block: this.pastrBlocks){
			if (block.contains(loc)){
				return false;
			}
		} return true;
	}
	
	private MapLocation reflect(MapLocation loc){
		int midX2 = (this.myHQLoc.x + this.enemyHQLoc.x), midY2 = (this.myHQLoc.y + this.enemyHQLoc.y);
		return new MapLocation(midX2 - loc.x, midY2 - loc.y);
	}
	
	private MapLocation findRandomPastureLoc(){
		int x = (int) (this.random() * (this.mapWidth - 4)) + 2;
		int y = (int) (this.random() * (this.mapHeight - 4)) + 2;
		
		return new MapLocation(x, y);
	}
	
	public MapLocation findBestPastureLoc(){
		MapLocation candidate;
		while (true){
			candidate = findRandomPastureLoc();
			if (this.spawnRates[candidate.x][candidate.y] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				// high spawn rate and not a void square
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			
			else if (this.spawnRates[candidate.x+1][candidate.y+1] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x+1, candidate.y+1);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x+1][candidate.y-1] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x+1, candidate.y-1);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x-1][candidate.y-1] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x-1, candidate.y-1);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x-1][candidate.y+1] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x-1, candidate.y+1);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			
			else if (this.spawnRates[candidate.x+2][candidate.y+2] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x+2, candidate.y+2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x+2][candidate.y-2] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x+2, candidate.y-2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x-2][candidate.y-2] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
				candidate = new MapLocation(candidate.x-2, candidate.y-2);
				if (candidate.distanceSquaredTo(this.enemyHQLoc) > candidate.distanceSquaredTo(this.myHQLoc)){
					return candidate;
				}
				else {
					return reflect(candidate);
				}
			}
			else if (this.spawnRates[candidate.x-2][candidate.y+2] >= 3 && this.myRC.senseTerrainTile(candidate).ordinal() < 2){
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

	public PastureBlock findPastureBlock(){
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
			//System.out.println(x1+" "+x2+" "+y1+" "+y2);
			for (int j=0; j<2*i+1; j++){
				//System.out.println(spawnRates[y1][x1+j]);
				//System.out.println(isGoodLoc(x1+j, y1));
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
			}i++;
		}
		if (vertex!= null){
			int a= vertex.x; int b= vertex.y;
			for (int j=0; j<this.mapHeight/2; j++){
				if (isGoodLoc(a, b+yadd*j) && spawnRates[a][b+yadd*j]>=spawnThresh){
					height++;
				} else {
					j= this.mapHeight;
				}
			} for (int k=0; k<this.mapWidth/2; k++){
				if (isGoodLoc(a+ xadd*k, b)&& spawnRates[a+xadd*k][b]>=spawnThresh){
					width++;
				} else{
					k= this.mapWidth;
				}
			}
			//this.detectedVertices.add(vertex);
			//System.out.println(this.detectedVertices.get(this.detectedVertices.size()-1));
			vertex= new MapLocation(min(a+width*xadd, a), min(b+height*yadd, b));
			block= new PastureBlock(vertex, width-1, height-1, pastrBuffer);
			this.pastrBlocks.add(block);
			block.pastrLocs(this.pastrLocs0);
		} 
		return block;
	}

}
