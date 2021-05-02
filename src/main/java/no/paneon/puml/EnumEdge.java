package no.paneon.puml;

import no.paneon.swagger2puml.Edge;
import no.paneon.swagger2puml.Place;

public class EnumEdge extends HiddenEdge {

	public EnumEdge(String from, Place place, String to) {
		super(from, place, to);
	}

	public EnumEdge(Place place, Edge edge) {
		super(place, edge);
	}

}
