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

	protected void get_herding_extrema(){ //this finds how far the robots can go in any direction
		double[][] cowGrowth = this.myRC.senseCowGrowth(); //cowGrowth[a][b] is growth at location (a, b)
		MapLocation base = this.myRC.getLocation();

		for(int i = 0; i < 8; i++){
			extrema[i] = base;

			TerrainTile curTerrain = this.myRC.senseTerrainTile(extrema[i]);
			while(curTerrain == TerrainTile.NORMAL || curTerrain == TerrainTile.ROAD){
				if(cowGrowth[extrema[i].x][extrema[i].y] == 0 || !this.myRC.canAttackSquare(extrema[i]))
					break;

				extrema[i] = extrema[i].add(dirs[i]);
				curTerrain = this.myRC.senseTerrainTile(extrema[i]);
			}

			
			extrema[i] = extrema[i].add(dirs[i].opposite());
		}
	}
}
