package no.paneon.swagger2puml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Place {
	LEFT("placeLeft"),
	RIGHT("placeRight"),
	ABOVE("placeAbove"),
	BELOW("placeBelow"),
	
	FORCELEFT("forceLeft"),
	FORCERIGHT("forceRight"),
	FORCEABOVE("forceAbove"),
	FORCEBELOW("forceBelow"),
	FORCEFLOAT("forceFloat");

    public final String label;
    
    private Place(String label) {
        this.label = label;
    }
    
    public boolean isForced() {
    	List<Place> forced = Arrays.asList(Place.FORCELEFT, Place.FORCERIGHT, Place.FORCEABOVE, Place.FORCEBELOW, Place.FORCEFLOAT);
    	return forced.stream().filter(p -> p.toString().equals(this.toString())).findAny().isPresent();
    }
    
    static public List<Place> coreValues() {
    	return Arrays.asList(Place.LEFT, Place.RIGHT, Place.ABOVE, Place.BELOW);
    }
    
	@SuppressWarnings("serial")
	static Map<Place,Place> mapping = new HashMap<Place,Place>() {{
		put(Place.LEFT, Place.RIGHT);
		put(Place.RIGHT, Place.LEFT);
		put(Place.ABOVE, Place.BELOW);
		put(Place.BELOW, Place.ABOVE);
	}};
	
	@SuppressWarnings("serial")
	static Map<Place,Place> coreDirection = new HashMap<Place,Place>() {{
		put(Place.FORCELEFT, Place.LEFT);
		put(Place.FORCERIGHT, Place.RIGHT);
		put(Place.FORCEABOVE, Place.ABOVE);
		put(Place.FORCEBELOW, Place.BELOW);
	}};
	
	static public Place reverse(Place direction) {
		return mapping.get(direction);
	}
	
}



