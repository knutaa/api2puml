package no.paneon.swagger2puml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import no.paneon.common.Config;
import no.paneon.puml.ClassEntity;
import no.paneon.puml.Comment;
import no.paneon.puml.Diagram;
import no.paneon.puml.EdgeEntity;
import no.paneon.puml.EnumEdge;
import no.paneon.puml.EnumEntity;
import no.paneon.puml.ForcedHiddenEdge;
import no.paneon.puml.HiddenEdge;

import org.apache.log4j.Logger;

public class Graph {

    private static Logger LOG = Logger.getLogger(Graph.class);

	private static List<Place> funcAboveAbove = Arrays.asList(Place.ABOVE, Place.ABOVE);
	private static List<Place> funcAboveBelow = Arrays.asList(Place.ABOVE, Place.BELOW);
	private static List<Place> funcBelowAbove = Arrays.asList(Place.BELOW, Place.ABOVE);
	private static List<Place> funcBelowBelow = Arrays.asList(Place.BELOW, Place.BELOW);
	@SuppressWarnings("unused")
	private static List<Place> funcLeftRight  = Arrays.asList(Place.LEFT, Place.RIGHT);
	private static List<Place> funcRightLeft  = Arrays.asList(Place.RIGHT, Place.LEFT);
    
    public class GraphNode {
    	// Map<String,String> properties;
    	List<Property> properties;
    	Set<String> outboundNeighbours;
    	Set<String> inboundNeighbours;
    	
    	Set<Edge> outEdges;
    	
    	Map<String,Neighbour> neighbours;
    	
    	Map<Place,List<String>> placements;
    	
    	String description = "";

    	List<OtherProperty> otherProperties;

    	GraphNode() {
    		// properties = new HashMap<>();
    		properties = new LinkedList<>();
    		outboundNeighbours = new HashSet<>();
    		inboundNeighbours = new HashSet<>();
    		outEdges = new HashSet<>();
    		neighbours = new HashMap<>();
    		placements = new HashMap<>();
    		
    		otherProperties = new LinkedList<>();
    		    		
    	}
    	
    }
    
    public class Neighbour {
    	
    	String to;
    	Set<Edge> edges;
    	
    	Neighbour(String to) {
    		this.to=to;
    		this.edges = new HashSet<>();
    	}
    	
    }
    
	String resource;
    List<String> nodes;
    Positions positions;
    Map<String, GraphNode> graphNodes;
    Map<String, EnumNode> enumNodes;
    Map<String, List<String>> enumMapping;

    List<String> processedSwagger;

	Config CONFIG = Config.getConfig();

	Map<String, JSONArray> required;
	
	public Graph(String resource) {
		this.resource = resource;
		this.nodes = new ArrayList<>();
		this.positions = new Positions();
		this.graphNodes = new HashMap<>();
		this.enumNodes = new HashMap<>();
		this.enumMapping = new HashMap<>();
		this.required = new HashMap<>();

	    positions.setPosition(resource);

		this.processedSwagger = new ArrayList<>();

	}
	
	
	public void addNode(String node) {
		if(!graphNodes.containsKey(node)) {
			graphNodes.put(node, new GraphNode());
			if(!nodes.contains(node)) nodes.add(node);
		}
	}
	
	public List<String> getNodes() {
		return nodes;
	}
	
	public void processResource(String resource, JSONObject swagger) {
		
	    if(processedSwagger.contains(resource)) return;
	    
	    processedSwagger.add(resource);
	    
	    addNode(resource);
	    
	    if(!Utils.getDefinitions(swagger).has(resource)) {
	        LOG.error("ISSUE: Resource " + resource + " not found in specification");
	        if(Utils.getDefinition(swagger,resource)==null) {
	            // process.exit(-1);
	            return;
	        }
	    }
	   
	    JSONObject definition = Utils.getDefinition(swagger,resource);
	    if(!definition.has("properties")) {
	    	LOG.error("ISSUE: Unable to locate properties for resource: " + resource);
	        return;
	    }
	    
	    addRequired(resource,definition.optJSONArray("required"));
	    
	    // process required from _Create variation - needed later for isRequired
	    JSONObject postDefinition = Utils.getDefinition(swagger,resource + "_Create");
	    if(postDefinition!=null) {
		    addRequired(resource + "_Create",postDefinition.optJSONArray("required"));
	    }

	    JSONObject properties = definition.getJSONObject("properties");
	    
	    if(definition.has("description")) {
	    	addDescription(resource, definition.getString("description"));
	    }
	    
	    properties.keySet().forEach(p -> {
	    	
	        JSONObject property = properties.getJSONObject(p);
	        
		    LOG.debug("processResource: resource=" + resource + " property="+p);
		    LOG.debug("processResource: resource=" + resource + " properties=" + property.toString(2));

		    boolean isRequired = this.isRequired(resource,p);
		    
		    boolean postIsRequired = this.isRequired(resource + "_Create", p);
		    
		    // System.out.println("processResource: resource=" + resource + " property=" + p + " isRequired="+isRequired + " postIsRequired="+postIsRequired );
		    isRequired = isRequired || postIsRequired;
		    
		    String cardinality = isRequired ? Config.CARDINALITY_MANDATORY : Config.CARDINALITY_OPTIONAL;
		    
	        if(property.has("$ref")) {
	            String part = getReference(property, "$ref"); 

			    LOG.debug("processResource: resource=" + resource + " property="+p + " part=" + part + " cardinality=" + cardinality);

			    if(showRelationship(swagger,resource,part)) {
				    processResource(part,swagger);
	                addEdge(resource, part, new Edge(resource, p, part, cardinality, isRequired )); 
	            } else {
	                if(Utils.isEnumType(swagger, part) && presentEnumForResource(resource)) {
			        	processEnum(resource, part, swagger);
			        } 
	                addProperty(resource, p, Utils.type(property,part), cardinality, isRequired);
	            }
	        } else if(property.has("items") && property.getJSONObject("items").has("$ref")) {
	            String part = getReference(property, "items", "$ref"); 

	            int min=0;
	            String max="*";
	            if(property.has("minItems")) {
	                min=property.optInt("minItems");
	            }
	            if(property.has("maxItems")) {
	                int optMax=property.optInt("maxItems");
	                max = Integer.toString(optMax);
	            }
	            cardinality=Integer.toString(min) +  ".." + max;
	            
			    if(showRelationship(swagger,resource,part)) {
	            	processResource(part,swagger);
	                addEdge(resource, part, new Edge(resource, p, part, cardinality, isRequired)); 
	            } else {
	                if(Utils.isEnumType(swagger, part) && presentEnumForResource(resource)) {
			        	processEnum(resource, part, swagger);
			        }
	                addProperty(resource, p, Utils.type(property,part), cardinality, isRequired);
	            }
	        } else if(property.has("items") && property.getJSONObject("items").has("type")) {
			    LOG.debug("processResource: array of simple type :: resource=" + resource + " property=" + property);
	            JSONObject item = property.getJSONObject("items");
	            int min=0;
	            String max="*";
	            if(property.has("minItems")) {
	                min=property.optInt("minItems");
	            }
	            if(property.has("maxItems")) {
	                int optMax=property.optInt("maxItems");
	                max = Integer.toString(optMax);
	            }
	            cardinality=Integer.toString(min) +  ".." + max;
	            
		    	String type = Utils.type(item,null);
		    	
		    	// System.out.println("addProperty:: " + type + " cardinailty=" + cardinality);
		    	
	            addProperty(resource, p, type, cardinality, isRequired);
				
	        } else if(property.has("type")) {
	            addProperty(resource, p, Utils.type(property,null), cardinality, isRequired);
	        }
	        
	        
	    });
	    
	}
	
	private boolean presentEnumForResource(String resource) {
		return !Utils.isBaseType(this.resource, resource);
	}


	private String getReference(JSONObject property, String items, String ref) {
        return property.getJSONObject(items).getString(ref).replaceAll(".*/([A-Za-z0-9.]*)", "$1");
	}


	private String getReference(JSONObject property, String ref) {
		return property.getString(ref).replaceAll(".*/([A-Za-z0-9.]*)", "$1");
	}


	private boolean showRelationship(JSONObject swagger, String resource, String part) {
		return !(Utils.isSimpleType(swagger,part) || Utils.isBaseType(this.resource,resource));
	}


	private void addDescription(String resource, String description) {
	    addNode(resource);
	    
	    GraphNode node = graphNodes.get(resource);
	    
	    LOG.trace("addDescription: node="+resource + " description=" + description );
	    
	    node.description = description; 
	   	
	}


	private void addRequired(String resource, JSONArray args) {
	    LOG.debug("addRequired: resource=" + resource + " args=" + args );

		required.put(resource, args);
	}

	public JSONArray getRequired(String resource) {
		JSONArray res=new JSONArray();
		if(required.containsKey(resource)) {
			res=required.get(resource);
		}
		return res;
	}

	public boolean isRequired(String resource, String property) {
		boolean res=false;
	    LOG.debug("isRequired: resource=" + resource + " property=" + property );

		if(required.containsKey(resource)) {
			JSONArray list=required.get(resource);
		    LOG.debug("isRequired: list=" + list);
		    if(list!=null)
		    	res = list.toList().stream().filter(o -> o instanceof String).map(o -> (String)o).anyMatch(s -> s.equals(property));
		}
		return res;
	}
	
