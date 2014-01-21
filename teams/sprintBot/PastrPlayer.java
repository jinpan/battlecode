package sprintBot;

import java.util.ArrayList;
import sprintBot.StateMessage;
import sprintBot.BaseRobot;
import battlecode.common.*;

public class PastrPlayer extends BaseRobot{
	
	ArrayList<Integer> myOrders = new ArrayList<Integer>(); //keeps track of the orders of robots gathering to it
	int[] dirCooldown = new int[8]; //avoid sending robots in the same direction multiple times
	MapLocation[] extrema = new MapLocation[8]; //how far we can go in each direction
	int[] timeToExtrema = new int[8]; //how long it takes to get to each of those locations
	int cur = 0; //current direction
	
    public PastrPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        get_herding_extrema();
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
        	
    		if (thestate.state == BaseRobot.State.DEFEND){ //claim a defending robot next to us as our own, and give it a job  
        		channel = BaseRobot.get_inbox_channel(robotOrder, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        		
        		if(this.myRC.readBroadcast(channel) == 0){
            		ActionMessage action;
            		MapLocation dest = get_good_herding_loc();
        			action = new ActionMessage(BaseRobot.State.HERD, this.ID, dest);
        			this.myRC.broadcast(channel, (int)action.encode());
        		}
    		}
    	}
    }
    
    protected void get_herding_extrema(){ //this finds how far the robots can go in any direction
    	double[][] cowGrowth = this.myRC.senseCowGrowth(); //cowGrowth[a][b] is growth at location (a, b)
    	MapLocation base = this.myRC.getLocation();
    	int timeStep = 0;
    	int timeLim = 250;
    	
    	for(int i = 0; i < 8; i++){
    		extrema[i] = base;
    		timeToExtrema[i] = 1;
    		
    		TerrainTile curTerrain = this.myRC.senseTerrainTile(extrema[i]);
    		while(curTerrain == TerrainTile.NORMAL || curTerrain == TerrainTile.ROAD){
    			if(timeToExtrema[i] > timeLim || cowGrowth[extrema[i].x][extrema[i].y] == 0)
    				break;
    			
    			extrema[i] = extrema[i].add(dirs[i]);
    			
    			if(curTerrain == TerrainTile.NORMAL)
    				timeStep = 10;
    			else
    				timeStep = 7;
    			if(dirs[i].isDiagonal())
    				timeStep = timeStep * 7 / 5;
    			
    			curTerrain = this.myRC.senseTerrainTile(extrema[i]);
    			timeToExtrema[i] += timeStep;
    		}
    		
    		extrema[i] = extrema[i].add(dirs[i].opposite());
    	}
    }
    
    protected MapLocation get_good_herding_loc() throws GameActionException{   
    	cur = (cur + 1) % 8;
    	return extrema[cur];
    }
}
