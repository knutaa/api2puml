package no.paneon.puml;

import java.util.LinkedList;
import java.util.List;

import no.paneon.swagger2puml.EnumNode;

public class EnumEntity extends Entity {

	String type;
	List<String> values;
	
	public EnumEntity(String type) {
		super();
		this.type = type;
		this.values = new LinkedList<>();
	}
	
	public EnumEntity(EnumNode enode) {
		super();
		this.type = enode.type;
		this.values = new LinkedList<>();
		enode.values.forEach(v -> addValue(v));
	}

	public void addValue(String value) {
		this.values.add(value);
	}
	
	public String toString() {
		String res="";
		res = res + "class " + this.type + " <<Enumeration>> {" + "\n";
	    
	    for(String v : values) {	
	    	res = res + INDENT + v + "\n";
	    }
	    res = res + "}" + "\n";
	    
	    return res;	
	}
	
}
	
