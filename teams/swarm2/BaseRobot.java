package swarm2;

import java.util.Arrays;
import java.util.LinkedList;

import battlecode.common.*;

public abstract class BaseRobot {
    
    public enum State {
        DEFAULT, ATTACK, DEFEND
    };

    protected RobotController myRC;
    protected Team myTeam;
    protected Team enemyTeam;
    protected MapLocation myHQLoc;
    protected MapLocation enemyHQLoc;
    protected int maxDist;
    protected int ID;
    protected int order;
    protected int pastrBuffer;
   
    public static final int IDBOX_BASE = 10000; //store this robot's order in the array
    public static final int ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 1;
    public static final int SOLDIER_ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 2;
    public static final int PASTR_DISTRESS_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 5;
    public static final int SQUAD_RETREAT_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 10;
    public static final int ALLY_NUMBERS = GameConstants.BROADCAST_MAX_CHANNELS - 15;
    public static final int HQ_BROADCAST_CHANNEL = 0;
    public static final int SQUAD_MESSAGE_CHANNEL = 100;
    public static final int NOISE_OFFENSE_CHANNEL = 1000;
    public static final int PASTR_LOC_CHANNEL = 1;
    public static final int NOISE_LOC_CHANNEL = 2;
    
    protected double[][] spawnRates;
    protected double[][] locScores;

    public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    
    public BaseRobot(RobotController myRC) throws GameActionException {
        this.myRC = myRC;
        this.construct_core();
    }
    
    protected void construct_core() throws GameActionException {        
        this.myTeam = this.myRC.getTeam();
        this.enemyTeam = this.myTeam.opponent();
        this.pastrBuffer = 10;
        
        this.myHQLoc = this.myRC.senseHQLocation();
        this.enemyHQLoc = this.myRC.senseEnemyHQLocation();
        this.ID = this.myRC.getRobot().getID();
        this.maxDist = this.myRC.getMapHeight() * this.myRC.getMapHeight() + this.myRC.getMapWidth() * this.myRC.getMapWidth();
        
        this.order = this.myRC.readBroadcast(BaseRobot.ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.ORDER_CHANNEL, this.order + 1);
		this.myRC.broadcast(IDBOX_BASE + this.ID, this.order);
    }

    public void run(){
        while (true){
            try {
                this.transition();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void transition() throws GameActionException {
        this.setup();
        this.step();
        this.myRC.yield();
    }
    
    protected void setup() throws GameActionException {
    }
    
    protected void step() throws GameActionException {
    }
    
    protected boolean canMove(Direction dir) {
    	MapLocation destination = this.myRC.getLocation().add(dir);
    	
    	return (this.myRC.canMove(dir)
    			&& destination.distanceSquaredTo(this.enemyHQLoc) >= 25);
    }

    public boolean isGoodLoc(MapLocation loc) {
    	return (this.myRC.senseTerrainTile(loc)==TerrainTile.NORMAL || this.myRC.senseTerrainTile(loc) == TerrainTile.ROAD);
    }
    
    public int idToOrder(int ID) throws GameActionException{
		return this.myRC.readBroadcast(IDBOX_BASE + ID);
    }
	
	//Cheaper/better math functions
	protected float random(){
		float random_float =  (float) (this.ID * Math.random());
		return random_float - (int) random_float;
	}
	
    protected int abs(int a){
    	if (a>0)
    		return a;
    	else return -1*a;
    }
    
    protected int min(int a, int b){
    	if (a<b)
    		return a;
    	else return b;
    }
    
    protected int max(int a, int b){
    	if (a>b)
    		return a;
    	else return b;
    }
}
