package no.paneon.swagger2puml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

public class Positions {
	
    private static Logger LOG = Logger.getLogger(Positions.class);

	Map<String,Position> position;
	
	Positions() {
		this.position = new HashMap<>();
	}
	
	void setPosition(String node) {
		position.put(node, new Position(true));
	}
	
	void positionToLeft(String from, String to) {
		Position pos=get(from);
		if(pos.isPositioned()) {
			pos=new Position(pos);
			pos.x -= 1;
			position.put(to, pos);
		} else {
			pos=get(to);
			pos=new Position(pos);
			pos.x += 1;
			position.put(from, pos);
		}
	}
	
	void positionToRight(String from, String to) {
		Position pos=get(from);
		if(pos.isPositioned()) {
			pos=new Position(pos);
			pos.x += 1;
			position.put(to, pos);
		} else {
			pos=get(to);
			pos=new Position(pos);
			pos.x -= 1;
			position.put(from, pos);
		}
	}
	
	void positionAbove(String from, String to) {
		Position fromPos=get(from);
		Position toPos=get(to);
		if(fromPos.isPositioned()) {
			if(!toPos.isPositioned()) {
				Position pos=new Position(fromPos);
				pos.y -= 1;
				position.put(to, pos);
			} else {
				Position pos=new Position(toPos);
				pos.y += 1;
				position.put(from, pos);
			}
		} else {
			Position pos=new Position(toPos);
			pos.y += 1;
			position.put(from, pos);
		}
	}
	
	void positionBelow(String from, String to) {
		Position pos=get(from);
		if(pos.isPositioned()) {
			pos=new Position(pos);
			pos.y += 1;
			position.put(to, pos);
		} else {
			pos=get(to);
			pos=new Position(pos);
			pos.y -= 1;
			position.put(from, pos);
		}
	}
	
	Position get(String node) {
		if(!position.containsKey(node)) {
			position.put(node,new Position());
		}
		return position.get(node);
	}
	
	List<String> getRightmostOf(Graph graph, String node) {
	    return getRightmostOf(graph,node,null);
	}
	 
	boolean isAtSameColumn(String nodeA, String nodeB) {
	    Position posA = get(nodeA);
	    Position posB = get(nodeB);
	    return posA.x==posB.x;	
	}
	
	boolean isAtSameLevel(String nodeA, String nodeB) {
	    Position posA = get(nodeA);
	    Position posB = get(nodeB);
	    return posA.y==posB.y;	
	}
	
	boolean isPositionedAbove(String nodeA, String nodeB) { 
	    Position posA = get(nodeA);
	    Position posB = get(nodeB);
	    boolean res = posA.y<posB.y;
	    
	    LOG.debug("isPositionedAbove: nodeA=" + nodeA + " nodeB=" + nodeB + " posA=" + posA + " posB=" + posB + " res=" + res);
	    return res;
	}
	
	boolean isPositionedBelow(String nodeA, String nodeB) { 
	    Position posA = get(nodeA);
	    Position posB = get(nodeB);
	    boolean res = posA.y>posB.y;
	    
	    LOG.debug("isPositionedBelow: nodeA=" + nodeA + " nodeB=" + nodeB + " posA=" + posA + " posB=" + posB + " res=" + res);
	    return res;
	}
	
	boolean isPositionedToRight(String nodeA, String nodeB) { 
	    Position posA = get(nodeA);
	    Position posB = get(nodeB);
	    boolean res = posA.x>posB.x;
	    
	    LOG.debug("isPositionedToRight: nodeA=" + nodeA + " nodeB=" + nodeB + " posA=" + posA + " posB=" + posB + " res=" + res);
	    return res;
	}
	
	boolean isPositionedToLeft(String nodeA, String nodeB) { 
	    Position posA = get(nodeA);
	    Position posB = get(nodeB);
	    boolean res = posA.x<posB.x;
	    
	    LOG.debug("isPositionedToLeft: nodeA=" + nodeA + " nodeB=" + nodeB + " posA=" + posA + " posB=" + posB + " res=" + res);	    
	    return res;
	}
	
	
	List<String> getRightmostOf(Graph graph, String node, String start) {
	    List<String> res = new ArrayList<>();
	    
	    res.add(node);
	    
	    if(start==null || !node.equals(start)) {
	        List<Position> rightmostPosition=new ArrayList<>();
	        LOG.debug("getRightmostof: node=" + node);
	        graph.getPlacedAt(node,Place.LEFT)
	        .stream()
	        .filter(n-> !n.equals(start))
	        .map(n-> {
	        	LOG.debug("n=" + n + " node=" + node); 
	        	return this.getRightmostOf(graph,n,node);
	        })
	        .forEach(m -> {
	            LOG.debug("getRightmostOf: m=" + m); 
	            m.forEach(n -> {
	                LOG.debug("getRightmostOf: n=" + n);
	                Position pos = get(n);
	                if(rightmostPosition.isEmpty()) {
	                    rightmostPosition.add(pos);
	                    res.add(n);
	                } else if(pos.x>rightmostPosition.get(0).x) {
	                    rightmostPosition.add(0,pos);
	                    res.add(n);
	                }
	            });
	        });
	    };
        LOG.debug("getRightmostof: res=" + res);
	    return res;
	}
	
