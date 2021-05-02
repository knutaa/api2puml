package no.paneon.swagger2puml;

public class Property {

	String name;
	String type;
	String cardinality;
	boolean required = false;
	
	public Property(String name, String type, String cardinality, boolean required) {
		this.name = name;
		this.type = type;
		this.cardinality = cardinality;
		this.required = required;
	}

	String getName() { 
		return name;
	}
	
	String getType() {
		return type;
	}
	
	String getCardinality() { 
		return cardinality;
	}

	boolean isRequired() {
		return required;
	}
	
}
