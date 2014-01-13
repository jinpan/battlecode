package attack;

import static org.junit.Assert.*;

import org.junit.Test;
import battlecode.common.MapLocation;

public class EnemyProfileMessageTest {

	@Test
	public void encodeDecodeTest() {
		
		int[] ids = {0, 1, 5, 16, 4095};
		int[] times = {0, 128, 1999};
		int[] healths = {0, 5, 80, 100, 184, 200};
		MapLocation[] locs = {
			new MapLocation(0, 0),
			new MapLocation(10, 0),
			new MapLocation(0, 10),
			new MapLocation(3, 255),
			new MapLocation(255, 3)
		};
		
		for (int id: ids){
			for (int health: healths){
				for (MapLocation loc: locs){
					for (int time: times){
						encodeDecode(id, health, loc, time);
					}
				}
			}
		}
	}
	
	private void encodeDecode(int id, int health, MapLocation loc, int time) {
		EnemyProfileMessage em = new EnemyProfileMessage(id, health, loc, time);
		long message = em.encode();
		EnemyProfileMessage em2 = EnemyProfileMessage.decode(message);
		
		assertEquals(id, em2.id);
		assertEquals(health, em2.health);
		assertEquals(loc, em2.lastSeenLoc);
		assertEquals(time, em2.lastSeenTime);
	}

}
