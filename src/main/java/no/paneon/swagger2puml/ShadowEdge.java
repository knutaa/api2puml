package no.paneon.swagger2puml;

public class ShadowEdge extends Edge {

	public ShadowEdge(String node, String relation, String related, String cardinality) {
		super(node, relation, related, cardinality, false);
	}

	public ShadowEdge(String from, String to) {
		super(from, "", to, "", false);
	}
	
}
