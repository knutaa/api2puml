package no.paneon.swagger2puml;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import no.paneon.puml.ClassEntity;
import no.paneon.puml.ClassProperty;
import no.paneon.puml.Comment;
import no.paneon.puml.Diagram;
import no.paneon.puml.EnumEntity;
import no.paneon.puml.HiddenEdge;
import no.paneon.swagger2puml.Graph.GraphNode;


public class Layout {

	private static Logger LOG = Logger.getLogger(Layout.class);

	JSONObject swagger;
	Graph graph;
	JSONObject layoutConfig;
	List<String> processed;

	Layout(JSONObject swagger, Graph graph, JSONObject layoutConfig) {
		this.swagger = swagger;
		this.graph = graph;
		this.layoutConfig = layoutConfig;
		this.processed = new ArrayList<>();
	}


	ClassEntity generateUMLClasses(Diagram diagram, String node, String resource) {
		if(processed.contains(node)) return null;

		String stereoType = Utils.getStereoType(graph, node,resource);

		ClassEntity cls = new ClassEntity(node, stereoType);

		GraphNode gnode = graph.graphNodes.get(node);

		LOG.debug("generateUMLClasses: node=" + node + " graphNode=" + gnode);

		// graph.graphNodes.get(node).properties.keySet().forEach(prop -> {
		gnode.properties.forEach(prop -> {
			LOG.debug("generateUMLClasses: node=" + node + " prop=" + prop);
			
			// System.out.println("generateUMLClasses: node=" + node + " prop=" + prop.getName() + " card=" + prop.getCardinality());

			String type =  prop.getType();
			if(Utils.isEnumType(swagger, type) && presentEnumForNode(resource,node)) {
				generateForEnum(cls, node, type);
				LOG.debug("generateUMLClasses: generate enum type=" + type);
			}
			cls.addProperty(new ClassProperty(prop.getName(),prop.getType(), prop.getCardinality(), prop.isRequired()));
		});	  
		
		gnode.otherProperties.forEach(prop -> {
			LOG.debug("generateUMLClasses: node=" + node + " otherProperty=" + prop);
			cls.addProperty(new ClassProperty(prop.getName(),prop.getValue(), prop.isRequired()));
		});	 

		cls.addDescription(gnode.description);

		LOG.trace("generateUMLClasses: finished node=" + node);

		diagram.addClass(cls);

		return cls;
	}


	private boolean presentEnumForNode(String resource, String node) {
		return !Utils.isBaseType(resource, node);
	}


	EnumEntity generateForEnum(ClassEntity cls, String resource, String type) {
		// console.log("generateForEnum: type=" + type);
		if(!Utils.isEnumType(swagger,type)) return null;

		LOG.debug("generateForEnum: type=" + type + " all enums: " + graph.enumNodes.keySet());

		boolean isProcessed = cls.isEnumProcessed(type);
		LOG.debug("generateForEnum: type=" + type + " isProcessed: " + isProcessed);

		if(isProcessed) return null;

		// diagram = "class " + type + " <<Enumeration>> {" + "\n";
		EnumEntity enode = new EnumEntity(type);
		cls.addEnum(enode);

		EnumNode e = this.graph.enumNodes.get(type);

		LOG.debug("generateForEnum: type=" + type + " enode=" + e);

		if(e!=null)
			e.values.forEach(v -> {
				LOG.debug("generateForEnum: type=" + type + "v=" + v.toString());
				enode.addValue(v.toString());
			});

		return enode;
	}


