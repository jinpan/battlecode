package team007micro;

import java.util.ArrayList;

import team007micro.BaseRobot;
import team007micro.StateMessage;
import battlecode.common.*;

public class PastrPlayer extends BaseRobot{
	
	ArrayList<Integer> myOrders = new ArrayList<Integer>(); //keeps track of the orders of robots gathering to it
	int cur = 0; //current direction
	
    public PastrPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
    }
    
    protected void step() throws GameActionException{
    	Robot[] nearbyHerders = myRC.senseNearbyGameObjects(Robot.class, 5, myTeam);
    	
    	int robotOrder, channel, id;
    	
    	for(Robot robot : nearbyHerders){
    		id = robot.getID();
    		robotOrder = idToOrder(id);
    		
    		if(!myOrders.contains(id))
    			myOrders.add(id);
    		
        	channel = BaseRobot.get_outbox_channel(robotOrder, BaseRobot.OUTBOX_STATE_CHANNEL);
        	StateMessage thestate = StateMessage.decode(this.myRC.readBroadcast(channel));
    	}
    }
}
