package team007;

import battlecode.common.MapLocation;

/*
 * Class for encoding action information inside messages.
 * We return longs to make sure things play nicely with the interface, but every value can be correctly interpreted
 * as an integer.
 * We use -1 as a poison pill value so any valid message encoding should not be 0.  This is ensured,
 * as the map locations should not be hitting their upper limit (255; 0xFF).
 *   First byte is used for the state
 *       0x0: DEFAULT
 *       0x1: ATTACK  ** should only be for attacking stationary objects like pastrs or noise towers; create a new state for attacking dynamic?
 *       0x2: DEFEND
 *       0x3: HERD
 *       0x4: PASTURIZE
 *   
 *   Second through fourth bytes are used for the target ID. Could be the ID of a robot, or some other enumeration (0 - 4095)
 *   Fifth through Sixth bytes are used for the horizontal map position (0 - 255)
 *   Seventh through Eighth bytes are used for the vertical map position (0 - 255)
 */

public class ActionMessage implements Message {
    
    protected BaseRobot.State state;
    protected int targetID;
    protected MapLocation targetLocation;
    
    public ActionMessage(BaseRobot.State state, int targetID, MapLocation targetLocation){
        this.state = state;
        this.targetID = targetID;
        this.targetLocation = targetLocation;
    }
    
    public long encode(){
    	int result = 0;
    	int state;
        switch (this.state) {
        	case DEFAULT: state = 0x0; break;
        	case ATTACK: state = 0x1; break;
        	case DEFEND: state = 0x2; break;
        	case HERD: state = 0x3; break;
        	case PASTURIZE: state = 0x4; break;
        	default: state = 0x0;
        }
        result += state; result <<= 12;
        result += this.targetID; result <<= 8;
        result += this.targetLocation.x; result <<= 8;
        result += this.targetLocation.y;
        
        return result;
    }
    
    public Action toAction(){
        return new Action(this.state, this.targetLocation, this.targetID);
    }
    
    public static ActionMessage decode(long message){
        int y_pos = (int) (message % 0x100); message >>>= 8;
        int x_pos = (int) (message % 0x100); message >>>= 8;
        int target_id = (int) (message % 0x1000); message >>>= 12;
        int state = (int) (message % 0x10); assert(state == message);
        
        MapLocation loc = new MapLocation(x_pos, y_pos);

        BaseRobot.State myState;
        switch (state) {
        	case 0x0: myState = BaseRobot.State.DEFAULT; break;
        	case 0x1: myState = BaseRobot.State.ATTACK; break;
        	case 0x2: myState = BaseRobot.State.DEFEND; break;
        	case 0x3: myState = BaseRobot.State.HERD; break;
        	case 0x4: myState = BaseRobot.State.PASTURIZE; break;
        	default: myState = BaseRobot.State.DEFAULT; break;
        }
        
        return new ActionMessage(myState, target_id, loc);
    }
}
