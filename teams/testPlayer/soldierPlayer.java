package testPlayer;

import battlecode.common.*;

public class soldierPlayer extends RobotBase{
	int turncounter = 0, pastrCounter = 0; //number of turns following pastrBot, number of turns to go towards pastr
	//int[] moveLog = new int[50];
	boolean pastrFollowing = true, towardPastr = true;
	boolean hasHitWall = false;
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
			Direction curdir, pDir;
			pDir = Direction.NORTH;
			if(pastrs.length != 0){
				pastrLoc = pastrs[pastrs.length - 1];
				pDir = rc.getLocation().directionTo(pastrLoc);
			}
			
			MapLocation[] nearby = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 6);
			int cowtot = 0;
			for(MapLocation i : nearby){
				cowtot += rc.senseCowsAtLocation(i);
			}
			if(cowtot > 5000 && rc.isActive()){
				boolean tooClose = false;
				for(MapLocation i : pastrs){
					if(rc.getLocation().distanceSquaredTo(i) < 12)
						tooClose = true;
				}
				if(!tooClose)
					rc.construct(RobotType.PASTR);
			}
			
			if(towardPastr){
				hasHitWall = false;
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
				if((pastrCounter > 30 && hasHitWall) || pastrCounter > 120){
					towardPastr = true;
				}
				
				curdir = pDir.opposite();
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
	
	
}
