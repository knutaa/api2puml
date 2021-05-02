package no.paneon.swagger2puml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

public class EdgeAnalyzer {
	
    private static Logger LOG = Logger.getLogger(EdgeAnalyzer.class);
	
	String node;
	Graph graph;
	
	Map<Place,List<String>> edgesPlaced;
	Map<Place,List<String>> edgeOptions;

	EdgeAnalyzer(Graph graph, String node) {
		this.graph = graph;
		this.node = node;
		this.edgesPlaced = new HashMap<>();
		this.edgeOptions = new HashMap<>();
	}
	
	interface Condition {
		boolean isCandidate(String toNode, String node, Graph graph);
	}
	
	
	@SuppressWarnings("serial")
	static Map<Place,List<Condition>> edgeConditions = new HashMap<Place,List<Condition>>() {{
		
		put(Place.LEFT, Arrays.asList(
					
					(toNode,node,graph) -> {	
						return !graph.isPlacedAt(node,Place.RIGHT) &&
								graph.getInboundNeighbours(node).size()>=3 &&
								graph.isLeafNode(toNode) && graph.isSingleFromNode(toNode) &&
								(!toNode.startsWith(node) && graph.getEdges(node,toNode).size()==1);
						},
					
					(toNode,node,graph) -> {
						return !graph.isPlacedAt(node,Place.RIGHT) && 
								graph.getOutboundNeighbours(node).size()>=4 &&
								graph.isLinearPath(toNode,2); }
					
//					,(toNode,node,graph) -> {
//						return  graph.isPlacedAt(node,Place.ABOVE) && graph.isPlacedAt(node,Place.BELOW) &&  
//								graph.getOutboundNeighbours(toNode).size()<=1; }
					
					,(toNode,node,graph) -> {
						return !graph.isPlacedAt(node,Place.RIGHT) && 
								graph.getNeighbours(node).size()>=4 &&
							    graph.isSingleFromNode(toNode) &&
								graph.isLeafNode(toNode); }
					
					,(toNode,node,graph) -> {
						return !graph.isPlacedAt(node,Place.RIGHT) && 
								graph.isPlacedAt(node,Place.ABOVE) && 
								graph.isPlacedAt(node,Place.BELOW) && 
								graph.isSingleFromNode(toNode) &&
								graph.isLeafNode(toNode); }
					
					,(toNode,node,graph) -> {
						return !graph.isPlacedAt(node,Place.RIGHT) && 
								graph.isPlacedAt(node,Place.ABOVE) && 
								graph.getNeighbours(node).size()>=3 &&
								graph.isSingleFromNode(toNode) &&
								graph.isLeafNode(toNode); }
					
				));
		
		put(Place.RIGHT, Arrays.asList( 
	        		
				(toNode,node,graph) ->  { 
	        		// put("R02", (toNode) -> {
					if(graph.isPlacedAt(node,Place.RIGHT) && graph.getOutboundNeighbours(node).size()==1) return false;
								 if(graph.positions.currentlyPlacedAtLevel(node,1)==0 && graph.getOutboundNeighbours(node).size()<=3) return false;
					
								 if(graph.isPlaced(toNode)) return false;
			        			 if(graph.isCirclePath(toNode)) return false;

								 if(graph.hasMultipleIntermediate(node,toNode)) return false;
			        			 
								 Set<String> outboundFromNode=graph.getOutboundNeighbours(node);
				                 if(outboundFromNode.size()<2) return false;
			
				                 Set<String> outboundFromToNode=graph.getOutboundNeighbours(toNode);
			
				                 TreeSet<String> common=Utils.intersection(outboundFromToNode,outboundFromNode);
				                 if(common.size()!=1) return false; 
			
				                 Set<String> inboundToCommon=graph.getInboundNeighbours(common.first());
				                 if(inboundToCommon.size()>2) return false;
			
				                 if(outboundFromToNode.size()==1 && outboundFromToNode.contains(node)) return false;
				                 
				                 boolean res = graph.isSingleFromNode(toNode) && common.size()==1 && 
				                               graph.getEdges(node,toNode).size()==1;
				                 
				                 LOG.trace("edgeConditions: R02 toNode=" + toNode + " res=" + res);

				                 return res;
	        				},
	        		
				(toNode,node,graph) ->  { 
	        		// put("R03", (toNode) -> {

								 if(graph.positions.currentlyPlacedAtLevel(node,1)==0 && graph.getOutboundNeighbours(node).size()<=3) return false;

								 if(graph.isPlaced(toNode)) return false;
			        			 if(graph.isCirclePath(toNode)) return false;

								 if(graph.hasMultipleIntermediate(node,toNode)) return false;

								 Set<String> outboundFromNode=graph.getOutboundNeighbours(node);
				                 if(outboundFromNode.size()<3) return false;
			
				                 Set<String> outboundFromToNode=graph.getOutboundNeighbours(toNode);
			
				                 if(outboundFromToNode.size()==1 && outboundFromToNode.contains(node)) return false;
				                    
				                 boolean res = graph.isSingleFromNode(toNode) && graph.isLeafNode(toNode) && 
				                               graph.getEdges(node,toNode).size()==1;
				                 
				                 LOG.trace("edgeConditions: R03 toNode=" + toNode + " res=" + res);

				                 return res;
			        		},
	        		
				(toNode,node,graph) ->  { 
	        		// put("R04", (toNode) -> {
					if(graph.isPlacedAt(node,Place.RIGHT) && graph.getOutboundNeighbours(node).size()==1) return false;

								if(graph.positions.currentlyPlacedAtLevel(node,1)==0 && graph.getOutboundNeighbours(node).size()<=3) return false;

								if(graph.isPlaced(toNode)) return false;
			        			if(graph.isCirclePath(toNode)) return false;

								if(graph.hasMultipleIntermediate(node,toNode)) return false;

				                Set<String> outboundFromToNode=graph.getOutboundNeighbours(toNode);                
				                if(outboundFromToNode.size()==1 && outboundFromToNode.contains(node)) return false;
				                
				                boolean res= graph.isSingleFromNode(toNode) && 
						                	 graph.isLeafNode(toNode) && 
						                     graph.getEdges(node,toNode).size()==1 &&
						                     (graph.isPlacedAt(node,Place.ABOVE) && !graph.isPlacedAt(node,Place.BELOW));
				                
				                LOG.trace("edgeConditions: R04 toNode=" + toNode + " res=" + res);
				                
				                return res;

	        				},
				
				(toNode,node,graph) -> {
					if(graph.isPlacedAt(node,Place.RIGHT) && graph.getOutboundNeighbours(node).size()==1) return false;

					if(graph.positions.currentlyPlacedAtLevel(node,1)==0 && graph.getOutboundNeighbours(node).size()<=3) return false;

					boolean res = !graph.isPlacedAt(node,Place.LEFT) && 
							      graph.getOutboundNeighbours(node).size()>=4 &&
							      graph.isLinearPath(toNode,2);
							
			        LOG.trace("edgeConditions: R05 toNode=" + toNode + " res=" + res);
			        return res;

						},
				
				(toNode,node,graph) -> {
					if(graph.isPlacedAt(node,Place.RIGHT) && graph.getOutboundNeighbours(node).size()==1) return false;

					if(graph.positions.currentlyPlacedAtLevel(node,1)==0 && graph.getOutboundNeighbours(node).size()<=3) return false;

					boolean res = !graph.isPlacedAt(node,Place.LEFT) && 
								   graph.isSingleFromNode(toNode) &&
								   graph.isLeafNode(toNode) &&
								   graph.positions.currentlyPlacedAtLevel(node,1) > graph.positions.currentlyPlacedAtLevel(node,0) + 3;
							
			        LOG.trace("edgeConditions: R06 toNode=" + toNode + " res=" + res);
			        return res;

				},
				
				(toNode,node,graph) -> {
					if(graph.isPlacedAt(node,Place.RIGHT) && graph.getOutboundNeighbours(node).size()==1) return false;

					return  graph.isSingleFromNode(toNode) && 
							graph.getOutboundNeighbours(toNode).size()==1 &&
							graph.isLinearPath(toNode,2) &&
							graph.getNeighbours(node).size()==2;	
				}

				
			));
		
		put(Place.ABOVE, Arrays.asList(
				
				(toNode,node,graph) -> {
					return  graph.isLeafNode(toNode) &&
							graph.isSingleFromNode(toNode) && 
							graph.getEdges(node, toNode).size()<=2;
				},
				
				(toNode,node,graph) -> {
					return  graph.isSingleFromNode(toNode) && 
							graph.getOutboundNeighbours(toNode).size()<=1 &&
							graph.isLinearPath(toNode,2) &&
							graph.getNeighbours(node).size()>2;	
				},
				
				(toNode,node,graph) -> {
					return graph.isCirclePath(toNode,3);	
				}
				
			));
	
		put(Place.BELOW, Arrays.asList(
				
				(toNode,node,graph) -> 	{ 
		            Set<String> neighbours = graph.getOutboundNeighbours(node); // filter(n=> !graph.isPlaced(n));
		            return neighbours.contains(toNode);
		        	}
				
				));
		
		
	}};
		

