package swarm3;

import java.util.Arrays;
import java.util.LinkedList;

import battlecode.common.*;

public abstract class BaseRobot {
    
	enum State {
        DEFAULT, ATTACK, DEFEND, NOISE,
    };
	
	enum Strategy {COWVERT, FARMVILLE, DEFAULT};

    protected RobotController myRC;
    protected Team my_team;
    protected Team enemy_team;
    protected MapLocation my_hq_loc;
    protected MapLocation enemy_hq_loc;
    protected int max_dist;
    protected int ID;
    protected int order;
    protected int pastr_buffer;
	int map_width; int map_height;
	
	Strategy strategy = null;
   
    public static final int IDBOX_BASE = 10000; //store this robot's order in the array
    public static final int ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 1;
    public static final int SOLDIER_ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 2;
    public static final int PASTR_DISTRESS_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 5;
    public static final int SQUAD_RETREAT_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 10;
    public static final int ALLY_NUMBERS = GameConstants.BROADCAST_MAX_CHANNELS - 15;
    public static final int SQUAD_MESSAGE_CHANNEL = 100;
    
    public static final int STRATEGY_CHANNEL = 0;
    public static final int HQ_BROADCAST_CHANNEL = 1;
    public static final int CAUTION_CHANNEL = 2;
    public static final int COWVERT_OPERATIONS_CHANNEL = 3;
    public static final int LOC_CHANNEL = 4;
    public static final int[] LOC_CHANNELS = {4, 5, 6, 7};
    public static final int[] NOISE_DIBS_CHANNELS = {8, 9, 10, 11};
    public static final int[] PASTR_DIBS_CHANNELS = {12, 13, 14, 15};
    public static final int[] NOISE_ATTACK_CHANNELS = {16, 17, 18, 19};
    public static final int PASTR_COW_CHANNEL = 20;
    public static final int NOISE_DIBS_CHANNEL = 21;
    public static final int RANDOM_MAP_CHANNEL = 22; // 1 if random, 2 otherwise

    public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    
    public BaseRobot(RobotController myRC) throws GameActionException {
        this.myRC = myRC;
        
        this.my_team = this.myRC.getTeam();
        this.enemy_team = this.my_team.opponent();
		this.map_width = this.myRC.getMapWidth();
		this.map_height = this.myRC.getMapHeight();
        this.pastr_buffer = 10;
        
        this.my_hq_loc = this.myRC.senseHQLocation();
        this.enemy_hq_loc = this.myRC.senseEnemyHQLocation();
        this.ID = this.myRC.getRobot().getID();
        this.max_dist = this.myRC.getMapHeight() * this.myRC.getMapHeight() + this.myRC.getMapWidth() * this.myRC.getMapWidth();
        
        this.order = this.myRC.readBroadcast(BaseRobot.ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.ORDER_CHANNEL, this.order + 1);
		this.myRC.broadcast(IDBOX_BASE + this.ID, this.order);
    }

    public void run(){
        while (true){
            try {
                transition();
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
    			&& destination.distanceSquaredTo(this.enemy_hq_loc) >= 25);
    }

    public boolean isGoodLoc(MapLocation loc) {
    	return (this.myRC.senseTerrainTile(loc)==TerrainTile.NORMAL || this.myRC.senseTerrainTile(loc) == TerrainTile.ROAD);
    }
    
    public int idToOrder(int ID) throws GameActionException{
		return this.myRC.readBroadcast(IDBOX_BASE + ID);
    }

}
