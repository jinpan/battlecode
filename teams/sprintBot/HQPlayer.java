package sprintBot;

import java.util.ArrayList;

import sprintBot.BaseRobot;
import sprintBot.ActionMessage;
import sprintBot.PastureBlock;
import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	
	Direction toEnemy;
	int distToEnemy;
	MapLocation[] myCorners;
	ArrayList<MapLocation> pastrLocs0;
	ArrayList<PastureBlock> pastrBlocks;
	double spawnThresh;
	int mapHeight; int mapWidth;
	double[][] spawnRates;
	double bestSpawnRate;
	int numBestSpawn;
	
	boolean offensive = false;
	
	int numRobots;
	int armySize; //number of robots we've gathered around the HQ

    public static final int MIN_ARMY_SIZE= 5;
    public static final int PASTURE_UNIT_SIZE= 3;
	
	public HQPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
        this.spawnRates= this.myRC.senseCowGrowth();
        this.mapHeight= this.myRC.getMapHeight();
        this.mapWidth= this.myRC.getMapWidth();
        
        this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
        this.distToEnemy= this.myHQLoc.distanceSquaredTo(this.enemyHQLoc);
        this.numRobots = 1;
        this.armySize = 0;
        
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
        	}
        }
        this.bestSpawnRate= best;
        this.spawnThresh= best*0.6;
        
        this.numBestSpawn= counter;
        this.pastrBlocks= new ArrayList<PastureBlock>();
        this.pastrLocs0= new ArrayList<MapLocation>();
        //this.detectedVertices= new ArrayList<MapLocation>();
        
        this.findPastureBlock();
    }

    @Override
    protected void step() throws GameActionException {
    	Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}    	
    	
        if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
            this.spawn();
            ++this.numRobots;
            ++this.armySize;
        }
        
        int dist = maxDist;
        MapLocation closestTarget = null;
        
        //finds enemy pastures and posts locations on message board
        MapLocation[] targets = this.myRC.sensePastrLocations(this.enemyTeam);
        for (int i=0; i<targets.length; ++i){
        	if (targets[i].distanceSquaredTo(this.enemyHQLoc) > 24){
        		if ((targets[i].distanceSquaredTo(this.myHQLoc) < dist)){
        			closestTarget = targets[i];
        		}
				EnemyProfileMessage enemyProf = new EnemyProfileMessage(0, 200, targets[i], Clock.getRoundNum());
				this.squad_send(BaseRobot.SQUAD_BLDG_HITLIST + 2 * i, enemyProf.encode());
        	}
        }
        
        int order, channel;
        StateMessage state;
        
        //if there's a pasture to attack, do so
        if(closestTarget != null){
        	
        	if (armySize>= this.MIN_ARMY_SIZE){
        		for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class)) {
        			if (idToOrder(robot.getID()) == 0){
        				continue;
        			}
        			order = idToOrder(robot.getID());
        			channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        			state = StateMessage.decode(this.myRC.readBroadcast(channel));
        			if (state.state == BaseRobot.State.DEFAULT){
        				ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
        				channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        				this.myRC.broadcast(channel, (int) action.encode());
        			}
        		}
        		armySize = 0;
        	}
        }
        
        else { //if there are no pastures to attack, we build our own.
        	MapLocation pastrLoc = null;
        	MapLocation[] myPastrs= this.myRC.sensePastrLocations(this.myTeam);

        	if (this.pastrLocs0.size()<= myPastrs.length){
        		this.findPastureBlock();
        	} if (this.pastrLocs0.size()>myPastrs.length){
        		pastrLoc=this.pastrLocs0.get(myPastrs.length);
        	}

        	for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class)) {
        		if (idToOrder(robot.getID()) == 0){
        			System.out.println("couldn't find order");
        			continue;
        		}
        		order = idToOrder(robot.getID());
        		channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        		state = StateMessage.decode(this.myRC.readBroadcast(channel));
        		if (state.state == BaseRobot.State.DEFAULT && pastrLoc!=null){
        			ActionMessage action = new ActionMessage(BaseRobot.State.PASTURIZE, 0, pastrLoc);
        			channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        			this.myRC.broadcast(channel, (int) action.encode());
        			armySize--;
        		}
        	}
        }    
    }
    
    private void assign_job() throws GameActionException{
    	
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
    }
    
    private boolean shoot(Robot[] nearbyEnemies) throws GameActionException{
    	MapLocation curloc;
    	
    	for(Robot r : nearbyEnemies){ //try to hit something directly
    		curloc = this.myRC.senseRobotInfo(r).location;
    		if(this.myRC.canAttackSquare(curloc)){
    			this.myRC.attackSquare(curloc);
    			return true;
    		}
    	}
    	
    	for(Robot r : nearbyEnemies){ //try to hit something for splash damage
    		curloc = this.myRC.senseRobotInfo(r).location;
    		curloc = curloc.add(curloc.directionTo(this.myHQLoc));
    		if(this.myRC.canAttackSquare(curloc)){
    			this.myRC.attackSquare(curloc);
    			return true;
    		}
    	}
    	
    	return false; //give up    	
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
    
    public boolean notOverlapping(MapLocation loc){
    	for (PastureBlock block: this.pastrBlocks){
    		if (block.contains(loc)){
    			return false;
    		}
    	} return true;
    }
    
    public PastureBlock findPastureBlock(){
    	int width=0; int height=0; 
    	int xadd=0; int yadd=0;
    	MapLocation vertex=null;
    	PastureBlock block = null;
    	
    	int i=1; 

		while (vertex==null && i<(int)this.mapHeight/2){
    		int y1= this.myHQLoc.y-i; 
    		int y2= this.myHQLoc.y+i;
    		int x1= this.myHQLoc.x-i; 
    		int x2= this.myHQLoc.x+i;
    		//System.out.println(x1+" "+x2+" "+y1+" "+y2);
    		for (int j=0; j<2*i+1; j++){
    			//System.out.println(spawnRates[y1][x1+j]);
    			//System.out.println(isGoodLoc(x1+j, y1));
    			if ((width==0&&height==0) && isGoodLoc(x1+j, y1) && spawnRates[x1+j][y1]>=spawnThresh){
    				MapLocation temp = new MapLocation(x1+j, y1);
    				if (notOverlapping(temp)){
    					xadd= 1; yadd= -1; 
    					width=1; height=1;
    					vertex= temp;
    				} else {
    					vertex=null;
    				}
    			} else if ((width==0&&height==0) && isGoodLoc(x1+j, y2) && spawnRates[x1+j][y2]>=spawnThresh){
    				MapLocation temp= new MapLocation(x1+j, y2);
    				if (notOverlapping(temp)){
    					xadd= 1; yadd= 1; 
    					width=1; height=1;
    					vertex= temp;
    				} else {
    					vertex=null;
    				}
    			} else if ((width==0&&height==0) && isGoodLoc(x1, y1+j) && spawnRates[x1][y1+j]>=spawnThresh){
    				MapLocation temp = new MapLocation(x1, y1+j);
    				if (notOverlapping(temp)){
    					xadd= -1; yadd= 1; 
    					width=1; height=1;
    					vertex= temp;
    				} else {
    					vertex=null;
    				}
    			} else if ((width==0&&height==0) && isGoodLoc(x2, y1+j) && spawnRates[x2][y1+j]>=spawnThresh){
    				MapLocation temp= new MapLocation(x2, y1+j);
    				if (notOverlapping(temp)){
    					xadd= 1; yadd= 1; 
    					width=1; height=1;
    					vertex= temp;
    				} else {
    					vertex=null;
    				}
    			}
    		}i++;
    	}
    	if (vertex!= null){
    		int a= vertex.x; int b= vertex.y;
    		for (int j=0; j<this.mapHeight/2; j++){
    			if (isGoodLoc(a, b+yadd*j) && spawnRates[a][b+yadd*j]>=spawnThresh){
    				height++;
    			} else {
    				j= this.mapHeight;
    			}
    		} for (int k=0; k<this.mapWidth/2; k++){
    			if (isGoodLoc(a+ xadd*k, b)&& spawnRates[a+xadd*k][b]>=spawnThresh){
    				width++;
    			} else{
    				k= this.mapWidth;
    			}
    		}
    		//this.detectedVertices.add(vertex);
    		//System.out.println(this.detectedVertices.get(this.detectedVertices.size()-1));
    		vertex= new MapLocation(min(a+width*xadd, a), min(b+height*yadd, b));
    		block= new PastureBlock(vertex, width-1, height-1, 10);
    		this.pastrBlocks.add(block);
        	block.pastrLocs(this.pastrLocs0);
    	} 
    	return block;
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

}