	void initialize() {
	    for(Place place : Place.coreValues()) {
			if(!edgeOptions.containsKey(place)) edgeOptions.put(place, new ArrayList<>());
			if(!edgesPlaced.containsKey(place)) edgesPlaced.put(place, new ArrayList<>());
	    }
	}
	
	void computeLayout() {
		
	    initialize();

	    Set<String> neighbours = graph.getOutboundNeighbours(node);

        LOG.trace("computeLayout: neighbours=" + neighbours);

	    for(String toNode: neighbours) {

	        // populate edgesPlaced property
	        Map<Place, List<String>> placed = graph.getPlaced(node);
	        
	        for(Place v : Place.coreValues() )  {           
	            if(placed.containsKey(v)) {
	            	for( String n : placed.get(v) ) {
	                    if(!edgesPlaced.containsKey(v)) {
	                    	edgesPlaced.put(v, new ArrayList<>());
	                    };
	                    if(!edgesPlaced.get(v).contains(n)) edgesPlaced.get(v).add(n);
	                }
	            }
	        };

	        for(Place place : Place.coreValues()) {
	        	List<Condition> conditions = edgeConditions.get(place);
	        	for(Condition condition: conditions) {
	        		if(condition.isCandidate(toNode, node, graph) && !edgeOptions.get(place).contains(toNode)) {
	        	        LOG.trace("computeLayout: qualified direction=" + place + " toNode=" + toNode);
	        			edgeOptions.get(place).add(toNode);
	        		}
	        	}
	        	edgeConditions.get(place).forEach(condition -> {
	        		if(condition.isCandidate(toNode, node, graph) && !edgeOptions.get(place).contains(toNode)) {
	        	        LOG.trace("computeLayout: qualified direction=" + place + " toNode=" + toNode);
	        			edgeOptions.get(place).add(toNode);
	        		}
	        	});
	        	
	        }
	        
	    }
		
	}
	
