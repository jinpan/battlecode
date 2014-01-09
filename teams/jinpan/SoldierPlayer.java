package jinpan;

import jinpan.BaseRobot.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierPlayer extends BaseRobot {
    
	MapLocation targetLoc;

    public SoldierPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
    }

    @Override
    protected void step() throws GameActionException {
        
        switch (this.myState) {
            case ATTACK: this.attack_step(); return;
            case ATTACKHIGH: this.attack_step(); return;
            case DEFENSE: this.defense_step(); return;
            case DEFENSEHIGH: this.defense_step(); return;
            case PASTURE: this.pasture_step(); return;
            case PASTUREHIGH: this.pasture_step(); return;
            case SCOUT: this.scout_step(); return;
            case SCOUTHIGH: this.scout_step(); return;
            
            default: this.default_step(); return;
        }
    }
    
    /*protected void rally_step() throws GameActionException {
    	Action action= this.actionQueue.getFirst();
    	if (this.myRC.getLocation().distanceSquaredTo(action.targetLocation)<=4
    			&& this.myRC.senseNearbyGameObjects(Robot.class, 4, this.myTeam).length >=2){
    		this.attack_step(); //attack if rally condition satisfied
    	} else {//go to rally location specified in the inbox
    		if (this.myRC.canMove(directionTo(action.targetLocation))&& this.myRC.isActive()){
    			this.myRC.move(directionTo(action.targetLocation));
    		}
    	}
    }*/
    
    //If at any time a robot goes into attack mode, it needs to send an ActionMessage to its own inbox with targetID
    protected void attack_step() throws GameActionException {
        // I have a target and I've rallied at the targetLoc with at least 2 other soldiers and imma go attack.
    	Action action= this.actionQueue.getFirst();
    	Robot target= null;
    	for (Robot enemy: enemies){
    		if (enemy.getID()==action.targetID){
    			target= enemy;
    		}
    	}
    	MapLocation targetLoc= this.myRC.senseRobotInfo(target).location;
    	if (this.myRC.getLocation().distanceSquaredTo(targetLoc)<10){
    		this.myRC.attackSquare(targetLoc);
    	} else {
    		this.myRC.move(directionTo(targetLoc));
    	}
    }
    
    protected void defense_step() throws GameActionException {
    	Action action = this.actionQueue.getFirst();
        // I'm gonna defend the pasture wheee go me
    }
    
    protected void pasture_step() throws GameActionException {
        // I'm gonna build me some pastures
    	if (this.myRC.isActive()){
	    	Action action = this.actionQueue.getFirst();
	    	

	    	
	    	if (this.myRC.getLocation().equals(action.targetLocation)){
	    		this.myRC.construct(RobotType.PASTR);
	    		return;
	    	}
	    	if (this.myRC.canSenseSquare(action.targetLocation)){
		    	GameObject squattingRobot = this.myRC.senseObjectAtLocation(action.targetLocation);
		    	if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam){
		    		Action newAction = new Action(BaseRobot.State.DEFENSE, action.targetLocation, squattingRobot.getID());
		    		this.actionQueue.remove(0);
		    		this.actionQueue.addFirst(newAction);
		    		return;
		    	}
	    	}
    		Direction dir = this.directionTo(action.targetLocation);
    		if (dir == null){
    			this.myRC.construct(RobotType.PASTR);
    			return;
    		}
    		else {
    			this.myRC.move(dir);
    			return;
    		}
    	}
    }    
    
    protected void scout_step() throws GameActionException {
        // I'm gonna scout me some enemies
        
    }
    
    protected void default_step() throws GameActionException {
        // I'm gonna just chill and try not to get in anyone's way
        
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