	public void addEnum(String type, EnumNode enumNode) {
		enumNodes.put(type, enumNode);
	}
	
	public EnumNode processEnum(String resource, String type, JSONObject swagger) {
	    LOG.debug("processEnum: type="+type+" resource="+resource);

	    if(!enumMapping.containsKey(resource)) {
		    enumMapping.put(resource, new LinkedList<>());
	    }
	    if(!Utils.isEnumType(swagger,type)) return null;
	    
	    enumMapping.get(resource).add(type);

	    if(processedSwagger.contains(type)) {
	    	EnumNode enode = this.enumNodes.get(type);
	    	// addEnum(type,enode);
	    	return enode;
	    }
	    
	    EnumNode enode = new EnumNode(type);
	    addEnum(type,enode);
	    				
	    LOG.debug("processEnum: type="+type+" resource="+resource);

	    JSONArray values = Utils.getDefinition(swagger,type).getJSONArray("enum");
	    
	    values.forEach(v -> {
	    	enode.addValue(v.toString());
	    });
	    
	    processedSwagger.add(type);
	    enumNodes.put(type, enode);
	    	    
	    return enode;
	    
	}
	
	void addEdge(String from, String to, Edge edge) {
	    addNode(from);
	    addNode(to);
	    GraphNode fromNode = graphNodes.get(from);
	    
	    LOG.trace("addEdge: from="+from+" to="+to+" relation=" + edge.relation);
	    
	    fromNode.outEdges.add(edge);
	    if(!fromNode.neighbours.containsKey(to)) {
	        fromNode.neighbours.put(to, new Neighbour(to));
	    }
	    fromNode.neighbours.get(to).edges.add(edge);
	    fromNode.outboundNeighbours.add(to);
	    
	    graphNodes.get(to).inboundNeighbours.add(from);

	}
	
	void addProperty(String node, String property, String type, String cardinality, boolean required) {    
		addNode(node);

		GraphNode graphNode = graphNodes.get(node);
		graphNode.properties.add(new Property(property, type, cardinality, required));
	
	}
	
	List<String> extractCoreGraph(String resource) {
	    List<String> core = new ArrayList<>(nodes);
	    List<String> nonCore = new ArrayList<>();
	    List<String> remove;
	    
	    do {
	        LOG.debug("extractCoreGraph: nonCore=" + nonCore);

	    	remove = new ArrayList<>();
	        for( String n:core) {      
	            // all outbound neighbours must be nonCore
	            Set<String> outbound=new HashSet<>(getOutboundNeighbours(n));
	            Set<String> inbound=new HashSet<>(getInboundNeighbours(n));

	            outbound.removeAll(nonCore);

	            if(outbound.size()==0 && inbound.size()<=1) {
	                remove.add(n);
	                nonCore.add(n);
	            } 
	        };

	        remove.forEach(n -> core.remove(n));
	        
	    } while(remove.size()>0);

        LOG.debug("extractCoreGraph: " + core);

	    return core;
	}
	
	Set<String> getOutboundNeighbours(String node) {
		if(graphNodes.containsKey(node)) {
			return new HashSet<>(graphNodes.get(node).outboundNeighbours);
		}
		return new HashSet<>();
	}

	
	Set<String> getInboundNeighbours(String node) {
		if(graphNodes.containsKey(node)) {
			return new HashSet<>(graphNodes.get(node).inboundNeighbours);
		}
		return new HashSet<>();
	}
	
	Set<String> getAllNeighbours(String node) {
		Set<String> s = getInboundNeighbours(node);
		s.addAll(getOutboundNeighbours(node));
		return s;
	}
			
		
	boolean forceLeftRight(ClassEntity cls, Place horizonalDirection, String pivot, String toNode) {
		boolean res=false;
		Place place = Place.mapping.get(horizonalDirection);
		placeAt(toNode,place,pivot);			
		res = cls.addEdge(new HiddenEdge(pivot,place,toNode));
		return res;
	}
	
	boolean placeEdgesToNeighbours(ClassEntity cls, String node, List<String> placeNodes, String rule, Place direction, List<String> processed) {
		return placeEdgesToNeighbours(cls, node, placeNodes, rule, direction, processed, null, Place.LEFT);
	}
	
	boolean placeEdgesToNeighbours(ClassEntity cls, String node, List<String> placeNodes, String rule, 
			Place placeDirection, List<String> processed, String pivot, Place horizonalDirection) {
		
		boolean res=false;
	    boolean first=true;

        LOG.debug("placeEdgesToNeighbours: node=" + node + " placeNodes=" + placeNodes + " placeDirection=" + placeDirection);

        String prev=null;
	    for( String toNode: placeNodes) {
	        boolean d = placeEdges(cls, node, toNode, placeDirection, rule, processed);
	        res = res || d;
	        if(d && pivot!=null) {
	            if(first) {
	                cls.addEdge(new HiddenEdge(toNode, Place.ABOVE, node));
	                first=false;
	            }
	            forceLeftRight(cls, horizonalDirection, pivot, toNode);
	            placeAt(toNode,Place.BELOW,pivot);
	            pivot=toNode;
	        };
	        if(prev!=null) {
	        	this.positions.position(toNode, prev, horizonalDirection==Place.LEFT ? Place.RIGHT : Place.LEFT);
	        }
            prev=toNode;
	    };
		return res;
	}

	boolean placeEdges(ClassEntity cls, String fromNode, String toNode, Place direction, String rule, List<String> processed) {
		boolean res=false;

		LOG.debug("placeEdges: isForced=" + direction.isForced());

    	if(direction.isForced()) {
        	res = placeForced(cls, fromNode, toNode, direction);
        	cls.addComment(new Comment("' rule: " + rule));
    	} else {
    		if(graphNodes.get(fromNode).neighbours.containsKey(toNode)) {
    			Neighbour from=graphNodes.get(fromNode).neighbours.get(toNode);
	    		    	
		        boolean d = from.edges.stream()
		        		.filter(edge -> !edge.isPlaced(processed))
		        		.map(edge -> {
		        			return placeEdgeHelper(cls, direction, fromNode, edge, processed);
		        		})
		        		.reduce(false, (a,b) -> a || b);
		        		
				LOG.debug("placeEdges: d=" + d);

		        if(d) {
		            cls.addComment(new Comment("' rule: " + rule));
		        }
		        res = res || d;
	    	}
	    }
		LOG.debug("placeEdges: res=" + res);

		return res;
	}
	
	boolean placeEdgeHelper(ClassEntity cls, Place direction, String from, Edge edge, List<String> processed) {
		boolean res = false;

		LOG.debug("placeEdgeHelper: direction=" + direction + " from=" + from + " edge=" + edge);
		
		if(edge.isPlaced(processed)) return res;
		edge.placed(processed);
		
		String to = edge.related;
		
	    placeAt(to, direction, from);
	    positions.position(from, to, direction);
	    res = cls.addEdge(new EdgeEntity(direction,edge));
		
		LOG.debug("placeEdgeHelper: diagram=" + res);
		return res;	
	}
	
	
	boolean placeForced(ClassEntity cls, String from, String to, Place direction) {
		boolean res=false;
		LOG.debug("placeForced: direction=" + direction + " from=" + from + " to=" + to);
		
		Place coreDirection = Place.coreDirection.get(direction);
		
	    placeAt(to,coreDirection, from);
	    placeAt(from,Place.mapping.get(coreDirection), to);
	    positions.position(from, to, coreDirection); // positionBelow(from,to);
	    res = cls.addEdge(new HiddenEdge(from, coreDirection, to));
		
		return res;	
	}
	
	
	void placeAt(String node, Place direction, String pivot) {			
		placeAtHelper(node, direction, pivot);			
		placeAtHelper(pivot, Place.mapping.get(direction), node); // TODO	
	}
	
	void placeAtHelper(String node, Place direction, String pivot) {	
		LOG.trace("placeAtHelper: node=" + node + " direction=" + direction + " pivot=" + pivot);
		if(!graphNodes.containsKey(node)) {
			graphNodes.put(node, new GraphNode());
		}
		if(!graphNodes.get(node).placements.containsKey(direction)) {
			graphNodes.get(node).placements.put(direction, new ArrayList<>());
		}
		graphNodes.get(node).placements.get(direction).add(pivot);		
	}
	
	boolean isPlaced(String node) {
		boolean res = false;
		
		res = graphNodes.containsKey(node) && !graphNodes.get(node).placements.isEmpty();
		
		res = res || graphNodes.containsKey(node) && graphNodes.get(node).placements.containsKey(Place.FORCEFLOAT);
		
		if(!res) {
			Optional<Optional<Place>> placed = graphNodes.keySet().stream()
				.map(n -> graphNodes.get(n).placements.keySet().stream()
								.filter(d -> graphNodes.get(n).placements.get(d).contains(node)).findAny())
				.filter( found -> found.isPresent()).findFirst();
			
			res = res || (placed.isPresent() && placed.get().isPresent());	
			LOG.trace("isPlaced: node=" + node + " placed=" + placed + " res=" + res);				 

		}
		
        LOG.trace("isPlaced: node=" + node + " res=" + res);
        
        return res;
	}
	