	boolean generateUMLEdges(Diagram diagram, String node, String resource, List<String> includeNodes, List<String> processed) {

		ClassEntity cls = diagram.getResource(node);

		if(cls==null) return false;

		List<String> neighbours = graph.getOutboundNeighbours(node).stream().sorted().collect(Collectors.toList());

		// if(neighbours.size()==0) return false;

		int edgeCount = cls.getEdgeCount();

		cls.addComment(new Comment("'processing edges for " + node));;

		LOG.debug("### ");
		LOG.debug("### generateUMLEdges for " + node);
		LOG.debug("### ");

		//
		// first process based on configuration details (manual override)
		//

		if(layoutConfig.has(node)) {
			Place directions[] = Place.values();

			for( Place direction: directions) {
				if(layoutConfig.getJSONObject(node).has(direction.label)) {
					JSONArray config = layoutConfig.getJSONObject(node).getJSONArray(direction.label);
					List<String> placeNodes = Utils.JSONArrayToList(config); 
					String rule = "Configuration override: " + direction;
					LOG.trace("manual layout of node=" + node + " placeNodes=" + placeNodes);
					graph.placeEdgesToNeighbours(cls, node, placeNodes, rule, direction, processed);
				}
			}
		}

		//
		// special case of recursive?
		//
		boolean recursive=false;
		String rule = "Recursive (self-reference)";

		if(graph.getOutboundNeighbours(node).contains(node)) {
			List<String> placeNodes = Arrays.asList(node);
			recursive = graph.placeEdgesToNeighbours(cls, node, placeNodes, rule, Place.RIGHT, processed);      	
		}

		LOG.debug("generateUMLEdges: node=" + node + " generated recursive=" + recursive);
		recursive = graph.getOutboundNeighbours(node).contains(node);
		LOG.debug("generateUMLEdges: node=" + node + " active recursive=" + recursive);

		//
		// special case of 'Item' sub-resource - this we try to place to the right
		//
		rule = "Item special case";

		Optional<String> subResource = graph.getOutboundNeighbours(node).stream()
				.filter(toNode -> !graph.isPlaced(toNode) && 
						graph.isSingleFromNode(toNode) &&
						(toNode.endsWith(node + "Item") || toNode.endsWith(node + "Items"))) // earlier without node as prefix
				.findFirst();

		if(subResource.isPresent()) {
			List<String> placeNodes = Arrays.asList(subResource.get());
			// diagram = diagram + graph.placeEdgesToNeighbours(node, placeNodes, rule, Place.RIGHT, processed);
			graph.placeEdgesToNeighbours(cls, node, placeNodes, rule, Place.RIGHT, processed);

		}

		LOG.debug("generateUMLEdges: node=" + node + " subResource=" + subResource.isPresent());

		//
		// before placing any enums, first handle inbound edges from already placed nodes
		//
		// layoutInboundFromPlacedNodes(node,cls,neighbours,includeNodes,processed);

//		// 
//		//
//		// place any enums
//		//
//
//		LOG.debug("generateUMLEdges: node=" + node + " place any enums (not already placed)");
//
//		boolean d = graph.layoutEnums(diagram, cls, node, recursive);
//		if(d)
//			LOG.debug("generateUMLEdges: node=" + node + " have placed enums");

		EdgeAnalyzer edgeAnalyzer = new EdgeAnalyzer(graph,node);
		edgeAnalyzer.computeLayout();

		//
		// layout in multiple steps / phases:
		//
		// 1) process neighbors with inbound edges from multiple already placed nodes
		// 2) process neighbors outbound edges to already placed nodes
		// 3) placed edges with already placed nodes
		// 4) left
		// 5) right
		// 6) above 
		// 7) check for unbalance above / below
		// 8) below (looking left and right)
		// 9) below any remaining
		//

		layoutBetweenAlreadyPlacedNodes(node,cls,neighbours,includeNodes, processed);

		layoutOutboundToPlacedNodes(node,cls,neighbours,includeNodes,processed);

		layoutOutboundEdgesWithPlacedNodes(node,cls,includeNodes,processed);

		layoutBetweenCommonNode(node,cls,neighbours,includeNodes, processed);

		
		// 
		//
		// place any enums
		//

		LOG.debug("generateUMLEdges: node=" + node + " place any enums (not already placed)");

		boolean d = graph.layoutEnums(diagram, cls, node, recursive);
		if(d)
			LOG.debug("generateUMLEdges: node=" + node + " have placed enums");
		
		//
		
		layoutLeft(node,cls,includeNodes,processed, edgeAnalyzer);    

		layoutRight(node,cls,includeNodes,processed,edgeAnalyzer, recursive);

		layoutAbove(node,cls,includeNodes,processed, edgeAnalyzer);

		layoutUnbalancedAboveBelow(node,cls,includeNodes,processed, edgeAnalyzer);

		layoutBelowLeftRight(node,cls,includeNodes,processed, edgeAnalyzer);

		layoutBelowRemaining(node,cls,includeNodes,processed);

		cls.addComment(new Comment("'completed processing of edges for " + node));

		LOG.debug("generateUMLEdges node=" + node + " completed");

		return cls.getEdgeCount()>edgeCount;

	}


