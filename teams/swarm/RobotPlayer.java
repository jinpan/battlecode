package swarm;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class RobotPlayer {
    public static void run(RobotController rc) {
        BaseRobot myRobot = null;
        
        try {
	        switch(rc.getType()){
	            case HQ: myRobot = new HQPlayer(rc); break;
	            case SOLDIER: myRobot = new SoldierPlayer(rc); break;
	            case PASTR: myRobot = new PastrPlayer(rc); break;
	            case NOISETOWER: myRobot = new NoisePlayer(rc); break;
	        }	        
	        myRobot.run();
	        //myRobot.myRC.setIndicatorString(0, (Navigation.pathFind(new MapLocation(6, 6), new MapLocation(46, 24), myRobot)).toString());
       
        } catch (GameActionException e) {
			System.out.println("Soldier Exception");
			e.printStackTrace();
		}
    }
}