	boolean isPlacedAt(String node, Place direction) {
		boolean res = !getPlacedAt(node, direction).isEmpty();

        LOG.trace("isPlacedAt: node=" + node + " direction=" + direction + " res=" + res);

        return res;
	}
	
	
	List<String> getPlacedAt(String node, Place direction) {
		if(graphNodes.containsKey(node) && graphNodes.get(node).placements.containsKey(direction))
			return graphNodes.get(node).placements.get(direction).stream().distinct().collect(Collectors.toList());
		else
			return new ArrayList<>();
	}
	
	String getEnumPlacedAt(String node, Place direction) {
		String res = "";
		
		List<String> placed = getPlacedAt(node, direction);	
		List<String> enums = getEnums(node);	

		res = placed.stream()
				.filter(n -> enums.contains(n))
				.sorted(Comparator.comparing(n -> this.positions.get(n).x))
				.reduce((first, second) -> second)
				.orElse(null);				
						
		
        LOG.debug("getEnumPlacedAt: node=" + node + " res=" + res);
		return res;
	}
	
	Set<String> getPlacedNodes() {
		return graphNodes.keySet().stream().filter(n -> isPlaced(n)).distinct().collect(Collectors.toSet());
	}

	
	Map<Place,List<String>> getPlaced(String node) {
		if(graphNodes.containsKey(node))
			return graphNodes.get(node).placements;
		else
			return new HashMap<>();
	}
	
	boolean isSingleFromNode(String node) {
	    Set<String> fromNodes=getInboundNeighbours(node);

	    return (fromNodes.size()<=1) || (fromNodes.size()==2 && fromNodes.contains(node));
	}
	
	boolean isLeafNode(String node) {
	    Set<String> toNodes=getOutboundNeighbours(node);
	    return toNodes.isEmpty();
	}
	
	List<Edge> getEdges(String from, String to) {	
		return graphNodes.get(from).outEdges.stream()
					.filter(edge -> edge.related.equals(to)).collect(Collectors.toList());

	}
	
	boolean layoutEnums(Diagram diagram, ClassEntity cls, String node, boolean recursive) {
		boolean placed=false;
		
		List<String> enumCandidates = this.enumMapping.get(node);
		
    	LOG.debug("layoutEnums: node=" + node + " enumCandidates=" + enumCandidates);

    	if(enumCandidates==null) return false;

	    boolean floatingEnums = CONFIG.getFloatingEnums();
    	LOG.debug("layoutEnums: floatingEnums=" + floatingEnums);

	    if(floatingEnums) {
		    for(String enode : enumCandidates) {	
		    	this.setPlacementFloating(enode);
		    }
		    return true;
	    }
	        	
	    List<EnumNode> enums = this.enumNodes.values().stream()
	    							.filter(e -> enumCandidates.contains(e.type))
	    							.filter(e -> !isPlaced(e.type))  // no links for enums, so only need to be processed/placed once
	    							.collect(Collectors.toList());
	    
    	LOG.debug("layoutEnums: enums=" + enums );
	    
	    String current=node;
	    Place prevDirection=null;
	    for(EnumNode enode : enums) {	     
	    	LOG.debug("layoutEnums: enode=" + enode);
	        //
	        // place to right if only one node already to right AND also something to left
	        //
	        int diffX=Integer.MAX_VALUE;
	        if(isPlacedAt(node,Place.RIGHT) && isPlacedAt(node,Place.LEFT) && prevDirection==null) {

	            List<String> atRight=positions.getRightmostOf(this, node).stream()
	            		.sorted(Comparator.comparing(n -> -this.positions.get(n).x))
	            		.filter(n -> !isFloatingNode(n, node))
	            		.collect(Collectors.toList());
	            
		    	LOG.debug("layoutEnums: atRight=" + atRight);
		    	LOG.debug("layoutEnums:     pos=" + atRight.stream().map(n->this.positions.get(n)).collect(Collectors.toList()));

	            if(!atRight.isEmpty()) {
	            	String rightMost=atRight.get(0);
	                diffX=positions.get(rightMost).x - positions.get(current).x;
	                if(diffX<=1) current=rightMost;
	            }
	        }

	    	LOG.debug("layoutEnums: enode=" + enode.type + " current=" + current + " recursive=" + recursive + " diffX=" + diffX);
	    	LOG.debug("layoutEnums: enode=" + enode.type + " current=" + current + " isPlacedAt " + Place.LEFT + " = " + isPlacedAt(current,Place.LEFT));
	    	LOG.debug("layoutEnums: enode=" + enode.type + " current=" + current + " isPlacedAt " + Place.RIGHT + " = " + isPlacedAt(current,Place.RIGHT));

	    	LOG.debug("layoutEnums: enode=" + enode.type + " current=" + current + " recursive=" + recursive + " diffX=" + diffX);

	        if(!isPlacedAt(current,Place.LEFT) || recursive || (diffX<=1) || prevDirection==Place.LEFT) {
	        	cls.addEdge(new EnumEdge(current, Place.RIGHT, enode.type));
	            placeAt(current,Place.LEFT,enode.type);
	            positions.positionToRight(current,enode.type);
		        enode.setPlacement(current, Place.LEFT);
		        prevDirection=Place.LEFT;
		        
	        } else if(!isPlacedAt(current,Place.RIGHT) || prevDirection==Place.RIGHT) {
	        	cls.addEdge(new EnumEdge(current, Place.LEFT, enode.type));
	            placeAt(current,Place.RIGHT,enode.type);
	            positions.positionToLeft(current,enode.type);
		        enode.setPlacement(current, Place.RIGHT);
		        prevDirection=Place.RIGHT;
	        } else {
	        	cls.addEdge(new EnumEdge(current, Place.BELOW, enode.type));
	            placeAt(current,Place.ABOVE,enode.type);
	            positions.positionBelow(current,enode.type);
		        enode.setPlacement(current, Place.ABOVE);
		        prevDirection=Place.ABOVE;
		    }
	        
	    	LOG.debug("layoutEnums: #1 enode=" + enode);
	        diagram.addEnum(new EnumEntity(enode)); 
        	placed=true;
	    	LOG.debug("layoutEnums: #2 enode=" + enode);

	        current=enode.type;
	    }
	    
	    return placed;
	}
		
	
	private void setPlacementFloating(String node) {
		LOG.trace("setPlacementFloating: node=" + node);
		if(!graphNodes.containsKey(node)) {
			graphNodes.put(node, new GraphNode());
		}
		graphNodes.get(node).placements.put(Place.FORCEFLOAT, new ArrayList<>());
	}


	Optional<String> hasAlreadyPlacedNeighbours(String node, String exclude) {
		Optional<String> res= Optional.empty();
		
		boolean allInboundPlaced = getInboundNeighbours(node).stream()
									.filter(p -> !p.equals(exclude))
									.allMatch(p -> isPlaced(p));
								
		if(allInboundPlaced) 
			res = Optional.of(node);
		
    	LOG.debug("hasAlreadyPlacedNeighbours: node=" + node + " res=" + res );

		return res;
	}
	
	Optional<String> hasPlacedSomeOutboundNeighbours(String node, String exclude) {
		Optional<String> res= Optional.empty();
		
		boolean allInboundPlaced = getOutboundNeighbours(node).stream()
									.filter(p -> !p.equals(exclude))
									.anyMatch(p -> isPlaced(p));
								
		if(allInboundPlaced) 
			res = Optional.of(node);
		
    	LOG.debug("hasPlacedOutboundNeighbours: node=" + node + " res=" + res );

		return res;
	}
	
    
	boolean layoutWithExistingNodes(ClassEntity cls, String node, List<String> neighbours, List<String> includeNodes, List<String> processed) {
		boolean res=false;
		
		LOG.debug("layoutWithExistingNodes: node=" + node + " neighbours=" + neighbours);    		
				
		boolean placed;
		for(String toNode : neighbours) { // TODO placedNeighbours) {
			placed = placeWithPlaced(cls, node, toNode, processed);
			res = res || placed;
		}
		
		return res;
		
	}
	
	boolean isFloatingNode(String node, String related) {
		boolean res=false;
		Set<String> neighbours=getNeighbours(node);
		neighbours.remove(related);
		res=neighbours.size()==1;
    	LOG.debug("isFloatingNode: node=" + node + " related=" + related + " res=" + res + " neighbours=" + neighbours);    		

		return res;
	}
	
