package no.paneon.puml;

import no.paneon.common.Config;
import no.paneon.swagger2puml.Edge;
import no.paneon.swagger2puml.Place;

public class EdgeEntity extends Entity {
			
    private static Config CONFIG = Config.getConfig();

	String from = "";
	String to = "";
	Edge edge = null;
	Place place = null;
	boolean required = false;

	public EdgeEntity(String from, Place place, String to, boolean required) {
		super();
		this.from=from;
		this.to=to;
		this.place=place;
		this.required=required;
	}

	public EdgeEntity(Place place, Edge edge) {
		super();
		this.place=place;
		this.edge = edge;
		this.required=edge.isRequired();
	}
	
	public String toString() {
		String res="";
		
		String to = edge!=null ? edge.related : this.to;
		String from = edge!=null ? edge.node : this.from;

		String cardinality = edge!=null ? edge.cardinality : "";
		
		if(cardinality.length()>0) {
			cardinality = "\"" + cardinality + "\"";
		}
		String label = edge!=null ? edge.relation : "";
//				
//		switch(place) {
//		case LEFT: 
//		case FORCELEFT:
//		    res = to + " " + cardinality + " <-left-* " + from + " : " + label + '\n';
//			break;
//			
//		case RIGHT:
//		case FORCERIGHT:
//		    res = from + " *-right-> " + cardinality + " " + to + " : " + label + '\n';
//			break;
//			
//		case ABOVE:
//		case FORCEABOVE:
//		    res = to+ " " + cardinality + " <--* " + from + " : " + label + '\n';
//			break;
//			
//		case BELOW:
//		case FORCEBELOW:
//		    res = from+ " *--> " + " " + cardinality + " " + to + " : " + label + '\n';
//			break;
//			
//		}
		
		String strLabel = label;
		if(required) {
			String format = CONFIG.getRequiredFormatting();
			strLabel = String.format(format,strLabel);
		} 
		
		switch(place) {
		case LEFT: 
		case FORCELEFT:
		    res = to + " " + cardinality + " <-left-* " + from + " : " + strLabel + '\n';
			break;
			
		case RIGHT:
		case FORCERIGHT:
		    res = from + " *-right-> " + cardinality + " " + to + " : " + strLabel + '\n';
			break;
			
		case ABOVE:
		case FORCEABOVE:
		    res = to+ " " + cardinality + " <--* " + from + " : " + strLabel + '\n';
			break;
			
		case BELOW:
		case FORCEBELOW:
		    res = from+ " *--> " + " " + cardinality + " " + to + " : " + strLabel + '\n';
			break;
		}
	    
	    return res;	
		
	}
	
}
	