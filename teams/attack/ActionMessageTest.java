package attack;

import battlecode.common.MapLocation;
import attack.BaseRobot.State;

public class ActionMessageTest {

	@Test
	public void encodeDecodeTest() {
		
		int[] ids = {0, 1, 5, 16, 4095};
		MapLocation[] locs = {
			new MapLocation(0, 0),
			new MapLocation(10, 0),
			new MapLocation(0, 10),
			new MapLocation(3, 255),
			new MapLocation(255, 3)
		};
		
		for (State state: State.values()){
			for (int id: ids){
				for (MapLocation loc: locs){
					encodeDecode(state, id, loc);
				}
			}
		}
	}
	
	private void encodeDecode(State state, int id, MapLocation loc) {
		ActionMessage am = new ActionMessage(state, id, loc);
		int message = (int) am.encode();
		ActionMessage am2 = ActionMessage.decode(message);
		
		//assertEquals(state, am2.state);
		//assertEquals(id, am2.targetID);
		//assertEquals(loc, am2.targetLocation);
	}

}