	boolean placeWithPlaced(ClassEntity cls, String nodeA, String nodeB, List<String> processed) {
		boolean res=false;
		
    	boolean floatingNodeA = isFloatingNode(nodeA, nodeB);
    	
    	LOG.debug("placeWithPlaced: nodeA=" + nodeA + " nodeB=" + nodeB + " floatingNodeA=" + floatingNodeA);    		

	    if(!positions.isPositionedToLeft(nodeA, nodeB) && !floatingNodeA)  {
	    	String tmp = nodeA;
	    	nodeA = nodeB;
	    	nodeB = tmp;
	    }
		 
		String rule = "";
		List<Place> func = null;
		
	    if(!floatingNodeA && positions.isAtSameColumn(nodeA, nodeB)) { 
	    	if(positions.isPositionedAbove(nodeA, nodeB)) { 
	    		rule="P01-1";
	    		func = funcBelowAbove;
	    		
	    	} else if(positions.isPositionedBelow(nodeA, nodeB)) {
	    		rule="P01-2";
	    		func = funcAboveBelow;
	    		
	    	} else {
	    		LOG.debug("ERROR: nodeA and nodeB in same position - should NOT happen");
	    		LOG.debug("ERROR:    nodeA = " + nodeA);
	    		LOG.debug("ERROR:    nodeB = " + nodeB);
	    	}
	    	
	    } else {
	    	if(positions.isPositionedAbove(nodeA, nodeB)) { 
	    		rule="P02-1";
	    		func = funcBelowAbove; 

	    	} else if(positions.isPositionedBelow(nodeA, nodeB)) {
	    		rule="P02-2";
	    		func = funcAboveBelow; 
	    		
	    	} else {
	    		
	    		if(floatingNodeA) {
	    			if(!isPlacedAt(nodeB,Place.RIGHT)) {
	    				func = funcRightLeft;
	    			} else if(!isPlacedAt(nodeB,Place.LEFT)) {
	    				func = funcLeftRight;
	    			} else {
	    				if(isPlacedEnumAt(nodeB,Place.RIGHT)) {
		    				func = funcRightLeft;
	    				} else if(isPlacedEnumAt(nodeB,Place.LEFT)) {
		    				func = funcLeftRight;
	    				} else
	    					func = funcBelowAbove;
	    			}
	    		} else {
		    		Position posA=positions.get(nodeA);
		    		int inboundFromSameLevel = getInboundNeighbours(nodeB).stream()
		    									.map(n -> positions.get(n))
		    									.filter(p -> p.y == posA.y)
		    									.mapToInt(e -> 1)
		    									.sum();
		    		
		            LOG.debug("placeWithPlaced #2-3 inboundFromSameLevel=" + inboundFromSameLevel );
	
		    		switch(inboundFromSameLevel) {
		    		case 2:
		    			if(!isPlacedAt(nodeA,Place.LEFT) && !isPlacedAt(nodeB,Place.RIGHT)) {
		    				func = funcRightLeft;
		    			} else if(!isPlacedAt(nodeB,Place.LEFT) && !isPlacedAt(nodeA,Place.RIGHT)) {
		    				func = funcLeftRight;
		    			} else {
					        func = funcBelowAbove;
		    			}
		    			break;
		    			
		    		case 1:
			    		Optional<String> optCommon = getCommonPathEnd(nodeA,nodeB);
			    		
				        func = inboundFromSameLevel>1 ? funcBelowAbove: funcRightLeft;
				        
				        if(isPlacedAt(nodeA,Place.LEFT) && isPlacedAt(nodeA,Place.RIGHT)) func = funcAboveBelow;
				        if(isPlacedAt(nodeA,Place.LEFT) && !isPlacedAt(nodeA,Place.RIGHT)) func = funcLeftRight; // funcLeftRight;
				        if(!isPlacedAt(nodeA,Place.LEFT) && isPlacedAt(nodeA,Place.RIGHT)) func = funcRightLeft; // funcRightLeft;
				        if(!isPlacedAt(nodeA,Place.LEFT) && !isPlacedAt(nodeA,Place.RIGHT)) func = funcRightLeft; // funcRightLeft;
	
			        	long countAtLevel = positions.currentlyPlacedAtLevel(nodeA);
			        	
			            func = countAtLevel<=4 ? func : funcBelowAbove;
			            LOG.debug("placeWithPlaced #2-3 countAtLevel=" + countAtLevel + " optCommon=" + optCommon);
			            break;
			         
			        default:
				        func = funcBelowAbove;
		    			break;
		    		}
	    		}
	    		
	    		rule="placeWithPlaced P02-3";
	    		
	    	}

	    }
	    
	    if(func!=null) {
    		if(isPlacedEnumAt(nodeB,func.get(0))) {
    			String enumNode=getEnumPlacedAt(nodeB,func.get(0));
            	cls.addEdge(new ForcedHiddenEdge(enumNode, func.get(0), nodeA));
    		}
    		res = placeEdgePackage(cls,nodeA,nodeB,func,rule,processed);
	    	cls.addComment(new Comment("' finished with " + rule));
    		LOG.debug("rule: " + rule + " finished");
	    }
	   
		return res;
		
	}
	
	
	private boolean isPlacedEnumAt(String node, Place direction) {
		boolean res = false;
		
		List<String> placed = getPlacedAt(node, direction);	
		List<String> enums = getEnums(node);	

		res = placed.stream().anyMatch(n -> enums.contains(n));
		
        LOG.debug ("isPlacedEnumAt: node=" + node + " direction=" + direction + " res=" + res);

        return res;
	}


	private List<String> getEnums(String node) {
		List<String> res = new LinkedList<>();
		if(enumMapping.get(node)!=null) {
			res.addAll(enumMapping.get(node));
		}
		return res;
	}


	boolean placeBetween(ClassEntity cls, String nodeA, String nodeB, String toNode, List<String> processed) {
		boolean res=false;
		
	    if(!positions.isPositionedToLeft(nodeA, nodeB))  {
	    	String tmp = nodeA;
	    	nodeA = nodeB;
	    	nodeB = tmp;
	    }

	    boolean multipleBetween = this.isMultipleBetween(nodeA, nodeB);
	    boolean placedBetween = this.isPlacedBetween(nodeA, nodeB);
	    boolean directConnection = this.hasDirectConnection(nodeA, nodeB);
	    
	    Set<String> outbound = this.getOutboundNeighbours(toNode);
	    outbound.remove(nodeA);
	    outbound.remove(nodeB);
	    boolean hasOutbound = outbound.size()>0;

	    LOG.debug("placeBetween: nodeA: " + nodeA + " nodeB: " + nodeB + " toNode: " + toNode );
	    LOG.debug("placeBetween: multipleBetween: " + multipleBetween + 
	    			" hasOutbound: " + hasOutbound + " placedBetween: " + placedBetween + 
	    			" directConnection: " + directConnection);

	    if(isPlaced(toNode)) {
	    	return placeBetweenToNodeAlreadyPlaced(cls, nodeA,nodeB,toNode,processed);
	    }

	    String rule = "placeBetween - " + nodeA + " < " + nodeB + " placing " + toNode + " multipleBetween=" + multipleBetween;

        List<Place> func;
        
	    if(!placedBetween && !directConnection ) {
	        // nothing between
	        if(positions.isAtSameLevel(nodeA,nodeB)) {
	            // just right-left as nothing between and the two nodes are at the same level
	        	// place between if multiple nodes between, otherwise vertically
	        	String ruleDetails = rule + " !between && sameLevel";

	        	long countAtLevel = positions.currentlyPlacedAtLevel(nodeA);
	        	boolean placed = false;
	        	boolean horizontal = countAtLevel<=4 && (multipleBetween || hasOutbound);
	        	
	            func = horizontal ? funcRightLeft : funcBelowAbove; // TODO: or funcAboveBelow
	            LOG.debug("placebetween #1-1 countAtLevel=" + countAtLevel + " multipleBetween=" + multipleBetween + " hasOutbound=" + hasOutbound + " horizontal=" + horizontal);
	            placed = placeEdgePackage(cls, nodeA,toNode,func,ruleDetails,processed);
	            res = res || placed;

	            func = horizontal ? funcLeftRight : funcBelowAbove; // TODO: or funcAboveBelow
	            LOG.debug("placebetween #1-2");
	            placed = placeEdgePackage(cls, nodeB,toNode,func,ruleDetails,processed);
	            res = res || placed;
	            
	        } else {
	        	String ruleDetails = rule + " !between && !sameLevel";
	        	boolean placed = false;

	            func = !this.isPlacedAt(nodeB, Place.BELOW) ? funcRightLeft : 
	            			positions.isPositionedAbove(nodeA,nodeB) ? funcBelowAbove : funcAboveBelow;
	        	LOG.debug("placebetween #2-1");
	        	placed = placeEdgePackage(cls, nodeA,toNode,func,ruleDetails,processed);
	            res = res || placed;

	            // func = !positions.isPositionedAbove(nodeA,nodeB) ? funcBelowAbove : funcAboveBelow;
	            func = this.isPlacedAt(nodeB, Place.BELOW) ? funcLeftRight : 
        				  positions.isPositionedAbove(nodeA,nodeB) ? funcBelowAbove : funcAboveBelow; // TODO was !
	            LOG.debug("placebetween #2-2");
	            placed = placeEdgePackage(cls, toNode,nodeB,func,ruleDetails,processed);
	            res = res || placed;
	        }
	    } else {
	        // something between
	        if(positions.isAtSameLevel(nodeA,nodeB)) {
	            // something already between and the two nodes are at the same level
	        	// place below unless there is also another path between
	        	
	        	boolean anotherPath = isPath(nodeA, nodeB, Arrays.asList(toNode));
	        	
	        	String ruleDetails = rule + " between && sameLevel";
	        	boolean placed = false;

	            // func = anotherPath ? funcAboveBelow : funcBelowAbove;
//	        	if(multipleBetween) {
//	        		if(placedBetween) {
//	        			func = funcBelowAbove;
//	        		} else {
//	        			func = positions.isPositionedAbove(nodeA, toNode) ? funcBelowAbove : funcAboveBelow;
//	        		}
//	        	} else {
//	        		// func = funcRightLeft;
//	        		func = anotherPath ? funcAboveBelow : funcBelowAbove;
//	        	}
	        	if(!anotherPath) {
	        		func = funcAboveBelow;
	        	} else {
	        		func = funcBelowAbove;
	        	}
	        	
	        	LOG.debug("not placebetween #3-1: rule=" + rule);
	        	placed = placeEdgePackage(cls, nodeA,toNode,func,ruleDetails,processed);
	            res = res || placed;
	        	
	            LOG.debug("not placebetween #3-2: rule=" + rule);
	            placed = placeEdgePackage(cls, nodeB,toNode,func,ruleDetails,processed);
	            res = res || placed;

	        } else {
	            // the nodeA and nodeB are not at the same level
	        	String ruleDetails = rule + " between && !sameLevel";
	        		        	
	        	boolean placed = false;

	        	if(multipleBetween) {
		            func = positions.isPositionedAbove(nodeA, nodeB) ? funcBelowAbove : funcAboveBelow;
	        	} else {
	        		func = funcRightLeft;
	        	}
	            LOG.debug("not placebetween #4-1");	   
	                     		        
	            placed = placeEdgePackage(cls, nodeA,toNode,func,ruleDetails,processed);

	            LOG.debug("not placebetween #4-1: placed=" + placed);	   

	            if(placed && !multipleBetween) {
	                String A = nodeA;
		            Set<String> candidates = this.getInboundNeighbours(A).stream()
		            							.map(n-> getOutboundNeighbours(n))
		            							.flatMap(Set::stream)
		            							.filter(n -> !n.equals(toNode) && !n.equals(A))
		            							.filter(n -> positions.placedAtLevel(A).contains(n))
		            							.collect(Collectors.toSet());
		   		    LOG.debug("not placebetween #4-1: candidates=" + candidates);

		   		    // TODO
//		   		    String to=A;
//		   		    for(String c:candidates) {
//	                	cls.addEdge(new HiddenEdge(c, Place.RIGHT, to));
//	                	to=c;
//		   		    }
		   		    
	            }
	            
	            res = res || placed;

	            func = positions.isPositionedAbove(nodeA, nodeB) ?  funcAboveBelow : funcBelowAbove;
				LOG.debug("not placebetween #4-2");		
				placed = placeEdgePackage(cls, nodeB,toNode,func,ruleDetails,processed);
	            res = res || placed;

	        }
	    }
		
		return res;
	}
	
