package no.paneon.swagger2puml;

import org.apache.log4j.Logger;

public class Position {
	
    private static Logger LOG = Logger.getLogger(App.class);

	int x=-100;
	int y=-100;
	boolean positioned=false;
	
	Position() {
	}
	
	Position(boolean center) {
		this.x = 10;
		this.y = 10;
		positioned=true;
	}
	
	Position(Position pos) {
		this.x = pos.x;
		this.y = pos.y;
		positioned=true;
	}
	
	public String toString() {
		return "[x=" + x + ", y=" + y + "]";
	}
	
	public boolean isPositioned() {
		return positioned;
	}
	
}
