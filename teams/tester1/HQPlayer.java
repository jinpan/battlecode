package tester1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.HashMap;

import attack.BaseRobot.State;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class HQPlayer extends BaseRobot {
	
	Direction toEnemy;
	MapLocation[] myCorners;
	ArrayList<MapLocation> pastrLocs0;
	ArrayList<PastureBlock> pastrBlocks;
	int counter=0;
	int deadZone;
	boolean safePastrs= false;
	
	double spawnThresh;
	
	int numRobots;
	int mapHeight; int mapWidth;
	double[][] spawnRates;
	double bestSpawnRate;
	int numBestSpawn;

    public HQPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
        this.spawnRates= this.myRC.senseCowGrowth();
        this.mapHeight= this.myRC.getMapHeight();
        this.mapWidth= this.myRC.getMapWidth();
        this.deadZone= max(this.mapWidth,  this.mapHeight)/4;
        
        double best= 0;
        int counter=0;
        for (int i=0; i<spawnRates.length; i++){
        	for (int j=0; j<spawnRates[i].length; j++){
        		if (best<spawnRates[i][j]){
        			best= spawnRates[i][j];
        			counter=0;
        		} if (best==spawnRates[i][j]){
        			counter++;
        		}
        	}//System.out.println(Arrays.toString(spawnRates[i]));
        }
        this.bestSpawnRate= best;
        this.spawnThresh= best*0.7;
        
        this.numBestSpawn= counter;
        this.pastrBlocks= new ArrayList<PastureBlock>();
        pastrBlocks.add(this.findPastureBlock());
        if (pastrBlocks.get(0)!=null){
        	this.pastrLocs0= this.findPastureBlock().pastrLocs(new ArrayList<MapLocation>());
        } else {
        	safePastrs=true;
        }
        
        this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
        this.numRobots = 1;
    }
    
    public int numPastures(){
    	return this.numBestSpawn/200;
    }
    
    public boolean isGoodLoc(int x, int y){
    	boolean xokay= x>=0 && x<this.mapWidth;
    	boolean yokay= y>=0 && y<this.mapHeight;
    	boolean inEnemySquare= abs(x-this.enemyHQLoc.x) <= this.mapWidth/5 && abs(y-this.enemyHQLoc.y)<= this.mapHeight/5;
    	return xokay && yokay && !inEnemySquare;
    }
    
    public PastureBlock findPastureBlock(){
    	int width=0; int height=0; 
    	int xadd=0; int yadd=0;
    	MapLocation vertex=null;
    	PastureBlock block = null;
    	//System.out.println(this.myHQLoc);
    	for (int i=1; i< 15; i++){
    		int y1= this.myHQLoc.y-i; 
    		int y2= this.myHQLoc.y+i;
    		int x1= this.myHQLoc.x-i; 
    		int x2= this.myHQLoc.x+i;
    		//System.out.println(x1+" "+x2+" "+y1+" "+y2);
    		for (int j=0; j<2*i+1; j++){
    			//System.out.println(spawnRates[y1][x1+j]);
    			//System.out.println(isGoodLoc(x1+j, y1));
    			if (isGoodLoc(x1+j, y1) && spawnRates[x1+j][y1]>=spawnThresh){
    				vertex= new MapLocation(x1+j, y1); 
    				xadd= 1; yadd= -1; 
    				width++; height++;
    				i= this.mapHeight/2; j=2*i;
    			}else if (isGoodLoc(x1+j, y2) && spawnRates[x1+j][y2]>=spawnThresh){
    				vertex= new MapLocation(x1+j, y2); xadd= 1; yadd= 1;
    				width++; height++;
    				i= this.mapHeight/2; j=2*i;
    			}else if (isGoodLoc(x1, y1+j) && spawnRates[x1][y1+j]>=spawnThresh){
    				vertex= new MapLocation(x1, y1+j); xadd= -1; yadd= 1;
    				width++; height++;
    				i= this.mapHeight/2; j=2*i;
    			}else if (isGoodLoc(x2, y1+j) && spawnRates[x2][y1+j]>=spawnThresh){
    				vertex= new MapLocation(x2, y1+j); xadd= 1; yadd= 1;
    				width++; height++;
    				i= this.mapHeight/2; j=2*i;
    			}
    		}
    	}
    	//System.out.println(vertex);
    	boolean alreadyDetected= false;
    	for (PastureBlock pastrblock: this.pastrBlocks){
    		if (pastrblock!=null && vertex== pastrblock.vertex){
    			alreadyDetected=true;
    			break;
    		}
    	}
    	if (vertex!= null && !alreadyDetected){
    		int a= vertex.x; int b= vertex.y;
    		for (int j=0; j<this.mapHeight; j++){
    			if (isGoodLoc(a, b+yadd*j) && spawnRates[a][b+yadd*j]>=spawnThresh){
    				width++;
    			} else {
    				j= this.mapHeight;
    			}
    		} for (int i=0; i<this.mapWidth; i++){
    			if (isGoodLoc(a+ xadd*i, b)&& spawnRates[a+xadd*i][b]>=spawnThresh){
    				height++;
    			} else{
    				i= this.mapWidth;
    			}
    		}
    		vertex= new MapLocation(min(a+width*xadd, a), min(b+height*yadd, b));
    		block= new PastureBlock(vertex, width-1, height-1);
    	} return block;
    	
    }
    
    
    public void assign_pasture(int order, int counter) throws GameActionException {
    	int channel= BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
    	MapLocation pastr=null;
    	if (counter>=0){
    		pastr= this.pastrLocs0.remove(0);
    	} else {
    		pastr= this.pastrLocs0.get(0);
    	}
    	ActionMessage msg= new ActionMessage(BaseRobot.State.PASTURIZE, 0, pastr);
    	this.myRC.broadcast(channel, (int) msg.encode());
    }
    
    public void step() throws GameActionException {
    	PastureBlock x= this.pastrBlocks.get(0);
    	System.out.println(x.vertex+" "+x.width+" "+x.height);
    	PastureBlock y= this.findPastureBlock();
    	if (y!=null){
    		System.out.println(y.vertex+" "+y.width+" "+y.height);
    	}//System.out.println(y);
    	//System.out.println(spawnRates.length+" "+this.mapWidth);
    	//System.out.println(spawnRates[0].length+" "+this.mapHeight);
    }
    
    /*@Override
    protected void step() throws GameActionException {
        if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
            this.spawn();
            ++this.numRobots;
        }
        
        if (!this.safePastrs && this.pastrLocs0.size()==0 && this.pastrBlocks.size()!=0){
        	PastureBlock nextblock= this.findPastureBlock();
        	if (nextblock!=null){
        		this.pastrBlocks.add(nextblock);
        		this.pastrLocs0= nextblock.pastrLocs(this.pastrLocs0);
        	} else {
        		this.safePastrs=true;
        	}
        }
        
        int dist = this.myRC.getMapHeight() * this.myRC.getMapHeight() + this.myRC.getMapWidth() * this.myRC.getMapWidth();
        MapLocation closestTarget = null;
        
        MapLocation[] targets = this.myRC.sensePastrLocations(this.enemyTeam);
        for (int i=0; i<targets.length; ++i){
        	if (targets[i].distanceSquaredTo(this.enemyHQLoc) > 15){
        		if ((targets[i].distanceSquaredTo(this.myHQLoc) < dist)){
        			closestTarget = targets[i];
        		}
				EnemyProfileMessage enemyProf = new EnemyProfileMessage(0, 200, targets[i], Clock.getRoundNum());
				this.squad_send(BaseRobot.SQUAD_LOW_HITLIST + 2 * i, enemyProf.encode());
        	}
        }
        
        HashMap<Integer, Integer> idToOrder = new HashMap<Integer, Integer>();
        int channel, id;
        for (int i=1; i<this.numRobots; ++i){
        	channel = BaseRobot.get_outbox_channel(i, BaseRobot.OUTBOX_ID_CHANNEL);
        	id = this.myRC.readBroadcast(channel);
        	idToOrder.put(id, i);
        }
        
        int order;
        StateMessage state;
        
        if (closestTarget != null){
	        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class)) {
	        	if (idToOrder.get(robot.getID()) == null){
	        		continue;
	        	}
	        	order = idToOrder.get(robot.getID());
	        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
	        	state = StateMessage.decode(this.myRC.readBroadcast(channel));
	        	if (state.state == BaseRobot.State.DEFAULT){
	        		if (this.safePastrs){
	        			ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
	        			channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
	        			this.myRC.broadcast(channel, (int) action.encode());
	        		} else {
	        			this.assign_pasture(order, counter);
	        			counter++;
	        			if (counter>0)
	        				counter=0;
	        		}
	        	}
	        }
        }
    }
    
    private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(this.toEnemy)) == null){
			this.myRC.spawn(this.toEnemy);
			return true;
		}
		else {
	        for (Direction dir: BaseRobot.dirs){
	            if (dir != this.toEnemy
	            		&& this.myRC.senseObjectAtLocation(this.myHQLoc.add(dir)) == null){
	                this.myRC.spawn(dir);
	                return true;
	            }
	        }
		}
    	return false;
    }*/

}