	boolean placeBetweenToNodeAlreadyPlaced(ClassEntity cls, String nodeA, String nodeB, String toNode, List<String> processed) {
		boolean res=false;

		boolean placed;
	    placed = placeEdgesBetweenNodesAlreadyPlaced(cls, toNode,nodeA, nodeB, processed);
	    res = res || placed;
	    
	    placed = placeEdgesBetweenNodesAlreadyPlaced(cls, toNode,nodeB, nodeA, processed);
	    res = res || placed;
	    
		return res;
	}
	

	boolean placeEdgesBetweenNodesAlreadyPlaced(ClassEntity cls, String nodeA, String nodeB, String nodeC, List<String> processed) {
	    boolean res=false;
		List<Place> func;

	    boolean multipleBetween = this.isMultipleBetween(nodeB, nodeC);

	    if(!positions.isPositionedToLeft(nodeA, nodeB))  {
	    	String tmp = nodeA;
	    	nodeA = nodeB;
	    	nodeB = tmp;
	    }
	    
	    LOG.debug("placeEdgesBetweenNodesAlreadyPlaced: nodeA: " + nodeA + " nodeB: " + nodeB + " nodeC: " + nodeC);
	
	    String rule = "placeEdgesBetweenNodesAlreadyPlaced - " + nodeA + " < " + nodeB ;
	
	    // boolean placeBetween = !isPlacedBetween(nodeA,nodeB) && positions.isAtSameLevel(nodeA,nodeB);
	    if(!multipleBetween && !isPlacedBetween(nodeA,nodeB) && positions.isAtSameLevel(nodeA,nodeB)) {
	        // at same level and nothing in between => left right
	        // if already some node to left, we place to right
	        List<String> alreadyPlacedToLeftOfNodeA = getPlacedAt(nodeA,Place.RIGHT);
	        List<String> alreadyPlacedToRightOfNodeA = getPlacedAt(nodeA,Place.LEFT);
	        Set<String> othersAtLeft = Utils.difference(alreadyPlacedToLeftOfNodeA, Arrays.asList(nodeA,nodeB)); 
	        Set<String> othersAtRight = Utils.difference(alreadyPlacedToRightOfNodeA, Arrays.asList(nodeA,nodeB)); 
	
	        if(othersAtLeft.size()==0) {
	            func = funcRightLeft; 
	            LOG.debug("placeEdgesBetweenNodesAlreadyPlaced #1-1");
	        } else if(othersAtRight.size()==0) {
	        	func = funcLeftRight; 
	            LOG.debug("placeEdgesBetweenNodesAlreadyPlaced #1-2");
	        } else {
	        	func = funcBelowAbove;
	            LOG.debug("placeEdgesBetweenNodesAlreadyPlaced #1-3");
	        }
	        res = placeEdgePackage(cls,nodeA,nodeB,func,rule,processed);
	        
	    } else if(positions.isAtSameLevel(nodeA,nodeB)) {
	        //
	        // possibly same level, but not placing left-right
	        // 
	        boolean nodeCisAbove = positions.isPositionedAbove(nodeC,nodeA) || positions.isPositionedAbove(nodeC,nodeB);
	        func = nodeCisAbove ? funcAboveBelow : funcBelowAbove;
	        LOG.debug("placeEdgesBetweenNodesAlreadyPlaced #3-1");
	        res = placeEdgePackage(cls, nodeA, nodeB, func, rule, processed);
	        
	    } else {
	        //
	        // not at same level - above below 
	        // 
	        func = positions.isPositionedAbove(nodeA,nodeB) ? funcBelowAbove : funcAboveBelow;
	        		
	        LOG.debug("placeEdgesBetweenNodesAlreadyPlaced #2-1");
	        res = placeEdgePackage(cls, nodeA,nodeB,func,rule,processed);

	    }
	    LOG.debug("placeBetweenNodesAlreadyPlaced: done");
	
	    return res;
	}

	
	boolean placeEdgePackage(ClassEntity cls, String nodeA, String nodeB, List<Place> func, String rule, List<String> processed) {
	   boolean res = false;
	   
       LOG.debug("placeEdgePackage: nodeA=" + nodeA + " nodeB=" + nodeB + " func=" + func);

	   boolean placed;
	   placed = placeEdges(cls, nodeA, nodeB, func.get(0), rule, processed);
	   res = res || placed;
	   
	   placed = placeEdges(cls, nodeB, nodeA, func.get(1), rule, processed);
	   res = res || placed;
	   
	   return res;
	}
	
	List<String> getNodesWithPaths(String to, String exclude) {
		List<String> res = new ArrayList<>();
		
		LOG.debug("getNodesWithPaths: to=" + to + " exclude=" + exclude);
	    
		getNodes().stream()
	        .filter(from -> isPlaced(from))
	        .filter(from -> !from.equals(to) && !from.equals(exclude))
	        .forEach(from -> {
	            String last=getLastElementOfPath(from,to,exclude, new ArrayList<>());
	            if(last!=null) {
	                res.add(last);
	            } else {
	                last=getLastElementOfPath(to,from,exclude, new ArrayList<>());
	                if(last!=null) res.add(last);
	            }
	        });
	    LOG.debug("getNodesWithPaths: res=" + res);
		return res;
	}
	
	String getLastElementOfPath(String from, String to, String exclude, List<String> seen) {
	    String res = null;
	    
		LOG.debug("getLastElementOfPath: from=" + from + " to=" + to + " exclude=" + exclude + " seen=" + seen);

	    if(to.equals(exclude) || to.equals(from) || seen.contains(from)) return res;
	    seen.add(from);
	    List<String> out = getOutboundNeighbours(from).stream().filter(x-> !x.equals(exclude)).collect(Collectors.toList());
	    if(out.contains(to))
	        return from;
	    else {
	        for(String n:out) {
	            res=getLastElementOfPath(n,to,exclude,seen);
	            if(res!=null) break;
	        };
	    }
	    // LOG.debug("getLastElementOfPath: res=" + res);
	    
	    return res;
	}
	
	boolean isConnection(String from, String to) {
		
		Set<String> outbound = getOutboundNeighbours(from);
		if(outbound.contains(to)) return true;
		
		boolean connection = outbound.stream()
								.anyMatch(n -> isConnection(n,to));
		
		if(connection) return connection;
		
		outbound = getOutboundNeighbours(to);
		if(outbound.contains(from)) return true;

		connection = outbound.stream()
						.anyMatch(n -> isConnection(n,from));
		
		return connection;
		
	}
	