	private void layoutBetweenCommonNode(String node, ClassEntity cls, List<String> neighbours, List<String> includeNodes, List<String> processed) {

		List<String> neighboursWithCircle = neighbours.stream()
				.filter(n -> graph.getAllNeighbours(n).size()==2)
				.filter(n -> graph.isCirclePath(n,99))
				.collect(Collectors.toList());

		LOG.debug("layoutBetweenCommonNode: node=" + node + " neighbours with circles: " + neighboursWithCircle);

		// List<String> seen = new LinkedList<>(); 
		for(String n : neighboursWithCircle) {
			// seen.add(n);
			List<String> myCandidates = new LinkedList<>(neighboursWithCircle);
			// myCandidates.removeAll(seen);
			LOG.debug("layoutBetweenCommonNode: node=" + node + " myCandidates=" + myCandidates);

			boolean hasPath = myCandidates.stream().anyMatch(cand -> graph.isConnectedPath(n, cand));
			
			LOG.debug("layoutBetweenCommonNode: node=" + node + "  n=" + n + " hasPath=" + hasPath);

			if(hasPath) {
				// place n below node
				Place func = Place.BELOW;
				String rule = "Common node - direction: " + func;

				LOG.debug("layoutBetweenCommonNode: node=" + node + " n=" + n);
		        graph.placeEdges(cls, node, n, func, rule, processed);

				continue;
			}
			
		}
			
		LOG.debug("layoutBetweenCommonNode: node=" + node + " done");
	}
	
	private void layoutInboundFromPlacedNodes(String node, ClassEntity cls, List<String> neighbours,
			List<String> includeNodes, List<String> processed) {
		
		List<String> inboundFromPlaced = graph.getInboundNeighbours(node).stream()
				.filter(n->graph.isPlaced(n))
				.collect(Collectors.toList());
			
		LOG.debug("layoutInboundFromPlacedNodes:: inboundFromPlaced=" + inboundFromPlaced);

		String rule = "Place edges inbound from already placed nodes";

		for(String toNode:inboundFromPlaced) {
			if(graph.positions.isPositionedToLeft(toNode, node)) 
				graph.placeEdges(cls, toNode, node, Place.RIGHT, rule, processed);
			else if(graph.positions.isPositionedToRight(toNode, node)) 
				graph.placeEdges(cls, toNode, node, Place.LEFT, rule, processed);	
			else if(graph.positions.isPositionedAbove(toNode, node)) 
				graph.placeEdges(cls, toNode, node, Place.BELOW, rule, processed);
			else if(graph.positions.isPositionedBelow(toNode, node)) 
				graph.placeEdges(cls, toNode, node, Place.ABOVE, rule, processed);
		}
		
		LOG.debug("layoutInboundFromPlacedNodes:: done=");

	}


	private void layoutBelowRemaining(String node, ClassEntity cls, List<String> includeNodes,
			List<String> processed) {

		String rule = "General below rule - either none already or unable to place left / right of currently placed";

		LOG.debug("layoutBelowRemaining:: processing rule: " + rule);

		Set<String> candidatesBelow = graph.getOutboundNeighbours(node);

		LOG.debug("layoutBelowRemaining:: remaining below - candidates for below: " + candidatesBelow);
		LOG.debug("layoutBelowRemaining:: includeNodes: " + includeNodes);

		List<String> candidatesList = candidatesBelow.stream()
				.sorted(Comparator.comparing(n -> graph.getOutboundNeighbours(n).size()))
				.filter(n->includeNodes.contains(n))
				.collect(Collectors.toList());

		LOG.debug("layoutBelowRemaining:: remaining below - sorted candidates for below: " + candidatesList);

		graph.placeEdgesToNeighboursBelow(cls, node, candidatesList, rule, processed);		
	}


