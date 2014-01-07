package testPlayer;

import battlecode.common.*;

public class soldierPlayer extends RobotBase{
	int turncounter = 0, pastrCounter = 0; //number of turns following pastrBot, number of turns to go towards pastr
	boolean pastrFollowing = true, towardPastr = true;
	boolean hasHitWall = false;
	Direction curdir = Direction.NORTH;
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
				if(rc.isActive()){
					curdir = adjustDir(curdir);
					if(rc.canMove(curdir))
						rc.move(curdir);
					turncounter++;
				}
			}
		} else {
			MapLocation[] pastrs = rc.sensePastrLocations(rc.getTeam());
			Direction pDir;
			pDir = Direction.NORTH;
			if(pastrs.length != 0){
				pastrLoc = pastrs[(int)(Math.random() * pastrs.length - 1)];
				pDir = rc.getLocation().directionTo(pastrLoc);
			}
			
			//detect if we should turn into PASTR instead
			MapLocation[] nearby = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 6); 
			int cowtot = 0;
			for(MapLocation i : nearby){
				cowtot += rc.senseCowsAtLocation(i);
			}
			if(cowtot > 3000 && rc.isActive()){
				boolean tooClose = false;
				for(MapLocation i : pastrs){
					if(rc.getLocation().distanceSquaredTo(i) < 12)
						tooClose = true;
				}
				if(!tooClose)
					rc.construct(RobotType.PASTR);
			}
			
			if(towardPastr){

				if(pastrs.length == 0){ //if not built yet, shuffle around
					curdir = directions[(int)(Math.random()*8)];
				} else { //when done, go to the nearest one
					if(!rc.canMove(curdir)){
						int i = 0;
						while(!rc.canMove(curdir) && i < 8){
							curdir = curdir.rotateLeft();
						}
					}
				}
				
				pastrCounter--;
				
				boolean nextToPASTR = false;
				for(MapLocation i : pastrs){
					if(rc.getLocation().distanceSquaredTo(i) < 3)
						nextToPASTR = true;
				}
				if(pastrCounter <= 0 || nextToPASTR){
					pastrCounter = 0;
					hasHitWall = false;
					towardPastr = false; 
					curdir = getGoodDir();
				}
				
			} else {
				pastrCounter++;
				if((pastrCounter > 30 && hasHitWall) || pastrCounter > 80){
					towardPastr = true;
					curdir = pDir;
				}
				
				if(!rc.canMove(curdir)){
					hasHitWall = true;
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
	
	Direction getGoodDir() throws GameActionException{
		int mini = 9999;
		int goodd = 0;
		int offset = 10000;
		for(int i = offset; i < offset+8; i++){
			if(rc.readBroadcast(i+offset) < mini){
				mini = rc.readBroadcast(i+offset);
				goodd = i-offset;
			}
		}
		rc.broadcast(goodd+offset, Clock.getRoundNum());
		return directions[goodd];
	}
}
