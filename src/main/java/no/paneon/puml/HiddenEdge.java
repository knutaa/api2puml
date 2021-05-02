package no.paneon.puml;

import no.paneon.swagger2puml.Edge;
import no.paneon.swagger2puml.Place;

public class HiddenEdge extends EdgeEntity {

	public HiddenEdge(String from, Place place, String to) {
		super(from, place, to, false);
	}

	public HiddenEdge(Place place, Edge edge) {
		super(place, edge);
	}

	public String toString() {
		String res="";
		
		String to = edge!=null ? edge.related : this.to;
		String from = edge!=null ? edge.node : this.from;
		
		switch(place) {
		case LEFT: 
		    res = to + " <-left[hidden]- " + from + '\n';
			break;
			
		case RIGHT:
		    res = from + " -right[hidden]-> " + to + '\n';
			break;
			
		case ABOVE:
		    res = to + " <-[hidden]- " + from  + '\n';
			break;
			
		case BELOW:
		    res = from + " -[hidden]-> " + to + '\n';
			break;
		
		default:
			LOG.error("HiddenEdge with unexpected place: " + place);
		}
	    
	    return res;	
	}
	
}
