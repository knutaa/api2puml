package no.paneon.swagger2puml;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class EnumNode {
	
    private static Logger LOG = Logger.getLogger(EnumNode.class);

	String node;
	public String type;
	
	String placedByNode;
	Place placedInDirection;
	
	EnumNode(String type) {
		this.type=type;
		this.values = new LinkedList<>();

		LOG.debug("EnumNode(): type=" + this.type + " values=" + this.values);

	}
	
	void setPlacement(String node, Place direction) {
		this.placedByNode = node;
		this.placedInDirection = direction;
		LOG.debug("setPlacement: enum=" + this.type + " placedByNode=" + placedByNode + " placedInDirection=" + placedInDirection);
	}
	
	public List<String> values;
		
	public void addValue(String value) {
		LOG.debug("addValue: type=" + this.type + " v=" + value);
		this.values.add(value);
	}

	public void addValues(List<String> enumValues) {
		enumValues.forEach(this::addValue);
	}

	public void setFloatingPlacement() {
		// TODO Auto-generated method stub
		
	}
	
}