	private void layoutBelowLeftRight(String node, ClassEntity cls, List<String> includeNodes, List<String> processed,
			EdgeAnalyzer edgeAnalyzer) {

		LOG.debug("layoutBelowLeftRight: node=" + node + " check for below - left / right");

		String nodeBelow = graph.getNearestBelow(node);
		Set<String> inboundToNodeBelow = graph.getInboundNeighbours(nodeBelow);

		List<String> nodesBelow = inboundToNodeBelow.stream()
				.map(n -> graph.getOutboundNeighbours(n))
				.flatMap(Set::stream)
				.filter(n -> graph.isPlaced(n) && graph.isPlacedAt(n,Place.BELOW))
				.filter(n -> !n.equals(node))
				.distinct()
				.collect(Collectors.toList());

		Optional<String> someNodePlacedRightOrLeft = nodesBelow.stream()
				.filter(n -> graph.isPlacedAt(n,Place.LEFT) || graph.isPlacedAt(n,Place.RIGHT) )
				.findFirst();

		boolean someEdgeFromRight = nodesBelow.stream()
				.map(n -> graph.getInboundNeighbours(n))
				.flatMap(Set::stream)
				.map(n -> graph.positions.get(n))
				.anyMatch(n -> n.x >= graph.positions.get(node).x);

		boolean someEdgeFromLeft = nodesBelow.stream()
				.map(n -> graph.getInboundNeighbours(n))
				.flatMap(Set::stream)
				.map(n -> graph.positions.get(n))
				.anyMatch(n -> n.x < graph.positions.get(node).x);

		LOG.debug("layoutBelowLeftRight:: place below: nodesBelow=" + nodesBelow);
		LOG.debug("layoutBelowLeftRight:: place below: someNodePlacedRightOrLeft=" + someNodePlacedRightOrLeft);


		// Place[] directions = {Place.LEFT, Place.RIGHT};

		List<Place> directions = new LinkedList<>();
		if(someEdgeFromRight && !someEdgeFromLeft) {
			directions.add(Place.LEFT);
		} else {
			if(!someEdgeFromRight && someEdgeFromLeft) {
				directions.add(Place.RIGHT);
			} else {
				directions.add(Place.RIGHT);
				directions.add(Place.LEFT);
			}
		}

		if(someNodePlacedRightOrLeft.isPresent()) {
			for( Place direction : directions ) {
				Set<String> placedInDirection = Utils.intersection(graph.getPlacedAt(node,Place.mapping.get(direction)),inboundToNodeBelow);
				placedInDirection.remove(node);

				String rule = "General below rule - direction to " + direction;

				LOG.debug("layoutBelowLeftRight:: processing rule: " + rule + " placedInDirection: " + placedInDirection);

				boolean placeToDirection = nodeBelow!=null && placedInDirection.isEmpty();
				if(placeToDirection) {
					// check placement on the left side, find the ones that have been placed to right of others
					List<String> pivot = graph.positions.getAllRightLeftOf(graph, nodeBelow, direction);

					Optional<String> directConnection = pivot.stream().filter(n -> graph.hasDirectConnection(nodeBelow,n)).findFirst();
					if(!directConnection.isPresent()) {
						pivot.add(nodeBelow);
						String pivotNode = graph.getEdgeBoundary(pivot,direction);

						List<String> candidates = edgeAnalyzer.getEdgesForPosition(Place.BELOW);
						if(!candidates.isEmpty()) {
							LOG.debug("layoutBelowLeftRight:: pivotNode=" + pivotNode + " candidates for below: " + candidates);

							candidates = candidates.stream()
									.sorted(Comparator.comparing(n -> graph.getOutboundNeighbours(n).size()))
									.filter(n->includeNodes.contains(n))
									.collect(Collectors.toList());

							LOG.debug("layoutBelowLeftRight:: pivotNode=" + pivotNode + " sorted candidates for below: " + candidates);
							LOG.debug("layoutBelowLeftRight:: placing below to " + direction + " all remaining below:: " + candidates);

							graph.placeEdgesToNeighboursBelow(cls, node, candidates, rule, processed, pivotNode, direction);

						}
					}
				}
			}
		}		
	}


