package no.paneon.swagger2puml;

public class OtherProperty {

	String name;
	String value;
	boolean required;
	
	public OtherProperty(String name, String value, boolean required) {
		this.name = name;
		this.value = value;
		this.required = required;
	}

	String getName() { 
		return name;
	}
	
	String getValue() {
		return value;
	}
	
	boolean isRequired() {
		return required;
	}
	
}