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
		int buffer= 10;
		int num= (this.width/buffer) * (this.height/buffer);
		//System.out.println(num);
		if (num==0){
			MapLocation loc = new MapLocation(vertex.x+width/2, vertex.y+height/2);
			pastrs.add(loc);
		} else {
			for (int i=0; i<=this.width/buffer; i++){
				for (int j=0; j<=this.height/buffer; j++){
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
