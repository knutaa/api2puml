package no.paneon.swagger2puml;

import java.util.List;

import org.apache.log4j.Logger;

public class Edge {
	
    private static Logger LOG = Logger.getLogger(Edge.class);

	public String relation;
	public String related;
	public String cardinality;
	public String node;
	public boolean required=false;
	
	Edge(String node, String relation, String related, String cardinality, boolean required) {
		this.node=node;
		this.relation=relation;
		this.related=related;
		this.cardinality=cardinality;
		this.required=required;
		 
		String t = node + "-" + related + "-" + relation + "-" + cardinality;
		System.out.println("Edge:: " + t);
		
	}
	
	String getID() {
		return node + "-" + related + "-" + relation + "-" + this.getClass();
	}
	
	boolean isPlaced(List<String> processed) {
		String id = getID();
		boolean res = processed.contains(id);
		LOG.debug("isPlaced: edge=" + this + " res=" + res);
		return res;
	}
	
	void placed(List<String> processed) {
		String id = getID();
		processed.add(id);
	}
	
	public String toString() {
		return "edge: " + node + " --> " + cardinality + " " + related;
	}
	
	public boolean isRequired() {
		return required;
	}
	
}
