package hqop;

import battlecode.common.MapLocation;
import java.util.ArrayList;

public class PastureBlock {
	
	MapLocation vertex;
	int width; int height;
	int buffer;
	
	public PastureBlock(MapLocation vert, int x, int y, int buff){
		this.vertex= vert; 
		this.width= x; 
		this.height=y;
		this.buffer= buff;
	}
	
	public ArrayList<MapLocation> pastrLocs(ArrayList<MapLocation> pastrs){
		int num= (this.width/this.buffer) * (this.height/this.buffer);
		if (num==0){
			MapLocation loc = new MapLocation(vertex.x+width/2, vertex.y+height/2);
			pastrs.add(loc);
		} else {
			for (int i=0; i<=this.width/this.buffer; i++){
				for (int j=0; j<=this.height/this.buffer; j++){
					pastrs.add(new MapLocation(buffer/2+ vertex.x+i*buffer, buffer/2+ vertex.y+j*buffer));
				}
			}
		} return pastrs;
	}
	
	public boolean contains(MapLocation loc){
		boolean xin= loc.x>=vertex.x && loc.x<=vertex.x+width;
		boolean yin= loc.y>=vertex.y && loc.y<=vertex.y+height;
		return xin && yin;
	}

}
