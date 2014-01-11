package pastrBasic;

import pastrBasic.Action;
import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {
    
	MapLocation targetLoc;
	MapLocation rallyLoc;
	MapLocation pastureloc;
	int ourPastrID;
	int squadNumber;
	boolean rallied=false;
	//boolean underAttack= false;
	
	protected static final int DEFENSE_RADIUS= 20;

    public SoldierPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
    }

    @Override
    protected void step() throws GameActionException {
    	        
        switch (this.myState) {
        	case RALLY: this.rally_step(); return;
            case ATTACK: this.attack_pasture_step(); return;
            case ATTACKHIGH: this.attack_pasture_step(); return;
            case SCOUTHIGH: this.rally_step(); return;
            
            case PASTURE: this.pasture_step(); return;
            case DEFENSE: this.defense_step(); return;
            case DEFENSEHIGH: this.defense_step(); return;
            case GATHEROUT: this.gatherout_step(); return;
            
            default: this.default_step(); return;
        }
    }
    
   /* protected void checkAttack() throws GameActionException {
    	underAttack= this.myRC.getHealth()<= RobotType.SOLDIER.maxHealth - RobotType.SOLDIER.attackPower;
    }*/
    
    protected void retreat() throws GameActionException {
    	//placeholder
    	Action action = new Action(BaseRobot.State.RALLY, rallyLoc, squadNumber);
    	this.actionQueue.addFirst(action);
    }
    
    protected void attack_pasture_step() throws GameActionException {
       	Action action = this.actionQueue.getFirst();
    	MapLocation target = action.targetLocation;
    	/*
    	if (this.myRC.getHealth() <= RobotType.SOLDIER.maxHealth - RobotType.SOLDIER.attackPower*4){
    		this.retreat();
    		return;
    	}
    	
    	if (this.myRC.canSenseSquare(target)) {
			GameObject onSquare = this.myRC.senseObjectAtLocation(target);
			if (onSquare==null) {
				System.out.println("job well done. pasture dead");
				int codemsg = this.myRC.readBroadcast(BaseRobot.SQUAD_BULLETIN_BASE + squadNumber);
				if (codemsg != 0) {
					ActionMessage msg = ActionMessage.decode(codemsg);
					Action newAction = msg.toAction();
					System.out.println("new target: " + newAction.targetLocation.x + " "+ newAction.targetLocation.y);
					
					this.actionQueue.removeFirst();
					this.actionQueue.add(newAction);

					this.myRC.broadcast(BaseRobot.SQUAD_BULLETIN_BASE+ squadNumber, 0);
					System.out.println("on to bigger and better things!");
				} else {
					System.out.println("Whoops, bulletin fail");
					MapLocation[] enemyPastrs = this.myRC.senseBroadcastingRobotLocations(this.enemyTeam);
					if (enemyPastrs.length != 0) {
						Action newAction = new Action(BaseRobot.State.ATTACK,enemyPastrs[0], squadNumber);
						this.actionQueue.removeFirst();
						this.actionQueue.add(newAction);
					}
				} return;
			}
		}
		*/
    	if(this.myRC.isActive()){
			if (this.myRC.getLocation().distanceSquaredTo(target)<10){
    			this.myRC.attackSquare(target);
    		} else if (directionTo(target)!= null){
    			this.myRC.move(directionTo(target));
    		}
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
    	//try to make a pasture at some location
    	//if a pasture is already there, become one of its herders
    	if (this.myRC.isActive()){
	    	Action action = this.actionQueue.getFirst();
	    	
	    	if (this.myRC.getLocation().equals(action.targetLocation)){
	    		this.myRC.construct(RobotType.PASTR);
	    		return;
	    	}
	    	if (this.myRC.canSenseSquare(action.targetLocation)){
		    	GameObject squattingRobot = this.myRC.senseObjectAtLocation(action.targetLocation); //if one of our PASTRs is already there
		    	if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam && this.myRC.senseRobotInfo((Robot)squattingRobot).type == RobotType.PASTR){
		    		ourPastrID = squattingRobot.getID(); //gets and stores the PASTR id
		    		pastureloc = action.targetLocation;
		    		Action newAction = new Action(BaseRobot.State.DEFENSE, pastureloc, ourPastrID);
		    		this.actionQueue.removeFirst();
		    		this.actionQueue.addFirst(newAction);
		    		return;
		    	}
	    	}
    		Direction dir = this.directionTo(action.targetLocation);
    		if (dir == null){
    			System.out.println("with new navigation, this should not happen");
    			this.myRC.construct(RobotType.PASTR);
    			return;
    		}
    		else {
    			if(action.targetLocation.distanceSquaredTo(this.myRC.getLocation()) < 16)
    				this.myRC.sneak(dir);
    			else 
    				this.myRC.move(dir);
    			return;
    		}
    	}
    }    
    
    protected void rally_step() throws GameActionException {
        // scout in squads; HQ assigns all scouts to the same pastr and rallying point. 
    	// Once at rallying point, if it senses other robots of its team in the vicinity, it waits.
    	// Otherwise it goes into attack mode.
    	
    	Action action = this.actionQueue.getFirst();
    	rallyLoc = action.targetLocation;
    	squadNumber = action.targetID;
    	
    	if (this.myRC.getLocation().distanceSquaredTo(rallyLoc)<5){ //if we're near the rally point
    		int numChannel = BaseRobot.SQUAD_BASE + squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_RALLYAMT_CHANNEL; //how many robots are already at the rally point    		
    		
    		//increment the count of robots already here
			int membersThere= this.myRC.readBroadcast(numChannel + this.squadNumber);
			if (!rallied){
    			System.out.println("Scout arrived, squad number " + squadNumber + ", " + membersThere + " members there too.");
    			membersThere++;
    			rallied= true;
    		}
			
    		this.myRC.broadcast(numChannel, membersThere);

    		if (membersThere >= MAX_SQUAD_SIZE || membersThere < 0){ //if the whole squad is there, or people have already left
    			int targetChannel = SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_ATTACKPT_CHANNEL;
    			int theTarget = this.myRC.readBroadcast(targetChannel);
    			
    			MapLocation nextTarget;
    			
    			if(theTarget == 0){ //if this squad doesn't have a target, assign one
    				nextTarget = get_next_attack_loc();
    				ActionMessage attackAction = new ActionMessage(BaseRobot.State.ATTACK, 0, nextTarget);
    				this.myRC.broadcast(targetChannel, attackAction.encode());
    			} else {
    				nextTarget = ActionMessage.decode(theTarget).targetLocation;
    			}
    			
        		Action newAction= new Action(BaseRobot.State.ATTACK, nextTarget, this.squadNumber);
        		this.actionQueue.removeFirst();
    			this.actionQueue.addFirst(newAction);
    			
    			//announce that we've left
    			this.myRC.broadcast(numChannel, -10);
    		}
    	} 

    	Direction dir = directionTo(rallyLoc);
    	if(this.myRC.isActive() && dir != null){
    		this.myRC.move(dir);
    	}	
    }
    
    protected MapLocation get_next_attack_loc() throws GameActionException{
    	
    	//placeholder
    	return this.myRC.senseEnemyHQLocation();    	
    }
    
    protected boolean loneRanger() throws GameActionException { //nobody is nearby
    	return this.myRC.senseNearbyGameObjects(Robot.class, 20, this.myTeam).length==0;
    }
    
    protected void gatherout_step() throws GameActionException{
    	if(this.myRC.isActive()){
    		Action action = this.actionQueue.getFirst();
    		MapLocation target = action.targetLocation;
    		
    		if(this.myRC.getLocation().distanceSquaredTo(target) < 3){
    			this.actionQueue.removeFirst();
    			Action newAction = new Action(BaseRobot.State.DEFENSE, targetLoc, ourPastrID);
    			this.actionQueue.addFirst(newAction);
    		} else {
    			Direction dir = this.directionTo(target);
    			if (dir != null)
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
        
        if(dir == Direction.NONE || dir == Direction.OMNI){
        	return null;
        }
        
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