	List<String> getAllRightLeftOf(Graph graph, String node, Place direction) {
		List<String> processed = new ArrayList<>();
		return getAllRightLeftOfHelper(graph, node, direction, processed);
	}
	
	List<String> getAllRightLeftOfHelper(Graph graph, String node, Place direction, List<String> processed) {
	    List<String> res=new ArrayList<>();
	    for( String n : graph.getPlacedAt(node,Place.mapping.get(direction))) {
	        if(!processed.contains(n)) {
	            processed.add(n);
	            LOG.debug("getAllRightLeftOf: n =" + n);
	            res.add(n);
	            List<String> tmp = getAllRightLeftOfHelper(graph, n, direction, processed);
	            res.addAll(tmp);
	        }
	    };
	    if(res.size()>0) res.remove(node);
	    return res;
	}
	
	int currentlyPlacedAtLevel(String node) {
		return currentlyPlacedAtLevel(node,0); 
	}
	
	int currentlyPlacedAtLevel(String node, int offset) {
		int res=0;
		if(position.containsKey(node)) {
			Position pos = position.get(node);
			List<String> nodes = position.keySet().stream()
									.filter(n -> position.get(n).y==pos.y+offset)
									.distinct().collect(Collectors.toList());
            LOG.trace("currentlyPlacedAtLevel: node=" + node + " offset=" + offset + " nodes=" + nodes);
            res = nodes.size();
		}
		return res;
	}

	List<String> placedAtLevel(String node) {
		List<String> res = new LinkedList<>();
		if(position.containsKey(node)) {
			Position pos = position.get(node);
			res = position.keySet().stream()
						.filter(n -> position.get(n).y==pos.y)
						.collect(Collectors.toList());
		}
        LOG.debug("placedAtLevel: node=" + node + " nodes=" + res);
        res.forEach(n -> { 
        	LOG.debug("placedAtLevel: node=" + node + " n=" + n + " pos=" + get(n));
        });

		return res;
	}
	
	boolean isUnbalancedLevel(String node) {
		long levelWidth = currentlyPlacedAtLevel(node,+1);
		long aboveWidth = currentlyPlacedAtLevel(node,-1);
		return levelWidth>aboveWidth;
	}

	public void position(String from, String to, Place direction) {
		switch(direction) {
		case LEFT: 
		    this.positionToLeft(from, to);
			break;
			
		case RIGHT: 
		    this.positionToRight(from,to);
			break;			
			
		case ABOVE: 
		    this.positionAbove(from,to);
			break;
			
		case BELOW:
		    this.positionBelow(from,to);
			break;
			
		default:
			break;
		
		}
	}

	public List<String> currentlyPlacedBetween(String nodeA, String nodeB) {
		List<String> res = new LinkedList<>();
		if(!isAtSameLevel(nodeA,nodeB)) return res;
		Position posA = get(nodeA);
		Position posB = get(nodeB);
		res = placedAtLevel(nodeA).stream()
				.filter(n -> {
					Position p = get(n);
			        LOG.debug("currentlyPlacedBetween: n=" + n + " p=" + p + " posA=" + posA + " posB=" + posB);
					if(posA.x<posB.x) return posA.x<p.x && p.x<posB.x;
					if(posB.x<posA.x) return posB.x<p.x && p.x<posA.x;
					return false;
				}).collect(Collectors.toList());
		return res;
	}

	
	public List<String> currentPlaced(String node, Place direction) {
		return placedAtLevel(node).stream()
				.filter(n -> !n.equals(node))
				.filter(n -> isPlaced(n,node,direction)) 
				.collect(Collectors.toList());
	}
	
	private boolean isPlaced(String nodeA, String nodeB, Place direction) {
		Position posA = get(nodeA);
		Position posB = get(nodeB);

		boolean res=false;
		switch(direction) {
		case LEFT: 
			res = posA.x<posB.x;
			break;

		case RIGHT: 
			res = posA.x>posB.x;
			break;			

		case ABOVE: 
			res = posA.y<posB.y;
			break;

		case BELOW:
			res = posA.y>posB.y;
			break;
		default:
		}

		return res;

	}

	public List<String> currentPlacedRightOf(String node) {
		Position pos = get(node);
		return placedAtLevel(node).stream()
				.filter(n -> !n.equals(node))
				.filter(n -> { Position p = get(n); return p.x>=pos.x;}) 
				.collect(Collectors.toList());
	}
	
	public List<String> currentPlacedLeftOf(String node) {
		Position pos = get(node);
		return placedAtLevel(node).stream()
				.filter(n -> !n.equals(node))
				.filter(n -> { Position p = get(n); return p.x<=pos.x;}) 
				.collect(Collectors.toList());
	}
	
	public List<String> currentPlacedAbove(String node) {
		Position pos = get(node);
		return placedAtLevel(node).stream()
				.filter(n -> !n.equals(node))
				.filter(n -> { Position p = get(n); return p.y<=pos.y;}) 
				.collect(Collectors.toList());
	}
	
	public List<String> currentPlacedBelow(String node) {
		Position pos = get(node);
		return placedAtLevel(node).stream()
				.filter(n -> !n.equals(node))
				.filter(n -> { Position p = get(n); return p.y>=pos.y;}) 
				.collect(Collectors.toList());
	}
	
	public boolean isPlaced(String node, Place direction) {
		List<String> placed = currentPlaced(node,direction);
		
		return !placed.isEmpty();

	}
	
}
