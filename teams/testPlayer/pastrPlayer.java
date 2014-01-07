package testPlayer;

import battlecode.common.*;

public class pastrPlayer extends RobotBase{
	MapLocation goal;
	int counter = 0;
	int turns = 0;
	Direction curdir = Direction.EAST;
	
	public pastrPlayer(RobotController rc) throws GameActionException{
		super(rc);
	}
	
	void step() throws GameActionException{
		//get far away from headquarters, then build one

		if(rc.isActive()){
			if(rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 400){
				rc.broadcast(turns, 999);
				rc.construct(RobotType.PASTR);
			}else{
				if(counter == 0){
					curdir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				}
					
				if(!rc.canMove(curdir)){
					int i = 0;
					while(!rc.canMove(curdir) && i < 8){
						curdir = curdir.rotateLeft();
					}
					counter += 5; //attempt to back away for a while b/c i'm too lazy to implement smart path stuff
				}
				
				if(rc.canMove(curdir)){
					rc.move(curdir);
					rc.broadcast(turns, dToInt(curdir)); //broadcast what it did
					counter--;
					turns++;
				}
			}
		}
		rc.yield();

	}

}
