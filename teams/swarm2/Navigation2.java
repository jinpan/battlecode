package swarm2;

import java.util.LinkedList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class Navigation2 {
	static boolean debug;
	
	int[][] map;
	static BaseRobot robot;
	
	
	public Navigation2(BaseRobot robot){
		this.robot = robot;
		
		int width = robot.myRC.getMapWidth(), height = robot.myRC.getMapHeight();
		map = new int[width][height];
		
		for (int i=0; i<width; ++i){
			for (int j=0; j<=height/2; ++j){
				if (robot.myRC.senseTerrainTile(new MapLocation(i, j)) == TerrainTile.VOID){
					map[i][j] = -1;
					map[width - i][height - j] = -1;
				}
			}
		}
	}
	
	public SearchNode search(MapLocation source, MapLocation target){
		MapLocation s = source, t = target;
		LinkedList<MapLocation> result_list = new LinkedList<MapLocation>();
		result_list.addLast(s);
		
		Direction dir, facing_a, facing_b;
		MapLocation s_a, s_b;
		
		while (! s.equals(t)){
			// move s towards t
			dir = s.directionTo(t);
			if (map[s.x + dir.dx][s.y + dir.dy] >= 0){
				s = new MapLocation(s.x + dir.dx, s.y + dir.dy);
				result_list.addLast(s);
				continue;
			}
			
			// something is in my way
			s_a = s; s_b = s;
			facing_a = dir.rotateLeft();
			facing_b = dir.rotateRight();
			while (true){
				facing_a = facing_a.rotateRight();
				while (map[s_a.x + facing_a.dx][s_a.y + facing_a.dy] < 0){
					facing_a = facing_a.rotateLeft();
				}
				s_a = s_a.add(facing_a);
				
				facing_b = facing_b.rotateLeft();
				while (map[s_b.x + facing_b.dx][s_b.y + facing_b.dy] < 0){
					facing_b = facing_b.rotateRight();
				}
				s_b = s_b.add(facing_b);
				
				
				
			}
		}

		SearchNode result = new SearchNode(source, 1, null);
		return result;
	}


}
