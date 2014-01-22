package swarm;

import java.util.ArrayList;

import battlecode.common.*;

public class Comms {
	
	static RobotController rc;
	static int[] lengthOfEachPath = new int[100];

	public static ArrayList<MapLocation> downloadPath(SoldierPlayer myRobot) throws GameActionException {
		ArrayList<MapLocation> downloadedPath = new ArrayList<MapLocation>();
		int locationInt = rc.readBroadcast(myRobot.myBand+1 + BaseRobot.PATH_CHANNEL);
		while(locationInt>=0){
			downloadedPath.add(VectorFunctions.intToLoc(locationInt));
			locationInt = rc.readBroadcast(myRobot.myBand+1+downloadedPath.size() + BaseRobot.PATH_CHANNEL);
		}
		rc.setIndicatorString(0, "path length "+downloadedPath.size()+", written round "+Clock.getRoundNum());
		myRobot.myBand = -locationInt*100;
		return downloadedPath;
	}
	

	public static void findPathAndBroadcast(int bandID,MapLocation start, MapLocation goal, int bigBoxSize, int joinSquadNo, BreadthFirst bfs) throws GameActionException{
		//tell robots where to go
		//the unit will not pathfind if the broadcast goal (for this band ID) is the same as the one currently on the message channel
		int band = bandID*100 + BaseRobot.PATH_CHANNEL;
		MapLocation pathGoesTo = VectorFunctions.intToLoc(rc.readBroadcast(band+lengthOfEachPath[bandID]));
		if(!pathGoesTo.equals(bfs.trimGoal(VectorFunctions.mldivide(goal,bigBoxSize)))){
			//rc.setIndicatorString(0,"sending from "+start+" to "+goal+" on round "+Clock.getRoundNum());
			ArrayList<MapLocation> foundPath = bfs.pathTo(VectorFunctions.mldivide(start,bigBoxSize), VectorFunctions.mldivide(goal,bigBoxSize), 100000);
			for(int i=foundPath.size()-1;i>=0;i--){
				rc.broadcast(band+i+1, VectorFunctions.locToInt(foundPath.get(i)));
			}
			lengthOfEachPath[bandID]= foundPath.size();
			rc.broadcast(band+lengthOfEachPath[bandID]+1, -joinSquadNo);
			rc.broadcast(band, Clock.getRoundNum());
		}
	}
	
}