	boolean isUnbalanced() {
		return graph.positions.isUnbalancedLevel(this.node);		
	}
	
	boolean isChallengeToPlaceBelow() {
		boolean res=true;
        Place[] directions = {Place.RIGHT, Place.LEFT};
        String nodeBelow = graph.getNearestBelow(this.node);
        
        if(nodeBelow==null) return false;
        
        long atBelowLevel = graph.positions.currentlyPlacedAtLevel(node,1);
        if(atBelowLevel < 5) return false;
        
        Set<String> inboundToNodeBelow = graph.getInboundNeighbours(nodeBelow);
        for( Place direction : directions ) {
        	Set<String> placedInDirection = Utils.intersection(graph.getPlacedAt(this.node,Place.mapping.get(direction)),inboundToNodeBelow);
        	placedInDirection.remove(node);

        	boolean placeToDirection = placedInDirection.isEmpty();
            if(!placeToDirection) {
            	res = false;
            } else {
                // check placement on the left side, find the ones that have been placed to right of others
                List<String> pivot = graph.positions.getAllRightLeftOf(graph, nodeBelow, direction);

                Optional<String> directConnection = pivot.stream().filter(n -> graph.hasDirectConnection(nodeBelow,n)).findFirst();
                res = res && directConnection.isPresent();
            } 
        };
        
        LOG.debug("isChallengeToPlaceBelow:: node=" + node + " res=" + res);
        return res;
	}
	
