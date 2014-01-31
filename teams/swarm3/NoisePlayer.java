package swarm3;

import java.util.LinkedList;

import swarm3.BaseRobot.Strategy;
import battlecode.common.*;

public class NoisePlayer extends BaseRobot{
	
	double health;
	double[][] grouped_spawn_rates;
	double[][] spawn_rates;
	double[][] map;
	double[][] tmp_map;
	Direction[] direction_order = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
	public static Direction[] direction_order_ext = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
			Direction.SOUTH_EAST, Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST};
	MapLocation my_loc;

	MapLocation[] herd_path;
	int herd_idx;
	MapLocation pastr_loc;
	Navigation navigator;
	
	int noise_order = 0;
	int ATTACK_CHANNEL;
	

	public NoisePlayer(RobotController myRC) throws GameActionException {
		super(myRC);
		
		RobotInfo info;
		for (Robot robot: myRC.senseNearbyGameObjects(Robot.class, 20000, my_team)){
			info = myRC.senseRobotInfo(robot);
			if (info.type == RobotType.NOISETOWER){
				++noise_order;
			}
		}
		if (strategy == null){
			int strategy_ord = myRC.readBroadcast(STRATEGY_CHANNEL);
			if (strategy_ord != 0){
				strategy = Strategy.values()[strategy_ord - 1];
			}
		}
		
		ATTACK_CHANNEL = NOISE_ATTACK_CHANNELS[noise_order];
		map = new double[myRC.getMapWidth()][myRC.getMapHeight()];
		tmp_map = new double[7][7];
		
		health = myRC.getHealth();
		my_loc = myRC.getLocation();
		navigator = new Navigation(myRC);
		
		spawn_rates = myRC.senseCowGrowth();
		herd_idx = 0;
		
		switch (strategy){
		case COWVERT: {
			LocationMessage locMsg = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNELS[0]));
			if (locMsg != null){
				pastr_loc = locMsg.pastr_loc;
			}
			break;
			}
			case FARMVILLE: {
				break;
			}
			default: {
				
			}
		}
	}

	protected void step() throws GameActionException{
		sense_enemies();
		
		if (myRC.isActive()){
			
			if (herd_path == null){
				ActionMessage targetMsg = ActionMessage.decode(myRC.readBroadcast(NOISE_TARGET_CHANNEL));
				if (targetMsg != null)
					herd_path = herd(targetMsg.targetLocation, pastr_loc);
				else
					return;
			}
			
			if (herd_idx < herd_path.length && myRC.canAttackSquare(herd_path[herd_idx])){
				if (attackLoc(herd_path[herd_idx])){
					++herd_idx;
				}
			}
			else {
				herd_idx = 0;
				ActionMessage targetMsg = ActionMessage.decode(myRC.readBroadcast(NOISE_TARGET_CHANNEL));
				herd_path = herd(targetMsg.targetLocation, pastr_loc);
			}
		}
	}
	
	boolean attackLoc(MapLocation loc) throws GameActionException{
		ActionMessage msg;
		for (int i=0; i<noise_order; ++i){
			msg = ActionMessage.decode(myRC.readBroadcast(NOISE_ATTACK_CHANNELS[i]));
			if (loc.distanceSquaredTo(msg.targetLocation) <= GameConstants.NOISE_SCARE_RANGE_LARGE){
				for (MapLocation loc2: myRC.sensePastrLocations(enemy_team)){
					if (myRC.canAttackSquare(loc2)){
						myRC.attackSquare(loc2);
						return false;
					}
				}
				return false;
			}
		}
		myRC.attackSquareLight(loc);
		myRC.broadcast(ATTACK_CHANNEL, new ActionMessage(State.ATTACK, Clock.getRoundNum(), loc).encode());
		return true;
	}
	
	protected void sense_enemies() throws GameActionException{
		if (myRC.getHealth() < health){
			Robot[] enemies = myRC.senseNearbyGameObjects(Robot.class, 35, this.enemy_team);
			myRC.broadcast(PASTR_DISTRESS_CHANNEL, enemies.length);
			health = myRC.getHealth();
		}
	}
	
	MapLocation[] herd(MapLocation source, MapLocation target) throws GameActionException{

		LinkedList<MapLocation> path = navigator.findPath(source, target);

		LinkedList<MapLocation> result = new LinkedList<MapLocation>();
		
		MapLocation current = source;
		Direction move_dir;
		path.removeFirst();
		while (true){
			if (current.equals(target)){
				break;
			}
			if (path.getFirst().equals(current)){
				path.removeFirst();
			}
			move_dir = navigator.directionTo(current, path.getFirst());
			result.add(current.add(move_dir.opposite()));
			current = current.add(move_dir);
		}
		return result.toArray(new MapLocation[0]);
	}
	
}
