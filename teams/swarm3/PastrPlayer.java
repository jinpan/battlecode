package swarm3;

import java.util.LinkedList;

import battlecode.common.*;

public class PastrPlayer extends BaseRobot{
	double[][] map, spawn_rates;
	
	MapLocation[] relevantMapLocations;
	Navigation navigator;
	MapLocation my_loc;
	
    public PastrPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
		int strategy_ord = myRC.readBroadcast(STRATEGY_CHANNEL);
		strategy = Strategy.values()[strategy_ord - 1];
		
		map = new double[myRC.getMapWidth()][myRC.getMapHeight()];
		spawn_rates = myRC.senseCowGrowth();
		map = myRC.senseCowGrowth();
		
		navigator = new Navigation(myRC);
		my_loc = myRC.getLocation();
		
		LocationMessage locMsg = LocationMessage.decode(myRC.readBroadcast(LOC_CHANNEL));
		
		LinkedList<MapLocation> tmp = new LinkedList<MapLocation>();
		for (MapLocation loc: MapLocation.getAllMapLocationsWithinRadiusSq(locMsg.noise_loc, 250)){
			if (navigator.isGood(loc) && spawn_rates[loc.x][loc.y] > 0){
				tmp.add(loc);
			}
		}
		relevantMapLocations = tmp.toArray(new MapLocation[0]);
		
		
		// fake first step
		for (MapLocation loc: relevantMapLocations){
			map[loc.x][loc.y] += spawn_rates[loc.x][loc.y];
		}
		MapLocation target = calculateMax();
		ActionMessage targetMsg = new ActionMessage(State.ATTACK, 0, target);
		myRC.broadcast(NOISE_TARGET_CHANNEL, targetMsg.encode());
    }

	@Override
    protected void step() throws GameActionException{
    	switch (strategy){
	    	case COWVERT: {
	    		cowvertStep();
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
	
	void cowvertStep() throws GameActionException {
		for (MapLocation loc: relevantMapLocations){
			map[loc.x][loc.y] += spawn_rates[loc.x][loc.y];
		}
		
		ActionMessage msg = ActionMessage.decode(myRC.readBroadcast(NOISE_ATTACK_CHANNELS[0]));
		if (msg != null){
			myRC.broadcast(NOISE_ATTACK_CHANNELS[0], 0);
			calculateCowMovement(msg.targetLocation);
			MapLocation target = calculateMax();
			ActionMessage targetMsg = new ActionMessage(State.ATTACK, 0, target);
			myRC.broadcast(NOISE_TARGET_CHANNEL, targetMsg.encode());
		}
	}
	
	void calculateCowMovement(MapLocation attackLoc){
		double tmp;
		MapLocation probe, newLoc;
		System.out.println("START " + Clock.getBytecodeNum());
		MapLocation[] affected = MapLocation.getAllMapLocationsWithinRadiusSq(attackLoc, 9);
		for (int i=affected.length-1; i>0; --i){
			if (affected.equals(attackLoc))
				continue;
			probe = affected[i];
			if (probe.x >= 0 && probe.x < map_width && probe.y >= 0 && probe.y < map_height
					&& spawn_rates[probe.x][probe.x] != 0){
				tmp = map[probe.x][probe.y];
				newLoc = probe.add(attackLoc.directionTo(probe));
				if (navigator.isGood(newLoc)){
					map[newLoc.x][newLoc.y] += tmp;
					map[probe.x][probe.y] -= tmp;
				}
			}
		}
		System.out.println("END " + Clock.getBytecodeNum());

		for (MapLocation neighbor: MapLocation.getAllMapLocationsWithinRadiusSq(my_loc, 5)){
			map[neighbor.x][neighbor.y] = 0;
		}
	}
	
	MapLocation calculateMax(){
		double maxCows = 0;
		MapLocation best = relevantMapLocations[0];
		for (MapLocation loc: relevantMapLocations){
			if (map[loc.x][loc.y]> maxCows){
				maxCows = map[loc.x][loc.y];
				best = loc;
			}
		}
		
		return best;
	}
}