	interface PlaceCounter {
		double get(List<String> options, List<String> placed);
	}
	
	@SuppressWarnings("serial")
	static Map<Place,PlaceCounter> placeCounter = new HashMap<Place,PlaceCounter>() {{
		
		put(Place.LEFT,  (options,place) -> {return 1;} );
		
		put(Place.ABOVE, (options,place) -> {
					        Set<String> realOptions = Utils.difference(options,place);
					        float res = realOptions.size()/2;
					        if(realOptions.size() <= options.size()/2) res=0;
					        return res;
					    });
		
		put(Place.RIGHT,  (options,place) -> {return 1;} );
		
		put(Place.BELOW,  (options,place) -> {return options.size();} );
		
	}};
	
	List<String> getEdgesForPosition(Place direction) {
		List<String> res = new ArrayList<>();
		
		computeLayout();
		
		if(direction==Place.LEFT) {
			if(!(graph.getNeighbours(node).size()>4) && graph.positions.currentlyPlacedAtLevel(node,1)< graph.positions.currentlyPlacedAtLevel(node)) 
				return res;
		}
		
		List<String> options = edgeOptions.get(direction).stream()
									.distinct()
									.filter(n -> !graph.isPlaced(n))
									.collect(Collectors.toList());
		List<String> placed =  edgesPlaced.get(direction);

		LOG.debug("getEdgesForPosition: node=" + node + " direction=" + direction + " options=" + options);
		
		double length = placeCounter.get(direction).get(options,placed);
		length = Math.ceil(length);

		if(direction==Place.ABOVE) {
			
			boolean challengeBelow = isChallengeToPlaceBelow();
			boolean unbalanced = isUnbalanced();
			
			LOG.debug("getEdgesForPosition: node=" + node + " challengeBelow=" + challengeBelow + " unbalanced=" + unbalanced);

			// adjust for how many are already placed below or are candidates for below

			Set<String> allOutbound = graph.getOutboundNeighbours(node);
			if( !challengeBelow && allOutbound.size()<=3) return res;

			List<String> placedLeft = graph.getPlacedAt(node,Place.RIGHT);
			List<String> placedRight = graph.getPlacedAt(node, Place.LEFT);
			Set<String> placedLeftRight = Utils.union(placedLeft,placedRight);

			List<String> placedAbove = graph.getPlacedAt(node, Place.BELOW);
			List<String> placedBelow = graph.getPlacedAt(node, Place.ABOVE);
			Set<String> placedAboveBelow = Utils.union(placedAbove,placedBelow);

			Set<String> alreadyPlaced = Utils.union(placedLeftRight,placedAboveBelow);

			Set<String> remainingOptionsAbove = Utils.difference(edgeOptions.get(Place.ABOVE),alreadyPlaced);
			Set<String> remainingOptionsBelow = Utils.difference(edgeOptions.get(Place.BELOW),alreadyPlaced);
			Set<String> remainingOptionsAboveBelow = Utils.union(remainingOptionsAbove, remainingOptionsBelow);

			Set<String> allOptionsAboveAndBelow = Utils.union(edgeOptions.get(Place.ABOVE), edgeOptions.get(Place.BELOW));

			allOptionsAboveAndBelow = Utils.difference(allOptionsAboveAndBelow,placedLeftRight);

			options = Utils.difference(edgeOptions.get(Place.ABOVE),alreadyPlaced).stream().distinct().collect(Collectors.toList());

			if(!challengeBelow && !unbalanced) {
	
				int max = remainingOptionsAboveBelow.size();

				int diff = Math.abs(placedAbove.size() - placedBelow.size());
				if(placedAbove.size() < placedBelow.size()) {
					// diff + half of rest above
					length = diff + (max - diff) / 2.0;
				} else {
					// half of rest above
					length = (max - diff) / 2.0;
				}
				LOG.debug("getEdgesForPosition: node=" + node + " max=" + max);
				LOG.debug("getEdgesForPosition: node=" + node + " diff=" + diff);
				LOG.debug("getEdgesForPosition: node=" + node + " placedAbove=" + placedAbove);
				LOG.debug("getEdgesForPosition: node=" + node + " placedBelow=" + placedBelow);

				// options = Utils.difference(edgeOptions.get(Place.ABOVE),alreadyPlaced).stream().distinct().collect(Collectors.toList());
				
			} else if(challengeBelow) {
				// challenge to place below
				float maxAbove = allOptionsAboveAndBelow.size(); 
				
				length = Math.min(Math.max(maxAbove - placedAbove.size(), 0), remainingOptionsAbove.size());
	
				// options = Utils.difference(edgeOptions.get(Place.ABOVE),alreadyPlaced).stream().distinct().collect(Collectors.toList());
				
			} else if(unbalanced) {
				// challenge to place below
				long unbalance = graph.positions.currentlyPlacedAtLevel(node,1)-graph.positions.currentlyPlacedAtLevel(node,-1);
				
				int max = remainingOptionsAboveBelow.size();

				long diff = Math.max(
								Math.abs(placedAbove.size() - unbalance),
								Math.abs(placedAbove.size() - placedBelow.size()));

				if(placedAbove.size() < placedBelow.size()) {
					// diff + half of rest above
					length = diff + (max - diff) / 2;
				} else {
					// half of rest above
					length = (max - diff) / 2;
				}
				LOG.debug("getEdgesForPosition: unbalanced node=" + node + " max=" + max);
				LOG.debug("getEdgesForPosition: unbalanced node=" + node + " diff=" + diff);
				LOG.debug("getEdgesForPosition: unbalanced node=" + node + " placedAbove=" + placedAbove);
				LOG.debug("getEdgesForPosition: unbalanced node=" + node + " placedBelow=" + placedBelow);
				LOG.debug("getEdgesForPosition: unbalanced node=" + node + " length=" + length);

				
				// options = Utils.difference(edgeOptions.get(Place.ABOVE),alreadyPlaced).stream().distinct().collect(Collectors.toList());	
			}
			
			
		}
		
		int iLength = (int) length;
		iLength = Math.min(iLength, options.size());
		
		if(iLength<0) iLength=0;
		
		LOG.debug("getEdgesForPosition: options=" + options + " length=" + iLength);
		res = options.stream().distinct()
				.sorted(Comparator.comparing(nodeA -> getNodeComplexity(graph,node,nodeA,direction)))
				.collect(Collectors.toList());
		
		LOG.debug("getEdgesForPosition: SORTED options=" + res + " length=" + iLength);

		res = res.subList(0, iLength);
		
		LOG.debug("getEdgesForPosition: before circlePath correction: res=" + res);

		// correct for circlePath elements missing in res
		List<String> circleNodes = new LinkedList<>();
		// res.stream().filter(n -> graph.isCirclePath(n,0)).map(n -> circleNodes.add(n)).count();
		
		res.forEach(n -> {
			if(graph.isCirclePath(n)) circleNodes.add(n);
		});
		
		LOG.debug("getEdgesForPosition: circleNodes=" + circleNodes);
		circleNodes.removeAll(res);
		res.addAll(circleNodes);
		
		LOG.debug("getEdgesForPosition: res=" + res);

		return res;
	}
	
	int getNodeComplexity(Graph graph, String node, String nodeA, Place direction) {
		int res=0;
		
	    int inbound=graph.getEdges(node,nodeA).size();
	    
	    int outbound=graph.getOutboundNeighbours(nodeA).size();
	    
	    boolean circlePath=graph.isCirclePath(nodeA);

	    int max = graph.nodes.stream()
					.filter(n -> graph.getEdges(node, n).size()>0)
					.map(n -> graph.getEdges(node, n).stream().map(e -> e.relation.length()).mapToInt(x -> x).sum())
					.max(Comparator.naturalOrder())
					.orElse(200);
	    
	    int relationLength = graph.getEdges(node, nodeA).stream().map(e -> e.relation.length()).mapToInt(x -> x).sum();
	    
	    switch(direction) {
	    case LEFT:
			res = 10000 * (circlePath?1:0) + 1000 * outbound + 100 * inbound + relationLength - max;
			break;
			
	    	default:
	    	res = -10000 * (circlePath?1:0) + 1000 * outbound + 100 * inbound + relationLength;
	    };
		LOG.trace("getNodeComplexity: node=" + node + " nodeA=" + nodeA + " res="+ res);

		return res;
	}
	
}
