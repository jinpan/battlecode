package defensePlus;

import battlecode.common.*;

public class NoisePlayer extends BaseRobot{
	
	int cur = 0; //current direction
	int[] dirPro = {0, 2, 4, 6, 7, 1, 3, 5};
	MapLocation[] extrema = new MapLocation[8];
	MapLocation curLoc;
	MapLocation pastrLoc;

	public NoisePlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		get_herding_extrema();
		curLoc = extrema[cur];
		pastrLoc = ActionMessage.decode(this.myRC.readBroadcast(PASTR_LOC_CHANNEL)).targetLocation;
	}

	protected void step() throws GameActionException{
		sense_enemies();
		
		if(this.myRC.isActive()){
			MapLocation checkPasture = ActionMessage.decode(this.myRC.readBroadcast(PASTR_LOC_CHANNEL)).targetLocation;
			if (!this.pastrLoc.equals(checkPasture)) {
				this.myRC.selfDestruct();
			}
			if(curLoc.distanceSquaredTo(this.myRC.getLocation())<=9){
				if(!cowsNearby()){
					cur = (cur+1)%8;
					curLoc = extrema[dirPro[cur]];					
				}
			} else {
				curLoc = curLoc.add(curLoc.directionTo(this.myRC.getLocation()));
			}

			this.myRC.attackSquare(curLoc);
		}

	}
	
	//attempts to reel in cows that we haven't gotten but could easily get
	protected boolean cowsNearby() throws GameActionException{
		return false;
		//will write this later
		//scan the squares we can see and pull in large clumps cows that are nearby		
	}
	
	protected void sense_enemies() throws GameActionException{
		Robot[] enemies = this.myRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.attackRadiusMaxSquared*5, this.enemyTeam);
		this.myRC.broadcast(PASTR_DISTRESS_CHANNEL, enemies.length);
	}

	protected void get_herding_extrema() throws GameActionException{ //this finds how far the robots can go in any direction
		MapLocation base = this.myRC.getLocation();

		for(int i = 0; i < 8; i++){
			extrema[i] = base;
			TerrainTile curTerrain = this.myRC.senseTerrainTile(extrema[i]);
			
			while(curTerrain == TerrainTile.NORMAL || curTerrain == TerrainTile.ROAD){
				if(!this.myRC.canAttackSquare(extrema[i]))
					break;

				MapLocation tempext = extrema[i].add(dirs[i]);
				curTerrain = this.myRC.senseTerrainTile(tempext);
				
				if (curTerrain==TerrainTile.VOID) { //if the next loc is void, it checks right and left to see if there are alternate routes.
					Direction dirA, dirB;
					if (this.random() < 0.5){
						dirA = dirs[i].rotateLeft();
						dirB = dirs[i].rotateRight();
					}
					else {
						dirA = dirs[i].rotateRight();
						dirB = dirs[i].rotateLeft();
					}
					
					MapLocation tempA= extrema[i].add(dirA);
					MapLocation tempB= extrema[i].add(dirB);
					TerrainTile tempAterr= this.myRC.senseTerrainTile(tempA);
					TerrainTile tempBterr= this.myRC.senseTerrainTile(tempB);
					
					if (tempAterr==TerrainTile.NORMAL || tempAterr==TerrainTile.ROAD){
						extrema[i]= tempA;
						curTerrain= tempAterr;
					} else if (tempBterr==TerrainTile.NORMAL || tempBterr==TerrainTile.ROAD){
						extrema[i]= tempB;
						curTerrain= tempBterr;
					} else {
						extrema[i]= tempext;
					}
				} else {
					extrema[i]= tempext;
				}	
			}
			
			if(!this.myRC.canAttackSquare(extrema[i]))
				extrema[i] = extrema[i].add(dirs[i].opposite());
			
		}
	}
}