	boolean isPath(String from, String to, List<String> exclude) {
		return isPath(from, to, exclude, new ArrayList<>(), true);
	}
	
	boolean isPlacedBetween(String nodeA, String nodeB) {
	    LOG.trace("isPlacedBetween: nodeA: " + nodeA + " nodeB: " + nodeB);
	    Set<String> rightA = new HashSet<>(getPlacedAt(nodeA,Place.LEFT));
	    rightA.removeAll(Arrays.asList(nodeA,nodeB));
	    
	    Set<String> leftB = new HashSet<>(getPlacedAt(nodeB,Place.RIGHT));
	    leftB.removeAll(Arrays.asList(nodeA,nodeB));

	    boolean isBetween = rightA.size()>0 || leftB.size()>0;

	    LOG.trace("isPlacedBetween: " + isBetween);
	    return isBetween;
	}
	
	
	boolean isPath(String from, String to, List<String> exclude, List<String> seen, boolean first) {
	
	    LOG.debug("isPath: from=" + from + " to=" + to + " exclude="+ exclude + " seen=" + seen + " first=" + first);

		if(exclude.contains(to) || to.equals(from) || seen.contains(from)) return false;
	    seen.add(from);

	    Optional<String> res = getOutboundNeighbours(from).stream()
			    					.filter(n -> isPlaced(n))
			    					.filter(n -> !exclude.contains(n))
			    					.filter(n -> (n.equals(to) &&!first) || isPath(n,to,exclude,seen,false))
			    					.findFirst();
	    
	    if(!res.isPresent()) {
		    res = getInboundNeighbours(from).stream()
					.filter(n -> isPlaced(n))
					.filter(n -> !exclude.contains(n))
					.filter(n -> (n.equals(to) && !first) || isPath(n,to,exclude,seen,false))
					.findFirst();
	    }
	    
	    LOG.debug("isPath: res=" + res + " == " + res.isPresent());

	    return res.isPresent();
	}
	
	boolean hasDirectConnection(String from, String to) {
	    boolean res = isEdgeBetween(from,to) || isEdgeBetween(to,from) || isEnumNeighbour(from,to);
	    LOG.debug("hasDirectConnection: from=" + from + " to=" + to + " res="+ res);
	    return res;
	}
	
	boolean isEdgeBetween(String from, String to) {
		return getOutboundNeighbours(from).contains(to);
	}
	
	boolean isEnumNeighbour(String from, String to) {
		boolean res=false;
		if(enumNodes.containsKey(from)) {
			EnumNode enode = enumNodes.get(from);
			res = res || enode.placedByNode.equals(to); 
		}
		if(enumNodes.containsKey(to)) {
			EnumNode enode = enumNodes.get(to);
			res = res || enode.placedByNode.equals(from); 
		}
		LOG.debug("isEnumNeighbour: known enums: " + enumNodes.keySet());
	    LOG.debug("isEnumNeighbour: from=" + from + " to=" + to + " res="+ res);
		return res;
	}
	
	String getNearestAbove(String node) {
	    String res=null;
	    Position closestPosition=null;

	    List<String> candidates = getPlacedAt(node,Place.BELOW);
	    if(candidates.contains(node)) candidates.remove(node);
	    
	    for(String p:candidates) {
	        Position pos=positions.get(p);
	        if(closestPosition==null) {
	            closestPosition=pos;
	            res=p;
	        } else if(pos.y>closestPosition.y) {// if(mypos.y-pos.y<mypos.y-closestPosition.y) {
	            closestPosition=pos;
	            res=p;
	        }
	    };
	    return res;
	}
	
	String getEdgeBoundary(List<String> nodes, Place direction) {
	    String res=null;
	    Position currentPosition=null;
	    
	    for( String n : nodes) {
	        Position pos=positions.get(n);
	        if(currentPosition==null) {
	            currentPosition=pos;
	            res = n;
	        } else {
	        	switch(direction) {
	        	case LEFT:
		        	if(pos.x<currentPosition.x) {
			            currentPosition=pos;
			            res = n;
			        }
	        		break;
	        		
	        	case RIGHT:
		        	if(pos.x>currentPosition.x) {
			            currentPosition=pos;
			            res = n;
			        }
	        		break;
	        		
	        	// following two should never happen	
	        	case ABOVE:
	        		break; 
	        	case BELOW:
	        		break;
	        	default:
	        	}
	        	
	        }
	    };
	    return res;
	}	
	
	String getNearestBelow(String node) {
	    String res = null;
	    Position closestPosition = null;

	    List<String> candidates = getPlacedAt(node,Place.ABOVE);
	    if(candidates.contains(node)) candidates.remove(node);
	    
	    LOG.debug("getNearestBelow: node="+node + " :: " + candidates);

	    for( String p:candidates) {
	        LOG.debug("getNearestBelow: node="+node + " p="+p);
	        Position pos=positions.get(p);
	        if(closestPosition==null) {
	            closestPosition=pos;
	            res = p;
	        } else if(pos.y<closestPosition.y) {// if(mypos.y-pos.y<mypos.y-closestPosition.y) {
	            closestPosition=pos;
	            res=p;
	        }
	    };
	    return res;
	}
	
	
	boolean placeEdgesToNeighboursBelow(ClassEntity cls, String node, List<String> neighbours, String rule, List<String> processed) {
		return 	placeEdgesToNeighboursBelow(cls, node, neighbours, rule, processed, null, Place.LEFT);
	}

	boolean placeEdgesToNeighboursBelow(ClassEntity cls, String node, List<String> neighbours, String rule, List<String> processed, String pivot, Place horizonalDirection) {

		boolean res=false;
	    boolean first=true;
	    String placed=pivot;
	    Place func = Place.BELOW;
	    
	    if(pivot!=null && !isPlaced(pivot)) {
	        LOG.debug("placeEdgesToNeighboursBelow: pivot=" + pivot + " not placed - ERROR");
	        pivot=null;
	    }
	
        LOG.debug("placeEdgesToNeighboursBelow: node=" + node + " neighbours=" + neighbours);

	    for(String toNode : neighbours) {
	        LOG.debug("placeEdgesToNeighboursBelow: node=" + node + " toNode=" + toNode + " pivot=" + pivot + " horizonalDirection=" + horizonalDirection);
	        boolean d = placeEdges( cls, node, toNode, func, rule, processed);
        	res = res || d;
	        
	        LOG.debug("placeEdgesToNeighboursBelow: node=" + node + " toNode=" + toNode + " d=" + d);

	        if(d) {
        		if(first && placed==null) {
        			positions.positionBelow(node,toNode);
        		} else if(horizonalDirection==Place.LEFT) {
                	positions.positionToLeft(placed,toNode);
                } else {
                	positions.positionToRight(placed,toNode);
                }
	            
    	        LOG.debug("placeEdgesToNeighboursBelow: node=" + node + " toNode=" + toNode + " placed in position: " + positions.get(toNode));
    	        
    	       
	            if(pivot!=null) {
	                if(first) {
	                	d = cls.addEdge(new HiddenEdge(node, Place.BELOW, toNode));
		                res = res || d;
	                } 
	                
	                Place placeDirection = Place.reverse(horizonalDirection);
	                if(isPlacedAt(node, horizonalDirection) && !isPlacedEnumAt(node, horizonalDirection))
	                	d = cls.addEdge(new ForcedHiddenEdge(toNode, placeDirection, placed));
	                else
	                	d = cls.addEdge(new HiddenEdge(toNode, placeDirection, placed));

                	placeAt(placed,placeDirection,toNode);
	                
//	                if(horizonalDirection==Place.LEFT) {
//	                	d = cls.addEdge(new HiddenEdge(toNode, Place.RIGHT, placed));
//                    	placeAt(placed,Place.RIGHT,toNode);
//                    } else {
//	                	d = cls.addEdge(new HiddenEdge(placed, Place.RIGHT, toNode));
//                    	placeAt(toNode,Place.RIGHT,placed);
//                    }
	                res = res || d;
	            } 
	            
                placed = toNode;
                first=false;	  
	        }
	    };
	    return res;
	}
	
	boolean isOnlyBetween(String target,String nodeA, String nodeB) {
		boolean res = true;
	    res = res && getInboundNeighbours(target).stream()
	    				.filter(x-> !x.equals(nodeA) && !x.equals(nodeB))
	    				.collect(Collectors.toList()).isEmpty();
	    
	    res = res && getOutboundNeighbours(target).stream()
	    				.filter(x-> !x.equals(nodeA) && !x.equals(nodeB))
	    				.collect(Collectors.toList()).isEmpty();
	    
	    LOG.debug("isOnlyBetween: " + res);
	    
		return res;
	}
	
	boolean isBetween(String target,String nodeA, String nodeB) {
	    boolean res=false;
	    Set<String> inbound = getInboundNeighbours(target);
	    Set<String> outbound = getOutboundNeighbours(target);
	 
	    res = res || (inbound.contains(nodeA) && outbound.contains(nodeB));
	    res = res || (inbound.contains(nodeB) && outbound.contains(nodeA));
	    
	    LOG.debug("isBetween: " + res);
	    
	    return res;
	}
	
