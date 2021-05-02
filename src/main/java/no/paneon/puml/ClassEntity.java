package no.paneon.puml;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Category;

import no.paneon.common.Config;

public class ClassEntity extends Entity {
	List<ClassProperty> classProperties;
	List<EnumEntity> enumEntities;
	List<EdgeEntity> edges;

	static List<String> processedEnums = new LinkedList<>();
	
	String name;
	String stereotype;
		
	Config CONFIG = Config.getConfig();

	String description = "";
	
	public ClassEntity(String name, String stereotype) {
		super();
		this.name = name;
		this.stereotype = stereotype;
		this.classProperties = new LinkedList<>();
		this.enumEntities = new LinkedList<>();
		this.edges = new LinkedList<>();
		
        LOG.debug("ClassEntity: name: " + name + " seq:" + seq);

	}
	
	public void addProperty(ClassProperty c) {
		if(c!=null) classProperties.add(c);
	}
	
	public boolean addEnum(EnumEntity c) {
        LOG.debug("ClassEntity: addEnum: " + c);

		if(c!=null) {
			enumEntities.add(c);
			processedEnums.add(c.type);
		}
		return c!=null;
	}

	public List<EnumEntity> getEnums() {
		return enumEntities;
	}
	
	public boolean addEdge(EdgeEntity c) {
        LOG.debug("ClassEntity: addEdge: " + c);

		if(c!=null) {
			edges.add(c);
		}
		return c!=null;
	}
	
	public String toString() {
		String res="";
	    res = res + "class " + this.name + " " + this.stereotype + " {" + "\n";
	    
	    String desc = description;
	    if(CONFIG.includeDescription()) {
		    if(desc.isEmpty()) {
		    	res = res + INDENT + "{field}//" + BLANK + "//\n";
		    } else {
		    	res = res + Utils.formatDescription(description, INDENT);
		    	res = res + INDENT + "{field}\n";
		    }
	    }
	    
	    res = res + classProperties.stream()
	    			.sorted(Comparator.comparing(p -> p.name))
	    			.collect(Collectors.partitioningBy(p -> p.name.startsWith("@")))
	    			.entrySet().stream()
	    			.map(n -> n.getValue())
	    			.flatMap(List::stream)
			    	.map(p -> {
			    		return INDENT + p.toString();
			    	})
			    	.collect(Collectors.joining("\n")) + "\n";
	    
	    res = res + "}" + "\n";
	    
	    return res;
	    
	}

	public int getEdgeCount() {
        LOG.debug("ClassEntity: getEdgeCount: " + edges);
        LOG.debug("ClassEntity: getEdgeCount: " + edges.size());
		return edges.size();
	}

	public boolean isEnumProcessed(String type) {
		return processedEnums.contains(type);
	}
	
	public void addDescription(String description) {
		this.description = description;
	}
	
	public static void clear() {
		processedEnums.clear();
	}
}
	