	private void layoutUnbalancedAboveBelow(String node, ClassEntity cls, List<String> includeNodes,
			List<String> processed, EdgeAnalyzer edgeAnalyzer) {

		List<String> neighbours = edgeAnalyzer.getEdgesForPosition(Place.ABOVE).stream().collect(Collectors.toList());
		
		boolean remainingAboveCandidates = neighbours.stream().anyMatch(n -> !graph.isPlaced(n));
					
		LOG.debug("layoutUnbalancedAboveBelow: check for unbalance above / below - remainingAboveCandidates=" + remainingAboveCandidates);

		if(remainingAboveCandidates) return;
		if(graph.getNeighbours(node).size()<4) return;
		
		int unbalance = graph.positions.currentlyPlacedAtLevel(node,1)-graph.positions.currentlyPlacedAtLevel(node,-1);
		if(unbalance>=0) {
			neighbours = edgeAnalyzer.getEdgesForPosition(Place.BELOW).stream().collect(Collectors.toList());
			if(neighbours.size()>0) {
				int subset = Math.min(neighbours.size(),unbalance);
				List<String> candidates = neighbours.subList(0, subset).stream()
						.filter(n->includeNodes.contains(n))
						.collect(Collectors.toList());
				String rule = "Unbalance below / above";
				Place func = Place.ABOVE;

				LOG.debug("layoutUnbalancedAboveBelow: unbalanced above/below placeAbove: node=" + node + " candidates=" + candidates);
				graph.placeEdgesToNeighbours(cls, node, candidates, rule, func, processed);
			}
		}		
	}


