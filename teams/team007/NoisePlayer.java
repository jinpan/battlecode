package team007;

import team007.BaseRobot;
import battlecode.common.*;

public class NoisePlayer extends BaseRobot{
	
	int cur = 0; //current direction
	MapLocation[] extrema = new MapLocation[8];
	MapLocation curLoc;

	public NoisePlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		get_herding_extrema();
		curLoc = extrema[cur];
	}

	protected void step() throws GameActionException{
		if(this.myRC.isActive()){
			if(curLoc.equals(this.myRC.getLocation())){
				cur = (cur+1)%8;
				curLoc = extrema[cur];
			} else {
				curLoc = curLoc.add(curLoc.directionTo(this.myRC.getLocation()));
			}

			this.myRC.attackSquare(curLoc);
		}
	}
	
	protected void sense_enemies() throws GameActionException{
		Robot[] enemies = this.myRC.senseNearbyGameObjects(Robot.class, RobotType.SOLDIER.attackRadiusMaxSquared*2, this.enemyTeam);
		if (enemies.length>0){
			this.myRC.broadcast(PASTR_DISTRESS_CHANNEL, enemies.length);
		}
	}

	protected void get_herding_extrema() throws GameActionException{ //this finds how far the robots can go in any direction
		double[][] cowGrowth = this.myRC.senseCowGrowth(); //cowGrowth[a][b] is growth at location (a, b)
		MapLocation base = this.myRC.getLocation();

		for(int i = 0; i < 8; i++){
			extrema[i] = base;
			TerrainTile curTerrain = this.myRC.senseTerrainTile(extrema[i]);
			
			while(curTerrain == TerrainTile.NORMAL || curTerrain == TerrainTile.ROAD){
				if(cowGrowth[extrema[i].x][extrema[i].y] == 0 || !this.myRC.canAttackSquare(extrema[i]))
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

			
			extrema[i] = extrema[i].add(dirs[i].opposite());
		}
	}
}
