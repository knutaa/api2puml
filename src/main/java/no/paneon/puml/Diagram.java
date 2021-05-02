package no.paneon.puml;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import no.paneon.common.Config;
import no.paneon.swagger2puml.Args;

public class Diagram {

	protected static Logger LOG = Logger.getLogger(Diagram.class);

    Config CONFIG = Config.getConfig();

	Map<String,ClassEntity> classes; 
	List<EdgeEntity> edgeEntities;
	Args args;
	JSONObject swagger;
	String file;
	
	public Diagram(Args args, JSONObject swagger, String file) {
		this.args = args;
		this.swagger = swagger;
		this.file = file;
		classes = new HashMap<>();
		edgeEntities = new LinkedList<>();
		Core.reset();
		ClassEntity.clear();
	}
	
	public Diagram addClass(ClassEntity c) {
        LOG.debug("Diagram: addClass: c=" + c);

        if(c!=null) classes.put(c.name,c);
		return this;
	}
	
	public Set<String> getResources() {
		return classes.keySet();
	}
	
	public static void usage() {
	 	try {
    		InputStream is = new ClassPathResource("layout.json").getInputStream();
    	    String config = IOUtils.toString(is, StandardCharsets.UTF_8.name());
    	    JSONObject json = new JSONObject(config); 
    	    
    		System.out.println(
					"Example layout configuration json (--config option):" + "\n" + 
					json.toString(2)
					);
			System.out.println();

		} catch (Exception e) {

		}
		
	}
	
	public String toString() {
		
		List<String> processed = new LinkedList<>();
		Config CONFIG = Config.getConfig();
		
    	String footer="";
	    if(args.source>0) {
	    	footer = "right footer ";
        	final JSONObject info = swagger.getJSONObject("info");
        	if(!info.isEmpty()) {
        		if(info.has("title")) footer = footer + info.getString("title");
        		if(info.has("version")) footer = footer + " v" + info.getString("version");
        	}
        	if(footer.length()>0 && args.source>1) {
        		File f = new File(file);
        		String basename = f.getName();
        		footer = footer + "  - file: " + basename;
        	}
	    }
	    if(footer.length()>0) footer = footer + "\n";
	    		
	    String res = CONFIG.getPuml();
	    
		res = res + footer + "\n";

		for(ClassEntity entity : classes.values()) {
            LOG.debug("Diagram: processing for resource=" + entity.name + " seq=" + entity.seq);
		}
		
		List<Integer> processedSeq = new LinkedList<>();

		int fromSeq=0;
		List<ClassEntity> classSeq = classes.values().stream()
										.sorted(Comparator.comparingInt(ClassEntity::getSeq))
										.collect(Collectors.toList());
		
		for(ClassEntity entity : classSeq) {
			
            LOG.debug("Diagram: processing for resource=" + entity.name);
            
            if(includeDebug()) {
            	res = res + "'sequence: " + entity.seq + '\n';
            }
            
			res = res + entity.toString();
			res = res + "\n";
			
			for(EnumEntity e : entity.enumEntities) {
				if(!processed.contains(e.type)) {
		            LOG.debug("Diagram: processing for enum=" + e);
	
		            if(includeDebug()) {
			            res = res + entity.getComments(fromSeq,e.getSeq());
			            res = res + "'sequence: " + e.getSeq() + '\n';
		            }
					res = res + e.toString();

					res = res + "\n";
					
					processed.add(e.type);
					fromSeq = e.getSeq();
		            LOG.debug("Diagram: last seen seq=" + fromSeq);

				}
	            processedSeq.add(e.getSeq());
			}
            processedSeq.add(entity.getSeq());
			
		}
		
		Stream<Core> stream = Core.stream(0);
		res = res + stream
				.filter(p -> !processedSeq.contains(p.getSeq()))
				.filter(p -> (p instanceof Comment && includeDebug() || !(p instanceof Comment)))
				.filter(p -> (p instanceof Comment) || (p instanceof EdgeEntity))
				.filter(p -> ((p instanceof ForcedHiddenEdge) || (p instanceof EnumEdge) || !(p instanceof HiddenEdge) || (p instanceof HiddenEdge && CONFIG.includeHiddenEdges())))
				// .peek(p ->  LOG.debug("Diagram: processing for entity=" + p.seq))
				.map(p -> p.toString())
				.collect(Collectors.joining("\n"));
			
		res = res + "\n";
		res = res + "@enduml";
		
		return res;
	
	}

	public ClassEntity getResource(String node) {
		return classes.get(node);
	}

	public void addEnum(EnumEntity enumEntity) {
		// TODO Auto-generated method stub
		
	}
	
//	private static Optional<Boolean> pumlComments = Optional.empty();
//	public static void setIncludeDebug(Optional<Boolean> v) {
//		pumlComments = v;		
//	}
	
	private boolean includeDebug() {
		// return pumlComments.isPresent() && pumlComments.get();
		return CONFIG.getIncludeDebug();
	}

}
