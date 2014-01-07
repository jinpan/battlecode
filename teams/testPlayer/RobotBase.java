package testPlayer;

import battlecode.common.*;

public class RobotBase {
	RobotController rc;
	Team alliedTeam;
	Team enemyTeam;
	Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public RobotBase(RobotController rc) throws GameActionException{
		this.rc = rc;
		this.alliedTeam = rc.getTeam();
		this.enemyTeam = this.alliedTeam.opponent();
	}
	
	void run() throws GameActionException{
		while(true){
			this.step();
		}
	}
	
	void step() throws GameActionException{
		rc.yield();
	}
	
	static int dToInt(Direction dir){
		Direction test = Direction.NORTH;
		int counter = 0;
		for(int i = 0; i < 8; i++){
			if(test == dir)
				return counter;
			test = test.rotateRight();
			counter++;
		}
		return counter;
	}
	
	Direction adjustDir(Direction curdir){
		Direction dir = curdir;
		if(!rc.canMove(dir)){
			int i = 0;
			while(!rc.canMove(dir) && i < 8){
				dir = dir.rotateLeft();
			}
		}
		return dir;
	}
}
