package jinpan;

import battlecode.common.MapLocation;

/*
 * Class for encoding information inside messages.
 * We have 8 bytes in an integer; Each byte can contain 16 mappings.
 * We use -1 as a poison pill value so any valid message encoding should not be 0.  This is ensured,
 * as the map locations should not be hitting their upper limit (255; 0xFF).
 *   First byte is used for the state
 *       0x0: DEFAULT
 *       0x1: ATTACK
 *       0x2: DEFENSE
 *       0x3: PASTURE
 *       0x4: SCOUT
 *       
 *       0x5: ATTACKHIGH
 *       0x6: DEFENSEHIGH
 *       0x7: PASTUREHIGH
 *       0x8: SCOUTHIGH
 *   Second through fourth bytes are used for the target ID. Could be the ID of a robot, or some other enumeration (0 - 4095)
 *   Fifth through Sixth bytes are used for the horizontal map position (0 - 255)
 *   Seventh through Eighth bytes are used for the vertical map position (0 - 255)
 */

public class ActionMessage implements Message {
    
    public BaseRobot.State myState;
    public int targetID;
    public MapLocation targetLocation;
    
    public ActionMessage(BaseRobot.State state, int targetID, MapLocation targetLocation){
        this.myState = state;
        this.targetID = targetID;
        this.targetLocation = targetLocation;
    }
    
    public int encode(){
    	int result = 0;
    	int state;
        switch (this.myState) {
        	case DEFAULT: state = 0x0; break;
        	case ATTACK: state = 0x1; break;
        	case DEFENSE: state = 0x2; break;
        	case PASTURE: state = 0x3; break;
        	case SCOUT: state = 0x4; break;
        	case ATTACKHIGH: state = 0x5; break;
        	case DEFENSEHIGH: state = 0x6; break;
        	case PASTUREHIGH: state = 0x7; break;
        	case SCOUTHIGH: state = 0x8; break;
        	default: state = 0x0;
        }
        result += state; result <<= 12;
        result += this.targetID; result <<= 8;
        result += this.targetLocation.x; result <<= 8;
        result += this.targetLocation.y;
        
        return result;
    }
    
    public Action toAction(){
        return new Action(this.myState, this.targetLocation, this.targetID);
    }
    
    public static ActionMessage decode(int message){
        int y_pos = message % 0x100; message >>= 8;
        int x_pos = message % 0x100; message >>= 8;
        int target_id = message % 0x1000; message >>= 12;
        int state = message % 0x10; assert(state == message);
        
        MapLocation loc = new MapLocation(x_pos, y_pos);

        BaseRobot.State myState;
        switch (state) {
        	case 0x0: myState = BaseRobot.State.DEFAULT; break;
        	case 0x1: myState = BaseRobot.State.ATTACK; break;
        	case 0x2: myState = BaseRobot.State.DEFENSE; break;
        	case 0x3: myState = BaseRobot.State.PASTURE; break;
        	case 0x4: myState = BaseRobot.State.SCOUT; break;
        	case 0x5: myState = BaseRobot.State.ATTACKHIGH; break;
        	case 0x6: myState = BaseRobot.State.DEFENSEHIGH; break;
        	case 0x7: myState = BaseRobot.State.PASTUREHIGH; break;
        	case 0x8: myState = BaseRobot.State.SCOUTHIGH; break;
        	default: myState = BaseRobot.State.DEFAULT; break;
        }
        
        return new ActionMessage(myState, target_id, loc);
    }

}