	boolean isBetween(String nodeA, String nodeB) {
	    boolean res=false;
	 
	    res = res || getInboundNeighbours(nodeA).contains(nodeB);
	    res = res || getOutboundNeighbours(nodeA).contains(nodeB);
	    
	    res = res || getInboundNeighbours(nodeB).contains(nodeA);
	    res = res || getOutboundNeighbours(nodeB).contains(nodeA);
 
	    LOG.debug("isBetween: nodeA=" + nodeA + " nodeB=" + nodeB + " res=" + res);
	    
	    return res;
	}
	
	boolean isLinearPath(String node, int maxLength) {
		boolean res=false;
		
	    Set<String> inbound = getInboundNeighbours(node);
	    Set<String> outbound = getOutboundNeighbours(node);
	    
	    res = inbound.size()==1 && outbound.size()==0;
	    
	    if(!res && maxLength==1) {
	    	res = inbound.size()==1 && outbound.size()==0;
	    } else if(!res && inbound.size()==1 && outbound.size()==1) {
	    	String next=outbound.toArray(new String[0])[0];
		    LOG.trace("isLinearPath: node=" + node + " next=" + next);

	    	res = isLinearPath(next,maxLength-1);
	    }
		return res;
	}
	
//	boolean isCirclePath(String node) {
//		return isCirclePath(node, 0);
//	}
	
	boolean isCirclePath(String node) {
		return isCirclePath(node, 99); // was 3
	}
	
//	boolean isCirclePath(String node, int maxLength) {
//		
//		boolean res=false;
//		Set<String> seen =  new HashSet<>();
//		
//	    LOG.debug("isCirclePath: node=" + node);
//
//		if(getOutboundNeighbours(node).size()+getInboundNeighbours(node).size()==2)
//			res = getOutboundNeighbours(node).stream().anyMatch(n->isCirclePathHelper(n,node,maxLength-1,seen)) ||
//				  getInboundNeighbours(node).stream().anyMatch(n->isCirclePathHelper(n,node,maxLength-1,seen));
//			
//	    LOG.debug("isCirclePath: node=" + node + " res=" + res);
//
//		return res;
//	}
	
//	boolean isCirclePath(String node, int maxLength) {
//		
//	    LOG.trace("isCirclePath: node=" + node);
//
//	    Set<String> neighbours = getNeighbours(node);
//	    
//	    if(neighbours.size()<2) return false;
//	    
//	    Set<String> seen = new HashSet<>();
//	    seen.add(node);
//	    
//	    boolean res = neighbours.stream()
//						.anyMatch(n->{
//						    LOG.trace("isCirclePath: node=" + node + " calling isCirclePathHelper n=" + n);
//							return isCirclePathHelper(n,maxLength-1,neighbours, seen);
//						});
//	    LOG.debug("isCirclePath: node=" + node + " res=" + res);
//
//		return res;
//	}
	
	private Map<String,Boolean> isComputedCirclePath = new HashMap<>();
	
	boolean isCirclePath(String node, int maxLength) {
		
		if(isComputedCirclePath.containsKey(node)) return isComputedCirclePath.get(node);
		
	    LOG.trace("isCirclePath: node=" + node);

	    Set<String> neighbours = getNeighbours(node);
	    
	    if(neighbours.size()<2) return false;
	    
	    Set<String> seen = new HashSet<>();
	    seen.add(node);
	    
	    boolean res = neighbours.stream()
						.anyMatch(n->{
						    LOG.trace("isCirclePath: node=" + node + " calling isCirclePathHelper n=" + n);
							return isCirclePathHelper(n,maxLength-1,neighbours, seen);
						});
	    LOG.debug("isCirclePath: node=" + node + " res=" + res);

	    isComputedCirclePath.put(node,res);
	    
		return res;
	}
	
	boolean isCirclePathHelper(String node, int maxLength, Set<String> target, Set<String> seen) {
		boolean res=false;
		
	    LOG.trace("isCirclePathHelper: node=" + node + " target=" + target + " maxLength=" + maxLength);

		if(maxLength<=0)
			return res;
		
		if(seen.contains(node))
			return res;
			
		Set<String> myTarget = new HashSet<>(target);
		myTarget.remove(node);
		if(myTarget.size()==0) 
			res = true;
		else {
			Set<String> mySeen = new HashSet<>(seen);
			mySeen.add(node);
			Set<String> neighbours = getNeighbours(node);
		    LOG.trace("isCirclePathHelper: node=" + node + " neighbours=" + neighbours );
			if(maxLength>0 && neighbours.size()>=2) { // was "==2"
				res = neighbours.stream()
							.anyMatch(n->{
							    LOG.trace("isCirclePathHelper: node=" + node + " calling isCirclePathHelper n=" + n);
							    return isCirclePathHelper(n,maxLength-1,myTarget,mySeen);
							});
			}	
		}
	    LOG.trace("isCirclePathHelper: node=" + node + " target=" + target + " res=" + res + " maxLength=" + maxLength);
		return res;
	}
	
	
//	boolean isCirclePathHelper(String node, String from, String target, int maxLength, Set<String> seen) {
//		boolean res=false;
//		
//
//	    LOG.debug("isCirclePathHelper: node=" + node + " from=" + from + " target=" + target + " maxLength=" + maxLength + " seen=" + seen);
//
//		if(maxLength<0)
//			return res;
//			
//		if(node.equals(target) && !(seen.size()==0)) 
//			res = true;
//		else {
//			Set<String> neighbours = getNeighbours(node);
//			if(maxLength>0 && neighbours.size()==2) {
//				Set<String> myseen = new HashSet<>(seen);
//				String edge = Arrays.asList(node,from).stream().sorted().collect(Collectors.toList()).toString();
//				myseen.add(edge);
//				res = neighbours.stream()
//							.anyMatch(n->{
//							    String candidate = Arrays.asList(n,node).stream().sorted().collect(Collectors.toList()).toString();
//							    LOG.debug("isCirclePathHelper: edge=*" + edge + "*");
//							    if(myseen.contains(candidate))
//							    	return false;
//							    else 
//							    	return isCirclePathHelper(n,node,target,maxLength-1,myseen);
//							});
//			}	
//		}
//	    LOG.debug("isCirclePathHelper: node=" + node + " from=" + from + " target=" + target + " res=" + res + " maxLength=" + maxLength);
//		return res;
//	}
	
//	boolean isCirclePathHelper(String from, String to, int maxLength, Set<String> seen) {
//		boolean res=false;
//		
//		if(maxLength<=0)
//			return res;
//		
//		if(seen.contains(from))
//			return res;
//		
//		seen.add(from);
//		
//	    LOG.debug("isCirclePathHelper: from=" + from + " to=" + to + " maxLength=" + maxLength);
//
//		if(from.equals(to)) 
//			res = true;
//		else {
//			if(maxLength>0 && getOutboundNeighbours(from).size()+getInboundNeighbours(from).size()==2)
//				res = getOutboundNeighbours(from).stream()
//							.filter(n -> !seen.contains(n))
//							.anyMatch(n->isCirclePathHelper(n,to,maxLength-1,seen)) ||
//					  getInboundNeighbours(from).stream()
//					  		.filter(n -> !seen.contains(n))
//					  		.anyMatch(n->isCirclePathHelper(n,to,maxLength-1,seen));
//				
//		}
//	    LOG.debug("isCirclePathHelper: from=" + from + " to=" + to + " res=" + res + " maxLength=" + maxLength);
//		return res;
//	}
		
