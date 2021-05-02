package no.paneon.swagger2puml;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;

import no.paneon.common.Config;
import no.paneon.puml.Diagram;


public class App 
{
		
    private static Logger LOG = Logger.getLogger(App.class);

	Args args;
	JSONObject layoutConfig;
	JCommander commandLine;
	
	Layout layout; 
	
	App(String ... argv) {
		args = new Args();
        
		commandLine = JCommander.newBuilder()	
        .addObject(args)
        .build();
		
		try {
			commandLine.parse(argv);
		} catch(Exception ex) {
			System.out.println(ex.getMessage());
			System.out.println("Use option --help or -h for usage information");
			System.exit(0);
		}
		
	   	BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.OFF);
		
	}
		
    public static void main( String ... argv )
    {
    	System.out.println("... swagger2puml ");

        App app = new App(argv);         
        app.run();  
    }
    
    private void run() {
    
        Config CONFIG = Config.getConfig();

        if (args.help) {
            commandLine.usage();
            CONFIG.usage();
            Diagram.usage();
            return;
        }
        
    	if(args.debug!=null) {
    		if(Utils.levelmap.containsKey(args.debug)) {
    			Level level = Utils.levelmap.get(args.debug);
    			Logger rootLogger = Logger.getRootLogger();
    			rootLogger.setLevel(level);
    		}
    	};
              
    	LOG.debug("args: " + args);

        Config.setDefaults(args.defaults);

        CONFIG.setIncludeDebug(args.pumlComments);

        CONFIG.setShowAllCardinality(args.showAllCardinality);

        CONFIG.setRequiredHighlighting(args.highlightRequired);
        CONFIG.setIncludeDescription(args.includeDescription);

        CONFIG.setFloatingEnums(args.floatingEnums);
               
        CONFIG.setOrphanEnums(args.orphanEnumConfig);

        CONFIG.setLayout(args.layout);

        CONFIG.setConfig(args.config);

    	LOG.debug("... load configuration ");

        try {
        	layoutConfig = CONFIG.getLayout(); // Utils.readJSON(args.config, args.config.equals(args.defaultConfig));
	        	
        	String file = args.files.size()>0 ? args.files.get(0) : args.file; 
        	String target = args.files.size()>1 ? args.files.get(1) : args.target; 

        	if(!target.endsWith("/")) target = target + "/";
        	
        	if(file==null) {
        		System.out.println("expected one file name as argument (try --help for usage details)");
        		System.exit(1);
        	}
        	
        	if(!file.endsWith(".json") && !file.endsWith(".yaml") && !file.endsWith(".yml")) {
        		System.out.println("file " + file + " is not of expected type (.json or .yaml/.yml)");
        		System.exit(2);
        	}
        	
        	LOG.debug("... load swagger definitions from " + file);
        	
        	JSONObject swagger = Utils.readJSONOrYaml(file);
        	
        	LOG.trace("... swagger: " + swagger.toString(2));

        	List<String> resources = Utils.extractResources(swagger);
         	
        	LOG.debug("resources=" + resources);

        	resources = resources.stream().distinct()
        					.map(x -> x.replaceAll(".*/([A-Za-z0-9.]*)", "$1"))
        					.collect(Collectors.toList());
        	       
        	List<String> resourcesFromRules = Utils.extractResourcesFromRules(args.rulesFile);
        	
        	resourcesFromRules.removeAll(resources);
        	resources.addAll( resourcesFromRules);
      
        	resources = resources.stream().distinct()
    					.filter(x -> (args.resource==null)||(x.equals(args.resource)))
    					.collect(Collectors.toList());

        	LOG.debug("resources=" + resources);
        	
        	final String TARGET = target;

        	resources.forEach(resource -> {
        		System.out.println("... processing " + resource);

        		LOG.info("... processing " + resource);
        	    Graph graph = new Graph(resource);
        	    graph.processResource(resource, swagger);
        	    
        	    Diagram diagram = new Diagram(args, swagger, file);
        	    
        	    layout = new Layout(swagger, graph, layoutConfig);
        	    
        	    LOG.debug("#");
        	    LOG.debug("# Process resource: resource=" + resource + " all nodes=" + graph.nodes);
        	    LOG.debug("#");
        	            	            	    
        	    graph.nodes.forEach(node -> {
        	        layout.generateUMLClasses(diagram, node, resource);
			    });
        	            	  
        	    LOG.debug("# diagram nodes=" + diagram.getResources());

        	    List<String> coreGraph = graph.extractCoreGraph(resource);
                LOG.debug("### " + resource + " core graph: " + coreGraph);
                LOG.debug("### " + resource + "  all nodes: " + graph.nodes);

                List<String> generated = new ArrayList<>();
                
                // first process edges for all core graph nodes
                LOG.debug("### ");
                LOG.debug("### " + resource + " process edges with core nodes: " + coreGraph);
                LOG.debug("### ");

                boolean edges=false;
                List<String> nodesToProcess = coreGraph.stream().collect(Collectors.toList());
                while(!nodesToProcess.isEmpty()) {
                	                    
                    List<String> sorted = nodesToProcess.stream()
											.sorted(Comparator.comparing(n -> { return -graph.getInboundEdgesFromPlaced(n);}))
											.collect(Collectors.toList());
        	
                    Optional<String> optNode = sorted.stream().findFirst();
                    		                	
                    LOG.debug("nodesToProcess:  optNode=" + optNode);

                	if(optNode.isPresent()) {
                		String node = optNode.get();
            	    	boolean res = layout.generateUMLEdges(diagram, node, resource, coreGraph, generated);
            	    	edges = edges || res;  
                    	nodesToProcess.remove(node);
                	}
                }
        	    
          	    if(edges) {
        	    	LOG.debug("node: " + resource + " generated edges with core nodes");
        	    } else {
        	    	LOG.debug("node: " + resource + " no edges with core nodes");
        	    }
        	            	    
        	    // add edges for non-core nodes
                LOG.debug("### ");
                LOG.debug("### " + resource + " process edges with non-core nodes: " + graph.nodes);
                LOG.debug("### ");

                edges=false;
                List<String> nodes = graph.nodes.stream()
                						.sorted(Comparator.comparing(n-> {
                							return -graph.getOutboundNeighbours(n).stream()
                								.filter(o -> !generated.contains(o))
                								.count();
                						}))
                						.collect(Collectors.toList());
                
                LOG.debug("### " + resource + " nodes: " + nodes);

        	    for(String node: nodes) {
        	    	boolean res = layout.generateUMLEdges(diagram, node, resource, graph.nodes, generated);
        	    	edges = edges || res;
        	    }
        	    
          	    if(edges) {
        	    	LOG.debug("node: " + resource + " generated edges with non-core nodes: " + edges);
        	    } else {
        	    	LOG.debug("node: " + resource + " no edges with non-core nodes");
        	    }
          	    
          	    List<String> orphans = CONFIG.getOrphanEnums();
          	    if(CONFIG.getIncludeOrphanEnums() || orphans.contains(resource)) {
          	    	List<String> orphanEnums = CONFIG.getOrphanEnums(resource);
          	    	if(orphanEnums.isEmpty())
          	    		layout.addOrphanEnums(diagram, resource, generated);   	
          	    	else {
          	    		orphanEnums.forEach(orphanEnum -> layout.addOrphanEnum(diagram, resource, orphanEnum, generated));
          	    	}
          	    		
          	    }
        	    
        	    String puml = diagram.toString();
        	    
        	    try {	
            	    String fileName = TARGET + "Resource_" + resource + ".puml";
            	    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));    	    
	        	    writer.write(puml);
	        	    writer.close();
        	    } catch(Exception ex) {
        	    	System.out.println("exception: " + ex);
        	    	// ex.printStackTrace();
        	    }


        	    graph.nodes.forEach(node -> {
        	    	LOG.debug("node: " + node + " outEdges=" + graph.graphNodes.get(node).outEdges);
        	        graph.graphNodes.get(node).outEdges.forEach(e -> {
    	                if(!e.isPlaced(generated)) {
    	                    LOG.error("edge not processed: " + e);
    	                }
        	        });
        	    });

        	});
        	
        } catch(Exception ex) {
        	LOG.error("Exception: " + ex);
	    	ex.printStackTrace();

       }
        
    }
    


}
	
