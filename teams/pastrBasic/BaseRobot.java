package pastrBasic;

import java.util.LinkedList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.GameConstants;

public abstract class BaseRobot {
    
    public enum State {
        DEFAULT, ATTACK, DEFENSE, PASTURE, SCOUT,
        ATTACKHIGH, DEFENSEHIGH, PASTUREHIGH, SCOUTHIGH, 
        GATHEROUT
    };
    
    protected RobotController myRC;
    protected State myState = State.DEFAULT;
    protected State startState;
    protected Team myTeam;
    protected Team enemyTeam;
    protected MapLocation myHQLoc;
    protected MapLocation enemyHQLoc;
    protected int ID;
    protected int order;
    
    public static final int INBOX_BASE = 0;
    public static final int INBOX_SIZE = 10;
    public static final int OUTBOX_BASE = 1000;
    public static final int OUTBOX_SIZE = 10;
    
    public static final int IDBOX_BASE = 10000; //store this robot's order in the array
    
    public static final int SQUAD_ID_BASE = 20000;//where the HQ posts who's in whose squad
    public static final int SQUAD_LOC_BASE = 20010;//where the squad members post whether or not they're at the rally point
    public static final int SQUAD_BULLETIN_BASE = 20020;//where the HQ posts the enemy pasture assignments
    
    public static final int INBOX_ACTIONMESSAGE_CHANNEL = 0;
    public static final int OUTBOX_ID_CHANNEL = 0;
    public static final int OUTBOX_STATE_CHANNEL = 1;
    public static final int ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 1;
    
    protected static final int MAX_PASTURES = 2; 
	protected static final int RALLY_DISTANCE = 5; //how far from target enemy squads will rally
	protected static final int NUM_SQUADS = 10;
    
	protected int inbox;
    protected boolean underAttack;
    
    protected LinkedList<Action> actionQueue;

    public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    
    public BaseRobot(RobotController myRC) throws GameActionException {
        this.myRC = myRC;
        
        this.construct_core();
        if(myRC.getType() != RobotType.PASTR)
        	this.establish_order();
    }
    
    public BaseRobot(RobotController myRC, State myState) throws GameActionException {
        this.myRC = myRC;
        this.myState = myState;
        
        this.construct_core();
        if(myRC.getType() != RobotType.PASTR)
        	this.establish_order();
    }
    
    protected void construct_core() throws GameActionException {        
        this.myTeam = this.myRC.getTeam();
        this.enemyTeam = this.myTeam.opponent();
        
        this.myHQLoc = this.myRC.senseHQLocation();
        this.enemyHQLoc = this.myRC.senseEnemyHQLocation();
        this.ID = this.myRC.getRobot().getID();
        this.actionQueue = new LinkedList<Action>();
        this.teardown();

        this.myRC.setIndicatorString(0, String.valueOf(this.ID));
    }
    
    protected void establish_order() throws GameActionException{
        this.order = this.myRC.readBroadcast(BaseRobot.ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.ORDER_CHANNEL, this.order + 1);
        this.myRC.broadcast(this.get_outbox_channel(BaseRobot.OUTBOX_ID_CHANNEL), this.ID);
        this.myRC.broadcast(IDBOX_BASE + this.ID, this.order);
        
        this.myRC.setIndicatorString(1, String.valueOf(this.order));
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
			Action action = msg.toAction();
			if (action.myState.name().contains("HIGH") && (this.actionQueue.size() == 0 || !action.isEqual(this.actionQueue.getFirst()))){
				this.actionQueue.addFirst(action);
			}
			else if(this.actionQueue.size() == 0 || !action.isEqual(this.actionQueue.getLast())) {
				this.actionQueue.addLast(action);
			}
			
			//remove this action once we read it
			myRC.broadcast(this.get_inbox_channel(BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL), 0);
    	}
    	
    	myRC.setIndicatorString(2, Integer.toString(actionQueue.size()));
    	if (this.actionQueue.size() > 0){
    		this.myState = this.actionQueue.getFirst().myState;
    	}
		
		this.startState = this.myState;
		this.myRC.setIndicatorString(0, this.myState.toString());
    }
    
    /*
     * Should be overridden in children
     */
    protected void step() throws GameActionException {
        
    }
    
    protected void teardown() throws GameActionException {
        StateMessage message = new StateMessage(this.myState);
	    this.myRC.broadcast(this.get_outbox_channel(BaseRobot.OUTBOX_STATE_CHANNEL), message.encode());
    }
    
    protected void yield() throws GameActionException {
        
        this.myRC.yield();
    }

    /*
     * Returns the message intended for this robot
     */
    protected int receive(int channel) throws GameActionException {
    	return this.myRC.readBroadcast(this.get_inbox_channel(channel));
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
}
