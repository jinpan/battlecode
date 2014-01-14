package sprintBot;

import java.util.Arrays;
import java.util.LinkedList;

import battlecode.common.*;

public abstract class BaseRobot {
    
    public enum State {
        DEFAULT, ATTACK, DEFEND, HERD, PASTURIZE
    };
    
    protected State myState = State.DEFAULT;
    protected Action newAction;
    protected State startState;

    protected RobotController myRC;
    protected Team myTeam;
    protected Team enemyTeam;
    protected MapLocation myHQLoc;
    protected MapLocation enemyHQLoc;
    protected int maxDist;
    protected int ID;
    protected int order;
    
    protected MapLocation previousLoc;
    protected MapLocation previousLoc2;
    
    public static final int INBOX_BASE = 0;
    public static final int INBOX_SIZE = 10;
    public static final int OUTBOX_BASE = 2000;
    public static final int OUTBOX_SIZE = 10;
    public static final int SQUAD_BASE = 4000;
    public static final int ENEMY_MEMORY_LEN = 15;
    public static final int SQUAD_SOLD_HITLIST = 0;
    public static final int SQUAD_BLDG_HITLIST = 50;
    
    public static final int INBOX_ACTIONMESSAGE_CHANNEL = 0;
    public static final int OUTBOX_ID_CHANNEL = 0;
    public static final int OUTBOX_STATE_CHANNEL = 1;
    public static final int IDBOX_BASE = 10000; //store this robot's order in the array
    public static final int PASTR_HERDERS_BASE = 20000; //keeps track of the number of herders for pasture
    
    
    public static final int ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 1;
    public static final int SOLDIER_ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 2;
    public static final int PASTR_ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 3;
    public static final int PASTR_BUILDING_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 4;
    
    protected int inbox;
    protected boolean underAttack;
    
    protected LinkedList<Action> actionQueue;
    
    protected double[][] spawnRates;
    protected double[][] locScores;
    protected boolean[][] couldBeVoid;

    public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    
    public BaseRobot(RobotController myRC) throws GameActionException {
        this.myRC = myRC;
        
        this.construct_core();
    }
    
    public BaseRobot(RobotController myRC, State myState) throws GameActionException {
        this.myRC = myRC;
        this.myState = myState;
        
        this.construct_core();
    }
    
    protected void construct_core() throws GameActionException {        
        this.myTeam = this.myRC.getTeam();
        this.enemyTeam = this.myTeam.opponent();
        
        this.myHQLoc = this.myRC.senseHQLocation();
        this.enemyHQLoc = this.myRC.senseEnemyHQLocation();
        this.ID = this.myRC.getRobot().getID();
        this.previousLoc = this.myRC.getLocation();
        this.previousLoc2 = this.myRC.getLocation();
        this.actionQueue = new LinkedList<Action>();
        this.maxDist = this.myRC.getMapHeight() * this.myRC.getMapHeight() + this.myRC.getMapWidth() * this.myRC.getMapWidth();
        
        this.order = this.myRC.readBroadcast(BaseRobot.ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.ORDER_CHANNEL, this.order + 1);
		this.myRC.broadcast(IDBOX_BASE + this.ID, this.order);
        this.myRC.broadcast(this.get_outbox_channel(BaseRobot.OUTBOX_ID_CHANNEL), this.ID);
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
    	this.myRC.setIndicatorString(0, this.previousLoc.toString());
        this.setup();
        this.step();
        this.teardown();
        this.yield();
    }
    
    /*
     * Called at the start of each turn.  Robot should check its inbox for new actions to do.
     */
    protected void setup() throws GameActionException {
    	this.inbox = this.receive(BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
    	if (inbox != 0){
			ActionMessage msg = ActionMessage.decode(this.inbox);
			this.newAction = msg.toAction();
			this.myRC.broadcast(this.get_inbox_channel(BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL), 0);
    	}
    	
    	if (this.actionQueue.size() > 0){
    		this.myState = this.actionQueue.getFirst().state;
    	}
		
		this.startState = this.myState;
    }
    
    /*
     * Should be overridden in children
     */
    protected void step() throws GameActionException {
        
    }
    
    protected void teardown() throws GameActionException {
    	if (this.startState != this.myState){
	        StateMessage message = new StateMessage(this.myState);
	        this.myRC.broadcast(this.get_outbox_channel(BaseRobot.OUTBOX_STATE_CHANNEL), (int) message.encode());
    	}
    }
    
    /*
     * RobotController "overrides"
     */
    protected void yield() throws GameActionException {
        this.myRC.yield();
    }
    
    protected boolean canMove(Direction dir) {
    	MapLocation destination = this.myRC.getLocation().add(dir);
    	
    	return (this.myRC.canMove(dir)
    			&& !destination.equals(this.previousLoc)
    			&& !destination.equals(this.previousLoc2)
    			&& destination.distanceSquaredTo(this.enemyHQLoc) > 24);
    }
    
    protected void move(Direction dir) throws GameActionException {
    	this.previousLoc2 = this.previousLoc;
    	this.previousLoc = this.previousLoc.add(dir);
    	this.myRC.move(dir);
    }
    
    protected void sneak(Direction dir) throws GameActionException {
    	this.previousLoc2 = this.previousLoc;
    	this.previousLoc = this.previousLoc.add(dir);
    	this.myRC.sneak(dir);
    }

    /*
     * Returns the message intended for this robot
     */
    protected int receive(int channel) throws GameActionException {
    	return this.myRC.readBroadcast(this.get_inbox_channel(channel));
    }
    
    protected void squad_send(int channel, long msg) throws GameActionException {
    	this.myRC.broadcast(BaseRobot.SQUAD_BASE + channel, (int) (msg >> 32));
    	this.myRC.broadcast(BaseRobot.SQUAD_BASE + channel + 1, (int) (msg & 0xFFFFFFFF));
    }
    
    protected void send(BaseRobot recipient, int channel, int msg) throws GameActionException {
    	assert(channel < BaseRobot.INBOX_SIZE);
    	this.myRC.broadcast(recipient.order * BaseRobot.INBOX_SIZE + channel, msg);
    }
    
    public static int get_inbox_channel(int order, int channel){
    	assert (channel < BaseRobot.INBOX_SIZE);
    	return BaseRobot.INBOX_BASE + order * BaseRobot.INBOX_SIZE + channel;
    }
    
    protected int get_inbox_channel(int channel) {
    	assert (channel < BaseRobot.INBOX_SIZE);
    	return BaseRobot.INBOX_BASE + this.order * BaseRobot.INBOX_SIZE + channel;
    }
    
    public static int get_outbox_channel(int order, int channel) {
    	assert (channel < BaseRobot.OUTBOX_SIZE);
    	return BaseRobot.OUTBOX_BASE + order * BaseRobot.OUTBOX_SIZE + channel;
    }
    
    protected int get_outbox_channel(int channel) {
    	assert (channel < BaseRobot.OUTBOX_SIZE);
    	return BaseRobot.OUTBOX_BASE + this.order * BaseRobot.OUTBOX_SIZE + channel;
    }
    
    protected float random(){
        float random_float =  (float) (this.ID * Math.random());
        return random_float - (int) random_float;
    }
    
	int idToOrder(int ID) throws GameActionException{
		return this.myRC.readBroadcast(IDBOX_BASE + ID);
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
