package testPlayer;

import battlecode.common.*;

public class soldierPlayer extends RobotBase{
	int turncounter = 0, pastrCounter = 0; //number of turns following pastrBot, number of turns to go towards pastr
	//int[] moveLog = new int[50];
	boolean pastrFollowing = true, towardPastr = true;
	MapLocation pastrLoc;
	
	public soldierPlayer(RobotController rc) throws GameActionException{
		super(rc);
	}
	
	void step() throws GameActionException{
		if(pastrFollowing){
			int pdir = rc.readBroadcast(turncounter);
			if(pdir == 999){
				pastrFollowing = false;
				towardPastr = true;
				pastrCounter = 10;
			} else { //retrace the pastrbot's steps
				Direction curdir = directions[pdir];
				turncounter++;
				if(rc.isActive() && rc.canMove(curdir)){
					rc.move(curdir);
				}
			}
		} else {
			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam());
			Direction curdir, pDir;
			pDir = Direction.NORTH;
			if(pastrs.length != 0){
				pastrLoc = pastrs[0];
				pDir = rc.getLocation().directionTo(pastrLoc);
			}
			
			if(towardPastr){
				pastrCounter--;
				if(pastrCounter <= 0){
					towardPastr = false; 
				}
				
				if(pastrs.length == 0){ //if not built yet, shuffle around
					curdir = directions[(int)(Math.random()*8)];
				} else { //when done, move towards it
					curdir = pDir;
					if(!rc.canMove(curdir)){
						int i = 0;
						while(!rc.canMove(curdir) && i < 8){
							curdir = curdir.rotateLeft();
						}
					}
				}
			} else {
				pastrCounter++;
				if(pastrCounter > 20){
					towardPastr = true;
				}
				
				curdir = pDir.opposite();
				if(!rc.canMove(curdir)){
					int i = 0;
					while(!rc.canMove(curdir) && i < 8){
						curdir = curdir.rotateLeft();
					}
				}
			}
			
			if(rc.isActive() && rc.canMove(curdir)){
				if(towardPastr)
					rc.move(curdir);
				if(!towardPastr)
					rc.sneak(curdir);
			}
		}
		

		rc.yield();
	}
	
	
}