	boolean isCirclePath_old(String node, int maxLength) {
		boolean res=false;
		
	    Set<String> inbound = getInboundNeighbours(node);
	    Set<String> outbound = getOutboundNeighbours(node);
	    
	    LOG.trace("isCirclePath: node=" + node + " inbound=" + inbound + " outbound=" + outbound);

	    if(inbound.size()==1 && outbound.size()==1) {
	    	String from = inbound.iterator().next();
	    	String to = outbound.iterator().next();
		    // Set<String> outboundFrom = getOutboundNeighbours(from);
		    Set<String> inboundTo = getInboundNeighbours(to);
		    Set<String> outboundTo = getOutboundNeighbours(to);

		    if(inboundTo.size()==2 && outboundTo.size()==0) {
			    inboundTo.remove(node);
			    inboundTo.remove(from);
			    res = inboundTo.size()==0;
		    }

	    } else if (inbound.size()==2 && outbound.size()==0) {
	    	Iterator<String> iter = inbound.iterator();
	    	String from1 = iter.next();
	    	String from2 = iter.next();

		    Set<String> outboundFrom1 = getOutboundNeighbours(from1);
		    Set<String> outboundFrom2 = getOutboundNeighbours(from2);
		    
		    LOG.trace("isCirclePath: node=" + node + " from1=" + from1 + " outboundFrom1=" + outboundFrom1);
		    LOG.trace("isCirclePath: node=" + node + " from2=" + from2 + " outboundFrom2=" + outboundFrom2);

		    if(outboundFrom1.size()==1) {
			    Set<String> inboundFrom1 = getInboundNeighbours(from1);
			    LOG.trace("isCirclePath: node=" + node + " inboundFrom1=" + inboundFrom1);

			    if(inboundFrom1.size()==1) {
				    String inboundFrom1Node = inboundFrom1.iterator().next();
				    res = inboundFrom1Node.equals(from2);
			    }
			    
		    } else if(outboundFrom2.size()==1) {
			    Set<String> inboundFrom2 = getInboundNeighbours(from2);
			    LOG.trace("isCirclePath: node=" + node + " inboundFrom2=" + inboundFrom2);

			    if(inboundFrom2.size()==1) {
				    String inboundFrom2Node = inboundFrom2.iterator().next();
				    res = inboundFrom2Node.equals(from1);
			    }
		    }
	    };
	    
	    LOG.trace("isCirclePath: node=" + node + " res=" + res);

		return res;
	}
	
	
	String circlePathNeighbour(String node) {
		String res=null;
		
		if(!isCirclePath(node,2)) return res;
		
	    Set<String> inbound = getInboundNeighbours(node);
	    Set<String> outbound = getOutboundNeighbours(node);
	    
	    LOG.trace("circlePathNeighbour: node=" + node + " inbound=" + inbound + " outbound=" + outbound);

	    if(inbound.size()==1 && outbound.size()==1) {
	    	res = outbound.iterator().next();
	    	
	    } else if (inbound.size()==2 && outbound.size()==0) {
	    	
	    	Iterator<String> iter = inbound.iterator();
	    	String from1 = iter.next();
	    	String from2 = iter.next();

		    Set<String> outboundFrom1 = getOutboundNeighbours(from1);
		    Set<String> outboundFrom2 = getOutboundNeighbours(from2);
		    
		    LOG.trace("circlePathNeighbour: node=" + node + " from1=" + from1 + " outboundFrom1=" + outboundFrom1);
		    LOG.trace("circlePathNeighbour: node=" + node + " from2=" + from2 + " outboundFrom2=" + outboundFrom2);

		    if(outboundFrom1.size()==1) {
		    	if(outboundFrom1.iterator().next().equals(node)) res=from1;   
		    } 
		    
		    if(outboundFrom2.size()==1) {
		    	if(outboundFrom2.iterator().next().equals(node)) res=from2;
		    }
		    
	    };
	    
	    LOG.trace("circlePathNeighbour: node=" + node + " res=" + res);

		return res;
	}
	
	Optional<String> getCommonPathEnd(String nodeA, String nodeB) {
		Optional<String> res = Optional.empty();
		
		Set<String> nodeCs = getInboundNeighbours(nodeB);
		nodeCs.addAll(getOutboundNeighbours(nodeB));
		nodeCs.remove(nodeA);
		nodeCs.remove(nodeB);
		
		if(nodeCs.size()!=1) return res;
		
		Optional<String> optC = nodeCs.stream().findFirst();
	
		return optC;
		
	}
	
	Set<String> getIntermediate(String nodeA, String nodeB) {
		Set<String> res = new HashSet<>();
		
		Set<String> nodeCs = getInboundNeighbours(nodeB);
		nodeCs.addAll(getOutboundNeighbours(nodeB));
		nodeCs.remove(nodeA);
		nodeCs.remove(nodeB);
		
		if(nodeCs.size()!=1) return res;
		
		Optional<String> optC = nodeCs.stream().findFirst();
		
		if(!optC.isPresent()) return res;
		
		String nodeC = optC.get();
		
		if(nodeC.equals(nodeA) || nodeC.equals(nodeB)) return res;
		
		Set<String> fromA = getOutboundNeighbours(nodeA);
		Set<String> fromC = getOutboundNeighbours(nodeC);
		
		Set<String> intermediate = Utils.intersection(fromA, fromC);
		
	    LOG.trace("getIntermediate: nodeA=" + nodeA + " nodeC=" + nodeC + " intermediate=" + intermediate);
		
		return intermediate;
	}
	
	boolean hasMultipleIntermediate(String nodeA, String nodeB) {
		return getIntermediate(nodeA, nodeB).size()>1;
	}

	boolean isSimple(List<String> candidates) {
		boolean res=false;
		
		if(candidates.size()>1) return res;
		
		Optional<String> cand = candidates.stream()
									.filter(c -> getInboundNeighbours(c).size()==2 && getOutboundNeighbours(c).size()==0)
									.filter(c -> isCommonLeafNode(c))
									.findFirst();
				
	    LOG.debug("isSimple: cand=" + cand);

		return cand.isPresent();
	}
	
	boolean isCommonLeafNode(String node) {
		boolean res=false;
		
		Set<String> inbounds = getInboundNeighbours(node);
		
		res = inbounds.stream().allMatch(c -> getOutboundNeighbours(c).size()==1 && getOutboundNeighbours(c).contains(node));
		
		return res;
	}
	
	boolean isMultipleBetween(String nodeA, String nodeB) {
		boolean res=false;
		
		Set<String> common = getOutboundNeighbours(nodeA);
		
		Set<String> neighbours = getOutboundNeighbours(nodeB);
		neighbours.addAll(getInboundNeighbours(nodeB));

		common.retainAll(neighbours);

		res = common.size()>1;
		
		common = getOutboundNeighbours(nodeB);
		
		neighbours = getOutboundNeighbours(nodeA);
		neighbours.addAll(getInboundNeighbours(nodeA));

		common.retainAll(neighbours);
		
		res = res || common.size()>1;

		return res;
	}
	
	int singleLaneLength(String node) {
		int res=0;
		
		Set<String> outbound = this.getOutboundNeighbours(node);
		
		if(outbound.size()==0) {
			Set<String> inbound = this.getInboundNeighbours(node);
			@SuppressWarnings("serial")
			Set<String> exclude = new HashSet<String>() {{ add(node); }};
			Optional<Integer>  pathLength = inbound.stream().map(n -> pathLength(n,exclude)).max(Comparator.naturalOrder());
			if(pathLength.isPresent()) res=pathLength.get();
		}
		
		return res;
	}
	
	int pathLength(String node, Set<String> exclude) {
		Optional<Integer> pathLength = Optional.empty();
		
	    LOG.debug("pathLength: node=" + node);

	    if(this.getInboundNeighbours(node).size()==1) {
			exclude.add(node);
			pathLength = this.getOutboundNeighbours(node).stream()
													.filter(n -> !exclude.contains(n))
													.map(n -> pathLength(n,exclude)).max(Comparator.naturalOrder());
			exclude.remove(node);
	    }
		return pathLength.isPresent() ? 1 + pathLength.get() : 1;
	}
	
	int getAdditionalNeighbours(String p, Collection<String> candidates) {
		Set<String> s = getAllNeighbours(p);
		s.removeAll(candidates);
		return s.size();
	}


	public int getInboundEdgesFromPlaced(String node) {
		return getInboundNeighbours(node).stream()
				.filter(n -> isPlaced(n))
				.collect(Collectors.reducing(0, e -> 1, Integer::sum));
	}


	public Set<String> getNeighbours(String node) {
		Set<String> res = getOutboundNeighbours(node);
		res.addAll(getInboundNeighbours(node));
		
		res = res.stream().distinct().collect(Collectors.toSet());
		
		return res;
	}
	
	
	private String processRawType(JSONObject obj, String key) {
		String res="";
		Object o = obj.opt(key);
		
		if(o instanceof JSONArray)
			res = processRawArrayType(obj.optJSONArray(key));
		else if(o instanceof JSONObject)
			res = processRawObjectType(obj.optJSONObject(key));
		else
			res = obj.opt(key).toString();
		return res;
	}


	private String processRawObjectType(JSONObject obj) {
		String res="";
		
		res = "###" + obj.toString();
		if(obj.has("type") && obj.getString("type").equals("array")) {
			res = "test";
		}
		return res;
	}


	private String processRawArrayType(JSONArray array) {
		String res="";
		
		res = array.toString();

		return res;
	}
	
	void addRawType(String node, String value, boolean required) {    
		addNode(node);

		GraphNode graphNode = graphNodes.get(node);
						
		graphNode.otherProperties.add(new RawType(value, required));
	    LOG.trace("addRawType: node=" + node + " value=" + value); 
	
	}
	
	
	boolean isConnectedPath(String from, String to) {
	    LOG.debug("isConnectedPath: from=" + from + " to=" + to);
	    return isConnectedPath(from, to, new ArrayList<>());
	}
	
	boolean isConnectedPath(String from, String to, List<String> exclude) {
		
	    LOG.debug("isConnectedPath: from=" + from + " to=" + to + " exclude="+ exclude);

	    return isConnectedPath(from, to, exclude, new ArrayList<>(), true);

	}
	
	boolean isConnectedPath(String from, String to, List<String> exclude, List<String> seen, boolean first) {
		
	    LOG.debug("isConnectedPath: from=" + from + " to=" + to + " exclude="+ exclude + " seen=" + seen + " first=" + first);

		if(exclude.contains(to) || to.equals(from) || seen.contains(from)) return false;
	    seen.add(from);

	    Optional<String> res = getNeighbours(from).stream()
			    					.filter(n -> !exclude.contains(n))
			    					.filter(n -> (n.equals(to) &&!first) || isConnectedPath(n,to,exclude,seen,false))
			    					.findFirst();
	       
	    LOG.debug("isConnectedPath: res=" + res + " == " + res.isPresent());

	    return res.isPresent();
	    
	}

	
}