	private void layoutAbove(String node, ClassEntity cls, List<String> includeNodes, List<String> processed, EdgeAnalyzer edgeAnalyzer) {
		
		String nodeAbove = graph.getNearestAbove(node);
		Set<String> inboundToNodeAbove = graph.getInboundNeighbours(nodeAbove);

		LOG.debug("layoutAbove: check for above: node=" + node + " nodeAbove=" + nodeAbove);
		LOG.debug("layoutAbove: check for above: inboundToNodeAbove=" + inboundToNodeAbove);

		boolean directConnectionAbove=false;
		for(String n:inboundToNodeAbove) {
			for(String m:inboundToNodeAbove) {
				if(!n.equals(node) && !m.equals(node) && !n.equals(m) && graph.hasDirectConnection(n,m)) directConnectionAbove=true;
			}
		}

		LOG.debug("layoutAbove: check for above: directConnectionAbove=" + directConnectionAbove);

		boolean blockingEdges=false;
		if(nodeAbove!=null) {
			Set<String> outboundFromNodeAbove = graph.getOutboundNeighbours(nodeAbove);
			Set<String> edgesWithNode = graph.getInboundNeighbours(node);
			edgesWithNode.addAll(graph.getOutboundNeighbours(node));
			Set<String> common = Utils.intersection(outboundFromNodeAbove, edgesWithNode);
			common.remove(node);
			common.remove(nodeAbove);
			boolean circlePath = common.stream().allMatch(n -> graph.isCirclePath(n));
			blockingEdges = !common.isEmpty() && !circlePath;
		
			LOG.debug("layoutAbove: check for above: outboundFromNodeAbove=" + outboundFromNodeAbove);
			LOG.debug("layoutAbove: check for above: edgesWithNode=" + edgesWithNode);
			LOG.debug("layoutAbove: check for above: common=" + common);
			LOG.debug("layoutAbove: check for above: blockingEdges=" + blockingEdges);
		}
		
		Place[] directions = {Place.RIGHT, Place.LEFT};

		if(!directConnectionAbove && !blockingEdges)
			for( Place direction : directions ) {
				edgeAnalyzer.computeLayout(); 
				Set<String> placedInDirectionFromNode = new HashSet<>(graph.getPlacedAt(node, Place.mapping.get(direction)));   
				Set<String> nodes = Utils.intersection(placedInDirectionFromNode,inboundToNodeAbove);
				nodes.remove(node);

				boolean placeToDirection = nodes.isEmpty();
				if(placeToDirection) {
					// check placement on the each side, find the ones that have been placed in direction of others
					List<String> pivot = graph.positions.getAllRightLeftOf(graph, nodeAbove, direction);

					LOG.debug("layoutAbove: check for above direction=" + direction + " pivot=" + pivot);

					Optional<String> directConnection = pivot.stream().filter(n -> graph.hasDirectConnection(nodeAbove,n)).findFirst();
					if(!directConnection.isPresent()) {
						LOG.debug("layoutAbove: check for above - no direction connection found with nodeAbove=" + nodeAbove);
						String pivotNode = graph.getEdgeBoundary(pivot,direction);
						LOG.debug("layoutAbove: check for above - using pivotNode=" + pivotNode);

						List<String> candidates = edgeAnalyzer.getEdgesForPosition(Place.ABOVE).stream()
								.filter(n->includeNodes.contains(n))
								.sorted(Comparator.comparing(n->100-graph.getOutboundNeighbours(n).size()))
								.collect(Collectors.toList());

						LOG.debug("layoutAbove: check for above - candidates=" + candidates);

						if(!candidates.isEmpty()) {
							String rule = "General above rule - direction: " + direction;
							Place func = Place.ABOVE;

							LOG.debug("layoutAbove: placeAbove: node=" + node + " candidates=" + candidates + " pivotNode=" + pivotNode);
							graph.placeEdgesToNeighbours(cls, node, candidates, rule, func, processed, pivotNode, direction);

						}
					}
				}
			}	
	}


	private void layoutRight(String node, ClassEntity cls, List<String> includeNodes, List<String> processed, EdgeAnalyzer edgeAnalyzer, boolean recursive) {

		LOG.debug("layoutRight: check for right node=" + node + 
				" placeAt=" + graph.getPlacedAt(node,Place.LEFT) + " " + graph.getPlacedAt(node,Place.RIGHT));

		if(!recursive && !graph.isPlacedAt(node,Place.LEFT)) {

			List<String> candidates = edgeAnalyzer.getEdgesForPosition(Place.RIGHT).stream()
					.filter(n->includeNodes.contains(n))
					.collect(Collectors.toList());

			LOG.debug("layoutRight: check for right - candidates=" + candidates);

			if(!candidates.isEmpty()) {
				candidates = candidates.subList(0, 1);
				String rule = "General right rule";
				Place func = Place.RIGHT;

				LOG.debug("layoutRight: placeRIGHT: node=" + node + " candidates=" + candidates);

				LOG.debug("layoutRight: placeRIGHT: node=" + node + " isPlaced RIGHT =" + graph.getPlacedAt(node,Place.RIGHT));
				LOG.debug("layoutRight: placeRIGHT: node=" + node + " isPlaced LEFT =" + graph.getPlacedAt(node,Place.LEFT));

				graph.placeEdgesToNeighbours(cls, node, candidates, rule, func, processed);

			}

		};		
	}


	private void layoutLeft(String node, ClassEntity cls, List<String> includeNodes, List<String> processed, EdgeAnalyzer edgeAnalyzer) {

		LOG.debug("layoutLeft: check for left: node=" + node);

		List<String> candidates = edgeAnalyzer.getEdgesForPosition(Place.LEFT).stream()
				.filter(n->includeNodes.contains(n))
				.collect(Collectors.toList());

		LOG.debug("layoutLeft: check for left - candidates=" + candidates);

		if(!candidates.isEmpty() && !graph.positions.isPlaced(node,Place.LEFT)) {
			candidates = candidates.subList(0, 1);
			String rule = "General left rule";
			Place func = Place.LEFT;

			LOG.debug("layoutLeft: place LEFT: node=" + node + " candidates=" + candidates);
			graph.placeEdgesToNeighbours(cls, node, candidates, rule, func, processed);
		}		
	}


