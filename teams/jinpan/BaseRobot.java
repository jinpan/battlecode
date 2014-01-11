package jinpan;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.GameConstants;
import battlecode.common.TerrainTile;

public abstract class BaseRobot {
    
    public enum State {
        DEFAULT, ATTACK, DEFENSE, PASTURE, SCOUT,
        ATTACKHIGH, DEFENSEHIGH, PASTUREHIGH, SCOUTHIGH,
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
    
    public static final int INBOX_ACTIONMESSAGE_CHANNEL = 0;
    public static final int OUTBOX_ID_CHANNEL = 0;
    public static final int OUTBOX_STATE_CHANNEL = 1;
    public static final int ORDER_CHANNEL = GameConstants.BROADCAST_MAX_CHANNELS - 1;
    public static final int MAX_OPT_PASTURES = 5;
    
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
        this.actionQueue = new LinkedList<Action>();
        
        this.order = this.myRC.readBroadcast(BaseRobot.ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.ORDER_CHANNEL, this.order + 1);

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
			if (action.myState.name().contains("HIGH")){
				this.actionQueue.addFirst(action);
			}
			else {
				this.actionQueue.addLast(action);
			}
    	}
    	
    	if (this.actionQueue.size() > 0){
    		this.myState = this.actionQueue.getFirst().myState;
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
	        this.myRC.broadcast(this.get_outbox_channel(BaseRobot.OUTBOX_STATE_CHANNEL), message.encode());
    	}
    }
    
    protected void yield() throws GameActionException {
        
        this.myRC.yield();
    }
    
    /*
    protected double[][] computeLocScores() {
    	// explicit loops for bytecode optimization, partial updates
    	double[][] result = new double[this.myRC.getMapWidth()][this.myRC.getMapHeight()];

    	double[] tmp_cols = new double[3];
    	int idx, idx2, idx3, idx4;
    	tmp_cols[0] = this.spawnRates[0][0] + this.spawnRates[0][1] + this.spawnRates[0][2];
    	tmp_cols[0] = this.spawnRates[1][0] + this.spawnRates[1][1] + this.spawnRates[1][2];
    	tmp_cols[0] = this.spawnRates[2][0] + this.spawnRates[2][1] + this.spawnRates[2][2];

    	result[1][1] = tmp_cols[0] + tmp_cols[1] + tmp_cols[2];
    	for (int i=2; i<this.myRC.getMapWidth()-1; ++i){
			idx = (i-2) % 3;
    		result[i][1] = result[i-1][1] - tmp_cols[idx];
    		tmp_cols[idx] = this.spawnRates[i+1][0] + this.spawnRates[i+1][1] + this.spawnRates[i+1][2];
    		result[i][1] += tmp_cols[idx];
    	}
    	
    	double[] tmp_rows = new double[3];
    	for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
    		idx2 = i-1; idx3 = i+1;
    		tmp_rows[0] = this.spawnRates[idx2][0] + this.spawnRates[i][0] + this.spawnRates[idx3][0];
    		tmp_rows[1] = this.spawnRates[idx2][1] + this.spawnRates[i][1] + this.spawnRates[idx3][1];
    		tmp_rows[2] = this.spawnRates[idx2][2] + this.spawnRates[i][2] + this.spawnRates[idx3][2];
    		result[i][1] = tmp_rows[0] + tmp_rows[1] + tmp_rows[2];
    		
    		for (int j=2; j<this.myRC.getMapHeight()-1; ++j){
    			idx = (j-2) % 3;
    			idx4 = j-1;
        		result[i][j] = result[i][idx4] - tmp_rows[idx];
        		tmp_rows[idx] = this.spawnRates[idx2][idx4] + this.spawnRates[i][idx4] + this.spawnRates[idx3][idx4];
        		result[i][j] += tmp_cols[idx];
    		}
    	}
    	
    	return result;
    }
    */
    
    protected double[][] computeLocScores() {
    	double[][] result = new double[this.myRC.getMapWidth()][this.myRC.getMapHeight()];
    	for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
    		for (int j=1; j<this.myRC.getMapHeight()-1; ++j){
    			for (int a=-1; a<2; ++a){
    				for (int b=-1; b<2; ++b){
    					result[i][j] += this.spawnRates[i-a][j-b];
    				}
    			}
    		}
    	}
    	return result;
    }
    
    protected double dist_transform(int distM, int distE){
    	if (distM < 25 || distE < 25){
    		return 0;
    	}
    	else {
    		return (double) distE / (double) distM;
    	}
    }
    
    protected double[][] computeLocScoresDist() {
    	// explicit loops for bytecode optimization, partial updates
    	double[][] result = new double[this.myRC.getMapWidth()][this.myRC.getMapHeight()];

    	double[] tmp_cols = new double[3];
    	int idx, idx2, idx3, idx4;
    	double dist;
    	tmp_cols[0] = this.spawnRates[0][0] + this.spawnRates[0][1] + this.spawnRates[0][2];
    	tmp_cols[0] = this.spawnRates[1][0] + this.spawnRates[1][1] + this.spawnRates[1][2];
    	tmp_cols[0] = this.spawnRates[2][0] + this.spawnRates[2][1] + this.spawnRates[2][2];

    	dist = dist_transform((this.myHQLoc.x - 1) * (this.myHQLoc.x - 1) + (this.myHQLoc.y - 1) * (this.myHQLoc.y - 1),
    							(this.enemyHQLoc.x - 1) * (this.enemyHQLoc.x - 1) + (this.enemyHQLoc.y - 1) * (this.enemyHQLoc.y - 1));

    	result[1][1] = (tmp_cols[0] + tmp_cols[1] + tmp_cols[2]) * dist;
    	for (int i=2; i<this.myRC.getMapWidth()-1; ++i){
			idx = (i-2) % 3;
			if (dist == 0){
				result[i][1] = tmp_cols[0] + tmp_cols[1] + tmp_cols[2] - tmp_cols[idx];
			}
			else {
				result[i][1] = result[i-1][1] / dist - tmp_cols[idx];	
			}
    		tmp_cols[idx] = this.spawnRates[i+1][0] + this.spawnRates[i+1][1] + this.spawnRates[i+1][2];
    		dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - 1) * (this.myHQLoc.y - 1),
    								(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - 1) * (this.enemyHQLoc.y - 1));

    		result[i][1] += tmp_cols[idx];
    		result[i][1] *= dist;
    	}
    	
    	double[] tmp_rows = new double[3];
    	for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
    		idx2 = i-1; idx3 = i+1;
    		tmp_rows[0] = this.spawnRates[idx2][0] + this.spawnRates[i][0] + this.spawnRates[idx3][0];
    		tmp_rows[1] = this.spawnRates[idx2][1] + this.spawnRates[i][1] + this.spawnRates[idx3][1];
    		tmp_rows[2] = this.spawnRates[idx2][2] + this.spawnRates[i][2] + this.spawnRates[idx3][2];

    		dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - 1) * (this.myHQLoc.y - 1),
    								(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - 1) * (this.enemyHQLoc.y - 1));
    		result[i][1] = (tmp_rows[0] + tmp_rows[1] + tmp_rows[2]) * dist;
    		
    		for (int j=2; j<this.myRC.getMapHeight()-1; ++j){
    			idx = (j-2) % 3;
    			idx4 = j-1;
    			if (dist == 0){
    				result[i][j] = tmp_rows[0] + tmp_rows[1] + tmp_rows[2] - tmp_rows[idx];
    			}
    			else {
    				result[i][j] = result[i][idx4] / dist - tmp_rows[idx];	
    			}
        		tmp_rows[idx] = this.spawnRates[idx2][idx4] + this.spawnRates[i][idx4] + this.spawnRates[idx3][idx4];
        		dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - j) * (this.myHQLoc.y - j),
        								(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - j) * (this.enemyHQLoc.y - j) );
        		result[i][j] += tmp_rows[idx];
        		result[i][j] *= dist;
    		}
    	}

    	return result;
    }
    
    
    /*
     * Should be called after locScores is saved with computeLocScoresDist return value
     */
    protected MapLocation findFirstPastureLoc(){
    	double bestSoFar = 0;
    	int besti = 0, bestj = 0;
    	for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
    		for (int j=1; j<this.myRC.getMapHeight()-1; ++j){
    			if (this.locScores[i][j] > bestSoFar){
    				bestSoFar = this.locScores[i][j];
    				besti = i; bestj = j;
    			}
    		}
    	}
    	return new MapLocation(besti, bestj);
    }
    
    /*
     * Should only be called after locScores is initialized and created.
     * Assumes that loc is not a void spot.
     */
    protected MapLocation findPastureLoc(MapLocation loc) {
    	
    	double score;
    	int counter = 0;
    	int centeri = loc.x, centerj = loc.y;
    	
    	
    	while (true) {
    		++counter;
    		if (counter > 15){ return null;};

        	System.out.println("CENTER " + centeri + "," + centerj);
    		score = this.locScores[centeri][centerj];
    		
    		double[] scores = {
    			this.locScores[centeri-1][centerj-1],
    			this.locScores[centeri][centerj-1],
    			this.locScores[centeri+1][centerj-1],
    			this.locScores[centeri+1][centerj],
    			this.locScores[centeri+1][centerj+1],
    			this.locScores[centeri][centerj+1],
    			this.locScores[centeri-1][centerj+1],
    			this.locScores[centeri-1][centerj],
    		};
    		
    		while (true) {
    			int bestIdx=0; double bestScore = scores[0];
    			for (int i=0; i<scores.length; ++i){
    				System.out.print(scores[i] + " ");
    				if (scores[i] > bestScore) {
    					bestIdx = i;
    					bestScore = scores[i];
    				}
    			}
    			System.out.println();
    			System.out.println(bestIdx);
    			if (score >= bestScore){
    				return new MapLocation(centeri, centerj);
    			}
    			int x = centeri;
    			int y = centerj;
    			switch (bestIdx) {
					case 0: x = centeri-1; y = centerj-1; break;
					case 1: x = centeri; y = centerj-1; break;
					case 2: x = centeri+1; y = centerj-1; break;
					case 3: x = centeri+1; y = centerj; break;
					case 4: x = centeri+1; y = centerj+1; break;
					case 5: x = centeri; y = centerj+1; break;
					case 6: x = centeri-1; y = centerj+1; break;
					case 7: x = centeri-1; y = centerj-1; break;
    			}
    			if (this.myRC.senseTerrainTile(new MapLocation(x, y)) == TerrainTile.VOID){
    				scores[bestIdx] = 0;
    			}
    			else {
    				centeri = x; centerj = y;
    				break;
    			}
    		}
    	}
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
        float random_float =  (float) (this.myRC.getRobot().getID() * Math.random());
        return random_float - (int) random_float;
    }
}
