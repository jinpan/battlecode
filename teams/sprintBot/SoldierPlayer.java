package sprintBot;

import java.util.LinkedList;

import jinpan.BaseRobot.State;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierPlayer extends BaseRobot {
    
	MapLocation targetLoc;
	protected int soldier_order;
	

    public SoldierPlayer(RobotController myRC) throws GameActionException {
        super(myRC);

        this.soldier_order = this.myRC.readBroadcast(BaseRobot.SOLDIER_ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.SOLDIER_ORDER_CHANNEL, this.soldier_order + 1);
    }
    
    @Override
    protected void setup() throws GameActionException {
    	super.setup();
    	
    	if (this.newAction != null){
    		this.actionQueue.addFirst(this.newAction);
    		this.myState = this.newAction.state;
    		this.newAction = null;
    		System.out.println("NOT NULL");
    		System.out.println(this.myState);
    	}
    }

    @Override
    protected void step() throws GameActionException {
    	
    	this.myRC.setIndicatorString(2, this.myState.name());
       
        switch (this.myState) {
            case ATTACK: this.attack_step(); break;
            case DEFEND: this.defend_step(); break;
            case HERD: this.herd_step(); break;
            case PASTURIZE: this.pasturize_step(); break;
            
            default: this.default_step(); return;
        }
    }
    
    protected void attack_step() throws GameActionException {
    	Action action = this.actionQueue.getFirst();
		this.myRC.setIndicatorString(3, String.valueOf(this.myRC.getActionDelay()));
    	this.myRC.setIndicatorString(4, action.targetLocation.toString());
    	
    	LinkedList<EnemyProfileMessage> highEnemies = this.getEnemies(0);
    	LinkedList<EnemyProfileMessage> mediumEnemies = this.getEnemies(1);
    	LinkedList<EnemyProfileMessage> lowEnemies = this.getEnemies(2);
    	
    	int highCounter = highEnemies.size();
    	int mediumCounter = mediumEnemies.size();
    	int lowCounter = lowEnemies.size();
    	
    	boolean attacked = (this.myRC.getActionDelay() > 1);
    	
    	Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10, this.enemyTeam);
    	RobotInfo[] nearbyInfo = new RobotInfo[nearbyEnemies.length];
    	for (int i=0; i<nearbyEnemies.length; ++i){
    		nearbyInfo[i] = this.myRC.senseRobotInfo(nearbyEnemies[i]);
    	}
    	
    	for (EnemyProfileMessage enemyProf: highEnemies){
    		for (int i=0; i<nearbyEnemies.length; ++i){
    			if (nearbyEnemies[i].getID() == enemyProf.id){
    				if (attacked || this.myRC.getActionDelay() > 1){
    					if (nearbyInfo[i].location != enemyProf.lastSeenLoc){
    						enemyProf.lastSeenLoc = nearbyInfo[i].location;
    						enemyProf.lastSeenTime = Clock.getRoundNum();
    						long msg = enemyProf.encode();
    						this.squad_send(BaseRobot.SQUAD_HIGH_HITLIST + 2*i, msg);
    					}
    				}
    				else {
	    				this.myRC.attackSquare(nearbyInfo[i].location);
	    				attacked = true;
	    				
	    				enemyProf.lastSeenLoc = nearbyInfo[i].location;
	    				enemyProf.lastSeenTime = Clock.getRoundNum();
	    				enemyProf.health -= 10;
	    				
	    				long msg;
	    				if (enemyProf.health > 0){
		    				msg = enemyProf.encode();
	    				}
	    				else {
	    					msg = -1;
	    				}
	    				
	    				this.squad_send(BaseRobot.SQUAD_HIGH_HITLIST + 2*i, msg);
    				}
    				nearbyEnemies[i] = null; // set to null so we don't count it twice
    			}
    		}
    	}

    	for (EnemyProfileMessage enemyProf: mediumEnemies){
    		for (int i=0; i<nearbyEnemies.length; ++i){
    			if (nearbyEnemies[i] != null && nearbyEnemies[i].getID() == enemyProf.id){
    				if (attacked || this.myRC.getActionDelay() > 1){
    					if (nearbyInfo[i].location != enemyProf.lastSeenLoc){
    						enemyProf.lastSeenLoc = nearbyInfo[i].location;
    						enemyProf.lastSeenTime = Clock.getRoundNum();
    						long msg = enemyProf.encode();
    						this.squad_send(BaseRobot.SQUAD_MED_HITLIST + 2*i, msg);
    					}
    				}
    				else {
	    				this.myRC.attackSquare(nearbyInfo[i].location);
	    				attacked = true;
	    				
	    				enemyProf.lastSeenLoc = nearbyInfo[i].location;
	    				enemyProf.lastSeenTime = Clock.getRoundNum();
	    				enemyProf.health -= 10;
	    				
	    				long msg;
	    				if (enemyProf.health > 0){
		    				msg = enemyProf.encode();
	    				}
	    				else {
	    					msg = -1;
	    				}
	    				
	    				this.squad_send(BaseRobot.SQUAD_MED_HITLIST + 2*i, msg);
    				}
    				nearbyEnemies[i] = null; // set to null so we don't count it twice
    			}
    		}
    	}
    	
    	for (int i=0; i<nearbyEnemies.length; ++i){
    		if (nearbyEnemies[i] != null){
				int health = (int) nearbyInfo[i].health;
    			if (nearbyInfo[i].type == RobotType.SOLDIER){
    				if (!attacked && this.myRC.getActionDelay() < 1){
    					this.myRC.attackSquare(nearbyInfo[i].location);
    					attacked = true;
    					health -= 10;
    				}
    				EnemyProfileMessage enemyProf = new EnemyProfileMessage(nearbyEnemies[i].getID(), health, nearbyInfo[i].location, Clock.getRoundNum());
    				if (health <= 0){
    					// they're dead!
    				}
    				else if (health < 50){
    					this.squad_send(BaseRobot.SQUAD_HIGH_HITLIST + 2 * highCounter, enemyProf.encode());
    					++highCounter;
    				}
    				else {
    					this.squad_send(BaseRobot.SQUAD_MED_HITLIST + 2 * mediumCounter, enemyProf.encode());
    					++mediumCounter;
    				}
    				nearbyEnemies[i] = null;
    			}
    		}
    	}
    	
    	for (EnemyProfileMessage enemyProf: lowEnemies){
    		for (int i=0; i<nearbyEnemies.length; ++i){
    			if (nearbyEnemies[i] != null){
    				if (!attacked && this.myRC.getActionDelay() < 1) {
	    				this.myRC.attackSquare(nearbyInfo[i].location);
	    				attacked = true;
	    				enemyProf.health -= 10;
	    			
	    				long msg;
	    				if (enemyProf.health > 0){
		    				msg = enemyProf.encode();
	    				}
	    				else {
	    					msg = -1;
	    				}
	    				
	    				this.squad_send(BaseRobot.SQUAD_LOW_HITLIST + 2 * lowCounter, msg);
	    				++lowCounter;
    				}
    				nearbyEnemies[i] = null; // set to null so we don't count it twice
    			}
    		}
    	}
    	
    	if (attacked){
    		return;
    	}
    	
    	if (this.myRC.canSenseSquare(action.targetLocation) && this.myRC.senseObjectAtLocation(action.targetLocation) == null) {
			// target was destroyed
			this.actionQueue.removeFirst();
			if (this.actionQueue.size() > 0){
				this.myState = this.actionQueue.getFirst().state;
				this.step();
				return;
			}
			else {
				this.myState = State.DEFAULT;
				this.step();
				return;
			}
		}
    	else {
    		Direction dir = this.myRC.getLocation().directionTo(action.targetLocation);
			for (int i=0; i<7; ++i) {
				if (this.canMove(dir) && this.myRC.getActionDelay() < 1){
					
					this.move(dir);
					return;
				}
				else {
					dir = dir.rotateRight();
				}
			}
    	}
    }
    
    protected void defend_step() throws GameActionException {
        // I'm gonna defend the pasture wheee go me
    }
    
    protected void herd_step() throws GameActionException {
        // I'm gonna scout me some enemies
        
    }
    
    protected void pasturize_step() throws GameActionException {
        // I'm gonna build me some pastures
    	if (this.myRC.isActive()){
	    	Action action = this.actionQueue.getFirst();
	    	

	    	
	    	if (this.myRC.getLocation().equals(action.targetLocation)){
	    		this.myRC.construct(RobotType.PASTR);
	    		return;
	    	}
	    	if (this.myRC.canSenseSquare(action.targetLocation)){
	    		Direction dir = this.myRC.getLocation().directionTo(action.targetLocation);
				for (int i=0; i<7; ++i) {
					if (this.canMove(dir) && this.myRC.getActionDelay() < 1){
						
						this.sneak(dir);
						return;
					}
					else {
						dir = dir.rotateRight();
					}
				}
	    	}
    	}
    }    
    
    protected void default_step() throws GameActionException {
        // I'm gonna just chill and try not to get in anyone's way
    	MapLocation bestLoc = this.myRC.getLocation();
		int buildingpastrs = this.myRC.readBroadcast(BaseRobot.PASTR_BUILDING_CHANNEL);
    	if (this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) > 400 && this.myRC.senseNearbyGameObjects(Robot.class, 10, this.myTeam).length > 3 ) {
    		if (this.myRC.readBroadcast(BaseRobot.PASTR_BUILDING_CHANNEL) < 5){
    			double bestCows = this.myRC.senseCowsAtLocation(this.myRC.getLocation());
    			for (MapLocation loc: MapLocation.getAllMapLocationsWithinRadiusSq(this.myRC.getLocation(), 25)){
    				if (cowPotential(loc) > bestCows){
    					bestLoc = loc;
    					bestCows = cowPotential(loc);
    				}
    			}
    			this.myRC.broadcast(BaseRobot.PASTR_BUILDING_CHANNEL, buildingpastrs + 1);
    	    	this.newAction = new Action(BaseRobot.State.PASTURIZE, bestLoc, 0);
    		}
    	}
    }
    
    private double cowPotential(MapLocation loc) throws GameActionException {
    	double num = 0;
    	
    	for (MapLocation otherLoc: MapLocation.getAllMapLocationsWithinRadiusSq(loc, GameConstants.PASTR_RANGE)){
    		if (this.myRC.canSenseSquare(otherLoc)){
    			num += this.myRC.senseCowsAtLocation(otherLoc);
    		}
    	}
    	
    	return num;
    }
    
    private LinkedList<EnemyProfileMessage> getEnemies(int priority) throws GameActionException {
    	LinkedList<EnemyProfileMessage> result = new LinkedList<EnemyProfileMessage>();
    	
    	int channel = 0;
    	switch (priority){
	    	case 0: channel = BaseRobot.SQUAD_HIGH_HITLIST; break;
	    	case 1: channel = BaseRobot.SQUAD_MED_HITLIST; break;
	    	case 2: channel = BaseRobot.SQUAD_LOW_HITLIST; break;
    	}
    	
    	for (int i=0; i<25; i+=2){
    		int msg1 = this.myRC.readBroadcast(BaseRobot.SQUAD_BASE + channel + i);
    		int msg2 = this.myRC.readBroadcast(BaseRobot.SQUAD_BASE + channel + i + 1);
    		long msg = msg1; msg <<= 32; msg |= msg2 & 0xFFFFFFFFL;
    		if (msg == 0){
    			break;
    		}
    		else if (msg == -1){
    			continue;
    		}
    		else {
    			result.add(EnemyProfileMessage.decode(msg));
    		}
    	}
    	return result;
    }
    
    protected Direction directionTo(MapLocation loc) throws GameActionException {
        Direction dir = this.myRC.getLocation().directionTo(loc);
        
        if (this.myRC.canMove(dir)){
            return dir;
        }
        
        Direction dirA, dirB;
        if (this.random() < 0.5){
            dirA = dir.rotateLeft();
            dirB = dir.rotateRight();
        }
        else {
            dirA = dir.rotateRight();
            dirB = dir.rotateLeft();
        }
        
        if (this.myRC.canMove(dirA)){
            return dirA;
        }
        else if (this.myRC.canMove(dirB)){
            return dirB;
        }
        
        return null;        
    }

}