	private void layoutOutboundEdgesWithPlacedNodes(String node, ClassEntity cls, List<String> includeNodes, List<String> processed) {

		LOG.debug("layoutOutboundEdgesWithPlacedNodes: check for edges with already placed node=" + node);

		Set<String> placed = graph.getPlacedNodes();

		List<String>candidates = graph.getOutboundNeighbours(node).stream()
				.filter(n -> !n.equals(node))
				.filter(n -> placed.contains(n))
				.collect(Collectors.toList());

		LOG.debug("layoutOutboundEdgesWithPlacedNodes: check for edges with already placed node=" + node + " candidates= " + candidates);

		if(!candidates.isEmpty()) {
			graph.layoutWithExistingNodes(cls,node,candidates,includeNodes,processed);
		} else {
			LOG.debug("layoutOutboundEdgesWithPlacedNodes node=" + node + ": no layout towards placed neighbours");
		}

	}


	private void layoutOutboundToPlacedNodes(String node, ClassEntity cls, List<String> neighbours,
			List<String> includeNodes, List<String> processed) {

		List<String> candidates = neighbours.stream()
				.filter(n -> includeNodes.contains(n) && !graph.isPlaced(n))
				.map( n -> graph.hasPlacedSomeOutboundNeighbours(n,node) )
				.filter(opt_n -> opt_n.isPresent()) 
				.map(opt_n -> opt_n.get())
				.distinct().collect(Collectors.toList());

		LOG.debug("layoutOutboundToPlacedNodes: node=" + node + " process neighbours with outbound to placed neighbours: " + candidates);

		final List<String> cand = candidates;
		candidates = candidates.stream()
				.sorted(Comparator.comparing(p -> graph.getAdditionalNeighbours(p,cand)))
				.collect(Collectors.toList());		

		String prevNodeB=null;
		if(candidates.size()>0) { 
			for( String n : candidates) {
				String toNode = n;
				String nodeA = node;

				List<String> optB = graph.getOutboundNeighbours(n).stream()
						.filter(p -> !p.equals(nodeA))
						.filter(p -> graph.isPlaced(p))
						.collect(Collectors.toList());

				LOG.debug("layoutOutboundToPlacedNodes: node=" + node + " candidate: " + n + " optB: " + optB);
				LOG.debug("layoutOutboundToPlacedNodes: node=" + node + " nodeA: " + nodeA);
				LOG.debug("layoutOutboundToPlacedNodes: node=" + node + " prevNodeB: " + prevNodeB);

				String nodeB=null;
				if(prevNodeB!=null && optB.contains(prevNodeB))
					nodeB=prevNodeB;
				else if(optB.size()>0) 
					nodeB=optB.iterator().next();

				if(nodeB!=null) { //if(optB.isPresent()) {
					// String nodeB = optB.get();
					graph.placeBetween(cls, nodeA, nodeB, toNode, processed);
					graph.placeBetween(cls, nodeB, nodeA, toNode, processed);
				}
				prevNodeB=nodeB;
			}
		}

	}


	private void layoutBetweenAlreadyPlacedNodes(String node, ClassEntity cls, List<String> neighbours, List<String> includeNodes, List<String> processed) {

		List<String> candidates = neighbours.stream()
				.filter(n -> includeNodes.contains(n)) // && !graph.isPlaced(n))
				.map(n -> graph.hasAlreadyPlacedNeighbours(n,node) )
				.filter(opt_n -> opt_n.isPresent()) 
				.map(opt_n -> opt_n.get())
				.distinct().collect(Collectors.toList());

		LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " process neighbours with placed neighbours: " + candidates);

