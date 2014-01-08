package pastrBasic;

import java.util.ArrayList;

import battlecode.common.*;


public class PastrPlayer extends BaseRobot{
	
	ArrayList<Integer> myOrders = new ArrayList<Integer>(); //keeps track of the orders of robots gathering to it
	
    public PastrPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
    }
    
    protected void step() throws GameActionException{
    	Robot[] nearbyHerders = myRC.senseNearbyGameObjects(Robot.class, 5, myTeam);
    	
    	int robotOrder = 0;
    	
    	for(Robot robot : nearbyHerders){
    		//finds robot order by search, because there doesn't seem to be a better way to do this
    		robotOrder = 0;
    		int channel = 0;
    		int id = 0;
    		
    		while(id != robot.getID()){
    			robotOrder++;
    			channel = BaseRobot.get_outbox_channel(robotOrder, BaseRobot.OUTBOX_ID_CHANNEL);
    			id = this.myRC.readBroadcast(channel);
    		}
    		
    		if(!myOrders.contains(id))
    			myOrders.add(id);
    		
        	int outchannel = BaseRobot.get_outbox_channel(robotOrder, BaseRobot.OUTBOX_STATE_CHANNEL);
        	StateMessage thestate = StateMessage.decode(this.myRC.readBroadcast(outchannel));
    		this.myRC.setIndicatorString(1, thestate.myState.toString());
        	
    		if (thestate.myState == BaseRobot.State.DEFENSE){
        		ActionMessage action;
        		MapLocation dest = myRC.getLocation().add(dirs[(int)(Math.random()*8)], 10);
        		
        		action = new ActionMessage(BaseRobot.State.GATHEROUT, this.ID, dest);
        		this.myRC.broadcast(BaseRobot.get_inbox_channel(robotOrder, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL), action.encode());
    		}
    		
    	}
    	/*
    	myRC.setIndicatorString(0, Integer.toString(nearbyHerders.length));
    	if(nearbyHerders.length > 0){
    		myRC.setIndicatorString(1, Integer.toString(robotOrder));
    	}
    	*/
    }
}
