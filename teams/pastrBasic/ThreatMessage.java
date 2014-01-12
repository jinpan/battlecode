package pastrBasic;

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
 *       0x4: RALLY
 *       
 *       0x5: ATTACKHIGH
 *       0x6: DEFENSEHIGH
 *       0x7: GATHEROUT
 *       0x8: SCOUTHIGH
 *   Second through fourth bytes are used for the target ID. Could be the ID of a robot, or some other enumeration (0 - 4095)
 *   Fifth through Sixth bytes are used for the horizontal map position (0 - 255)
 *   Seventh through Eighth bytes are used for the vertical map position (0 - 255)
 */

public class ThreatMessage implements Message {
    
    public int targetID;
    public int roundNum; //round number mod 100
    public MapLocation targetLocation;
    
    public ThreatMessage(int roundNum, int targetID, MapLocation targetLocation){
        this.roundNum = roundNum%100;
        this.targetID = targetID;
        this.targetLocation = targetLocation;
    }
    
    public int encode(){
    	int result = 0;
        result += roundNum; result <<= 10;
        result += this.targetID; result <<= 7;
        result += this.targetLocation.x; result <<= 7;
        result += this.targetLocation.y;
        
        return result;
    }
    
    public Action toAction(){
        return new Action(BaseRobot.State.ATTACK, this.targetLocation, this.targetID);
    }
    
    public static ThreatMessage decode(int message){
        int y_pos = message % 128; message >>= 7;
        int x_pos = message % 128; message >>= 7;
        int target_id = message % 1024; message >>= 10;
        int round = message; assert(round < 100);
        
        MapLocation loc = new MapLocation(x_pos, y_pos);
        
        return new ThreatMessage(round, target_id, loc);
    }

}
