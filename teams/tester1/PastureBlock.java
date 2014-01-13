package tester1;

import battlecode.common.MapLocation;
import java.util.ArrayList;

public class PastureBlock {
	
	MapLocation vertex;
	int width; int height;
	
	public PastureBlock(MapLocation vert, int x, int y){
		this.vertex= vert; 
		this.width= x; 
		this.height=y;
	}
	
	public ArrayList<MapLocation> pastrLocs(ArrayList<MapLocation> pastrs){
		int num= this.width/10 * this.height/10 + 1;
		if (num==1){
			MapLocation loc = new MapLocation(vertex.x+width/2, vertex.y+height/2);
			pastrs.add(loc);
		} else {
			for (int i=0; i<this.height/10; i++){
				for (int j=0; j<this.width/10; j++){
					pastrs.add(new MapLocation(vertex.x+j*this.width/10, vertex.y+i*this.height/10));
				}
			}
		} return pastrs;
	}

}
