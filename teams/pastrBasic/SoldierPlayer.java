package pastrBasic;

import com.sun.corba.se.spi.orbutil.fsm.Action;

import pastrBasic.BaseRobot.State;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierPlayer extends BaseRobot {
    
	MapLocation targetLoc;
	MapLocation pastureloc;
	int ourPastrID;
	int enemyPastrID;
	
	protected static final int DEFENSE_RADIUS= 20;

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
            
            case GATHERIN: this.gatherin_step(); return;
            case GATHEROUT: this.gatherout_step(); return;
            
            default: this.default_step(); return;
        }
    }
    
    protected void attack_step() throws GameActionException {
        // I have a target and I'm gonna destroy it! Target may move though .. problems 
    	Action action= this.actionQueue.getFirst();
    	GameObject onSquare= this.myRC.senseObjectAtLocation(action.targetLocation);
    	if (onSquare!= null && this.myRC.senseRobotInfo(onSquare).type==RobotType.PASTR){
    		targetLoc= action.targetLocation;
    	}else {
    		Robot target= null;
    		for (Robot enemy: enemies){
    			if (enemy.getID()==action.targetID){
    			target= enemy;
    			}
    		}
    	targetLoc= this.myRC.senseRobotInfo(target).location;
    	}
    	if (this.myRC.getLocation().distanceSquaredTo(targetLoc)<10){
    		this.myRC.attackSquare(targetLoc);
    	} else {
    		this.myRC.move(directionTo(targetLoc));
    	}
    }
    
    protected void defense_step() throws GameActionException {
    	if(this.myRC.isActive()){
    		Action action = this.actionQueue.getFirst();
    		targetLoc = action.targetLocation;
    		
    		Direction dir = this.directionTo(theLocation);
    		if (dir != null)
    			this.myRC.move(dir);
    		
    		//actual defense code would go here when it's coded; i'm being lazy
    		
    		if(this.myRC.getLocation().distanceSquaredTo(theLocation) < 5){
    			if(this.actionQueue.size() > 1) //if told to do something else...
    				this.actionQueue.removeFirst();
    		}
    		
    	}
    	
    	
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
		    		ourPastrID = squattingRobot.getID(); //gets and stores the PASTR id
		    		pastureloc= action.targetLocation;
		    		Action newAction = new Action(BaseRobot.State.DEFENSE, pastureloc, ourPastrID);
		    		this.actionQueue.clear();
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
        // scout in squads; HQ assigns all scouts to the same pastr and rallying point. 
    	// Once at rallying point, if it senses other robots of its team in the vicinity, it waits.
    	// Otherwise it goes into attack mode.
    	Action action = this.actionQueue.getFirst();
    	targetLoc= action.targetLocation;
    	enemyPastrID= action.targetID;
    	if (this.myRC.getLocation()==targetLoc){
    		MapLocation newLoc= targetLoc.add(this.myHQLoc.directionTo(targetLoc).opposite(), 
    				BaseRobot.RALLYING_DISTANCE);
    		Action newAction = new Action(BaseRobot.State.ATTACK, newLoc, enemyPastrId);
    		if (withScoutTeam() || loneRanger()){
    			this.actionQueue.removeFirst();
    			this.actionQueue.addFirst(newAction);
    		} else {
    			this.actionQueue.add(1, newAction);
    		}
    	}
    }
    
    protected boolean withScoutTeam() throws GameActionException {
    	return this.myRC.senseNearbyGameObjects(Robot.class, 10, this.myTeam).length>0;
    }
    
    protected void loneRanger() throws GameActionExcpetion {
    	return this.myRC.senseNearbyGameObjects(Robot.class, 30, this.myTeam).length==0;
    }
    
    protected void gatherin_step() throws GameActionException{
    	if(this.myRC.isActive()){
    		Action action = this.actionQueue.getFirst();
    		MapLocation ourPastr = action.targetLocation;
    		Direction dir = this.directionTo(action.targetLocation);
    		
    		if (dir != null)
    			this.myRC.move(dir);
    		
    		if(this.myRC.getLocation().distanceSquaredTo(ourPastr) < 5){
    			this.actionQueue.remove(0);
    			Action newAction = new Action(BaseRobot.State.DEFENSE, ourPastr, this.myRC.senseObjectAtLocation(ourPastr).getID());
    			this.actionQueue.addFirst(newAction);
    		}
    		
    	}
    }
    
    protected void gatherout_step() throws GameActionException{
    	if(this.myRC.isActive()){
    		Action action = this.actionQueue.getFirst();
    		
    		Direction dir = this.directionTo(action.targetLocation);
    		if (dir != null)
    			this.myRC.sneak(dir);
    		
    		if(this.myRC.getLocation().distanceSquaredTo(action.targetLocation) < 5){
    			this.actionQueue.remove(0);
    			Action newAction = new Action(BaseRobot.State.DEFENSE, targetLoc, ourPastrID);
    			this.actionQueue.addFirst(newAction);
    		}
    		
    	}
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
