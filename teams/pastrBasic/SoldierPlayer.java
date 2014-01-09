package pastrBasic;

import pastrBasic.Action;
import battlecode.common.*;

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
            case GATHEROUT: this.gatherout_step(); return;
            
            default: this.default_step(); return;
        }
    }
    
    protected void attack_step() throws GameActionException {
    	// I have a target and I'm gonna destroy it! Target may move though .. problems 
    	Action action = this.actionQueue.getFirst();
    	GameObject onSquare = this.myRC.senseObjectAtLocation(action.targetLocation);
    	if (onSquare!= null && this.myRC.senseRobotInfo((Robot) onSquare).type==RobotType.PASTR){
    		targetLoc = action.targetLocation; //prioritize pastr shooting; those don't move
    	}else { //must account for targets moving; is there a better way?
    		/*
    		System.out.println("shouldn't be here");
    		Robot target = null;
    		Robot[] enemies = this.myRC.senseNearbyGameObjects(Robot.class, 1000, this.enemyTeam);
    		for (Robot enemy : enemies){
    			if (enemy.getID()==action.targetID){
    				target = enemy;
    			}
    		}
    		targetLoc = this.myRC.senseRobotInfo(target).location;
    		*/
    	}
    	if (this.myRC.getLocation().distanceSquaredTo(targetLoc)<10){
    		System.out.println("attacking");
    		this.myRC.attackSquare(targetLoc);
    	} else {
    		this.myRC.move(directionTo(targetLoc));
    	}
    }

    protected void defense_step() throws GameActionException {
    	if(this.myRC.isActive()){
    		Action action = this.actionQueue.getFirst();
    		targetLoc = action.targetLocation;
    		
    		Direction dir = this.directionTo(targetLoc);
    		if (dir != null)
    			this.myRC.move(dir);
    		
    		//flesh out later
    		
    		if(this.myRC.getLocation().distanceSquaredTo(targetLoc) < 5 && this.actionQueue.size() > 1)
    			this.actionQueue.removeFirst();
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
		    		pastureloc = action.targetLocation;
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
    	System.out.println(targetLoc.x + " " + targetLoc.y);
    	enemyPastrID= action.targetID;
    	
    	if (this.myRC.getLocation().equals(targetLoc)){
    		MapLocation newLoc= targetLoc.add(this.myHQLoc.directionTo(targetLoc), BaseRobot.RALLY_DISTANCE);
    		Action newAction = new Action(BaseRobot.State.ATTACK, newLoc, enemyPastrID);
    		if (withScoutTeam() || loneRanger()){
    			System.out.println("changing to attacking");
    			this.actionQueue.removeFirst();
    			this.actionQueue.addFirst(newAction);
    		} else {
    			System.out.println("waiting");
    			this.actionQueue.add(1, newAction);
    		}
    	} else {
    		Direction dir = directionTo(targetLoc);
    		if(this.myRC.isActive() && dir != null && this.myRC.canMove(dir)){
    			this.myRC.move(dir);
    		}
    	}
    }
    
    protected boolean withScoutTeam() throws GameActionException { //has allies next to it
    	return this.myRC.senseNearbyGameObjects(Robot.class, 10, this.myTeam).length>0;
    }
    
    protected boolean loneRanger() throws GameActionException { //nobody is nearby
    	return this.myRC.senseNearbyGameObjects(Robot.class, 30, this.myTeam).length==0;
    }
    
    protected void gatherout_step() throws GameActionException{
    	if(this.myRC.isActive()){
    		Action action = this.actionQueue.getFirst();
    		MapLocation target = action.targetLocation;

    		if(this.myRC.getLocation().distanceSquaredTo(target) < 5){
    			this.actionQueue.removeFirst();
    			Action newAction = new Action(BaseRobot.State.DEFENSE, targetLoc, ourPastrID);
    			this.actionQueue.addFirst(newAction);
    		} else {
    			Direction dir = this.directionTo(target);
    			if (dir != null && dir != Direction.NONE && dir != Direction.OMNI)
    				this.myRC.sneak(dir);
    		}
    	}
    }
    
    protected void default_step() throws GameActionException {
        //stop doing nothing if told to do something else
        if(this.actionQueue.size() > 0)
        	this.actionQueue.remove(0);
    }
    
    protected Direction directionTo(MapLocation loc) throws GameActionException {
        Direction dir = this.myRC.getLocation().directionTo(loc);
        
        if(dir == Direction.NONE || dir == Direction.OMNI)
        	return null;
        
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
