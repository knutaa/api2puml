package no.paneon.puml;

import java.util.LinkedList;
import java.util.List;

public class Comment extends Core {
	
	String comment;
	
	public Comment(String comment) {
		super();
		this.comment = comment;
	}
	
	public String toString() {
		return comment + " (seq=" + seq + ")";
	}
	
}
	
