package no.paneon.puml;

import no.paneon.swagger2puml.Edge;
import no.paneon.swagger2puml.Place;

public class ForcedHiddenEdge extends EnumEdge {

	public ForcedHiddenEdge(String from, Place place, String to) {
		super(from, place, to);
        LOG.debug("ForcedHiddenEdge: from=" + from + " place=" + place + " to=" + to);

	}

	public ForcedHiddenEdge(Place place, Edge edge) {
		super(place, edge);
        LOG.debug("ForcedHiddenEdge: place=" + place + " edge=" + edge);
	}

}
