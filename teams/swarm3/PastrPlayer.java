package swarm3;

import battlecode.common.*;

public class PastrPlayer extends BaseRobot{
	
    public PastrPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
		int strategy_ord = myRC.readBroadcast(STRATEGY_CHANNEL);
		strategy = Strategy.values()[strategy_ord - 1];
    }

	@Override
    protected void step() throws GameActionException{
    	switch (strategy){
	    	case COWVERT: {
	    		int total_cows = 0;
	    		for (MapLocation loc: MapLocation.getAllMapLocationsWithinRadiusSq(myRC.getLocation(), 5)){
	    			total_cows += myRC.senseCowsAtLocation(loc);
	    		}
	    		myRC.broadcast(PASTR_COW_CHANNEL, total_cows);
	    		break;
	    	}
	    	case FARMVILLE: {
	    		break;
	    	}
	    	default: {
	    		break;
	    	}
    	}
    }
}