		candidates = candidates.stream()
				.filter( n -> graph.getInboundNeighbours(n).size()==2 && 
				(graph.getOutboundNeighbours(n).size()==0 ||
				(graph.getOutboundNeighbours(n).size()==1 && 
				graph.isLeafNode(graph.getOutboundNeighbours(n).iterator().next())))) // TODO or ==0 ?
				.collect(Collectors.toList());

		LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " process neighbours with placed neighbours: candidates=" + candidates);
		LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " candidates=" + candidates); 

		if(candidates.size()>1) { // TODO || (candidates.size()==1 && !graph.isSimple(candidates))) {
			candidates = candidates.stream()
					.sorted(Comparator.comparing(n -> graph.getOutboundNeighbours(n).size()))
					.collect(Collectors.toList());

			LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " SORTED candidates=" + candidates); 

			List<String> placedCandidates = new LinkedList<>();
			for( String n : candidates) {
				String toNode = n;
				String nodeA = node;
				Optional<String> optB = graph.getInboundNeighbours(n).stream().filter(p -> !p.equals(nodeA)).findFirst();

				boolean placed=false;
				boolean d;
				if(optB.isPresent()) {
					String nodeB = optB.get();
					d = graph.placeBetween(cls, nodeA, nodeB, toNode, processed);
					placed = placed|| d; 
					d = graph.placeBetween(cls, nodeB, nodeA, toNode, processed);
					placed = placed|| d; 
				}
				if(placed) placedCandidates.add(toNode);
			}

			LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " placed candidates=" + placedCandidates); 

			if(placedCandidates.size()>1) {
				Iterator<String> iter = placedCandidates.iterator();
				String from = iter.next();
				String rule = "layoutBetweenAlreadyPlacedNodes: force left right direction";

				LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " placed from=" + from); 
				while(iter.hasNext()) {
					String to = iter.next();
					LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " placed to=" + to); 
					// graph.placeEdges(cls, from, to, Place.RIGHT, rule, processed); 
					// graph.placeEdgeHelper(cls, Place.RIGHT, from, new ShadowEdge(from,to), processed);
					cls.addEdge(new HiddenEdge(from, Place.RIGHT, to));
					from=to;
				}
			}

		} else if(candidates.size()==1 && !graph.isSimple(candidates)) {
			String toNode = candidates.iterator().next();
			String nodeA = node;
			Optional<String> optB = graph.getInboundNeighbours(toNode).stream().filter(p -> !p.equals(nodeA)).findFirst();

			if(optB.isPresent()) {
				String nodeB = optB.get();
				graph.placeBetween(cls, nodeA, nodeB, toNode, processed);
				graph.placeBetween(cls, nodeB, nodeA, toNode, processed);
			}
		}		
		LOG.debug("layoutBetweenAlreadyPlacedNodes: node=" + node + " done");
	}


	public void addOrphanEnums(Diagram diagram, String resource, List<String> generated) {    
  	    List<String> seen = generated.stream()
  	    								.map(x-> Arrays.asList(x.split("-")))
  	    								.flatMap(List::stream)
  	    								.collect(Collectors.toList());

  	    Collection<String> allDefs = Utils.getAllDefinitions(swagger);
  	    Collection<String> allRefs = Utils.getAllReferenced(swagger);
  	    
  	    allDefs = allDefs.stream()
  	    		.filter(type -> Utils.isEnumType(swagger, type))
  	    		.collect(Collectors.toList());
  	    
  	    allDefs.removeAll(seen);
  	    allDefs.removeAll(allRefs);

	    allDefs.forEach(orphanEnum ->  addOrphanEnum(diagram, resource, orphanEnum, generated) );
		
	}
	
	public void addOrphanEnum(Diagram diagram, String resource, String orphanEnum, List<String> generated) {    
  	
    	EnumNode enode = new EnumNode(orphanEnum);
    	enode.addValues(Utils.getEnumValues(swagger,orphanEnum));
    	graph.addEnum(orphanEnum, enode);

    	ClassEntity entity = diagram.getResource(resource);
    	generateForEnum(entity, resource, orphanEnum);
		
	}


}
