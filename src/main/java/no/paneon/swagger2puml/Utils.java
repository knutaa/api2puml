package no.paneon.swagger2puml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import no.paneon.common.Config;

public class Utils {
	
    private static Logger LOG = Logger.getLogger(Utils.class);
    private static Config CONFIG = Config.getConfig();

	@SuppressWarnings("serial")
    static Map<String, Level> levelmap = new HashMap<String, Level>() {{	
    	put("info", Level.INFO);
    	put("error", Level.ERROR);
    	put("debug", Level.DEBUG);
    	put("trace", Level.TRACE);
    	put("warn", Level.WARN);
    	put("fatal", Level.FATAL);
    	put("all", Level.ALL);
    	put("off", Level.OFF);

    }};
    
	@SuppressWarnings("serial")
	static Map<String,String> formatToType = new HashMap<String,String>() {{
        put("date-time", "DateTime");
        put("date", "Date");
        put("float", "Float");
        put("uri", "Uri");
        put("url", "Url");
	}};

	@SuppressWarnings("serial")
	static Map<String,String> typeMapping = new HashMap<String,String>() {{
		put("integer", "Integer");
		put("string", "String");
		put("boolean", "Boolean");
	}};

	@SuppressWarnings("serial")
	static Map<String,String> stereoTypeMapping = new HashMap<String,String>() {{
		put("Ref", " <<Ref>>");
		put("Relationship", " <<Ref>>");
	}};
	
	static boolean isSimpleType(JSONObject swagger, String type) {
	    List<String> simpleTypes = CONFIG.getSimpleTypes(); 
    
	    List<String> simpleEndings = CONFIG.getSimpleEndings();       
	    List<String> nonSimpleEndings = CONFIG.getNonSimpleEndings();
	    
        if(isEnumType(swagger,type)) {
        	return true;
        }
        else if(simpleTypes.contains(type)) {
	        return true;
	    } else {
	        boolean nonSimple=nonSimpleEndings.stream().anyMatch(x -> type.endsWith(x));
	        if(nonSimple) return false;
	
	        return simpleEndings.stream().anyMatch(x -> type.endsWith(x));
	    }
	}

	
	static String type(JSONObject property, String ref) {
	    if(ref!=null) {
	        return ref;
	    } else {		        
	        if(property.has("format")) {
	            String formatMapping = formatToType.get(property.getString("format"));
	            if(formatMapping!=null) {
	                return formatMapping;
	            }
	            LOG.error("format: " + property.getString("format") + " has no mapping, using type and format");
	            return property.getString("type") + '/' + property.getString("format");
	        }
	        if(typeMapping.get(property.getString("type"))!=null) {
	            return typeMapping.get(property.getString("type"));
	        }
	        return property.getString("type");
	    }
	}

	static String getStereoType(Graph graph, String node, String pivot) {
	    if(node.equals(pivot))
	        return " <<Pivot>>";
	    else {
	        String res = "";
	        List<Property> props = graph.graphNodes.get(node).properties;
	        boolean hasRef = props.stream().anyMatch(p -> p.getName().equals("href"));
	        if(hasRef) {
	        	Optional<String> key = stereoTypeMapping.keySet().stream().filter(x -> node.endsWith(x)).findFirst();
	        	if(key.isPresent()) res=stereoTypeMapping.get(key.get());
	        }
	        return res;
	    }
	}
	
	static boolean isEnumType(JSONObject swagger, String type) {
	    boolean res=false;
	    JSONObject def = getDefinition(swagger,type);
	    if(def!=null)
	        res = def.has("enum");
        LOG.trace("isEnumType: checking type=" + type + " res=" + res);
	    return res;
	}
	
	static List<String> JSONArrayToList(JSONArray array) {
		List<String> res = new ArrayList<>();
		Iterator<Object> it = array.iterator();
	    while (it.hasNext()) {
	    	res.add((String)it.next());
	    }
	    return res;
	}
	
	public static JSONObject readJSONOrYaml(String file) {
		JSONObject res = null;
		try {
			if(file.endsWith(".yaml") || file.endsWith(".yml")) 
				res = readYamlAsJSON(file,false);
			else
				res = readJSON(file,false);
		} catch(Exception e) {
			System.out.println("... unable to read file " + file + " (error: " + e.getLocalizedMessage() + ")");
			System.exit(0);
		}
		return res;
	}
	
	static JSONObject readYamlAsJSON(String fileName, boolean errorOK) throws Exception {
		try {
			String path = fileName.replaceFirst("^~", System.getProperty("user.home"));
	        File file = new File(path);
	        String yaml = FileUtils.readFileToString(file, "utf-8");
	        String json = convertYamlToJson(yaml);
	        return new JSONObject(json); 
		} catch(Exception ex) {
			if(!errorOK) throw(ex);
			return new JSONObject();
		}
    }
	
	static String convertJsonToYaml(JSONObject json) throws Exception {
		YAMLFactory yamlFactory = new YAMLFactory()	
			 .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) 
	         .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
	         // .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
	         ;
		
		YAMLMapper mapper = new YAMLMapper(yamlFactory);
	    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

		
	    ObjectMapper jsonMapper = new ObjectMapper();
	    jsonMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
	    
	    jsonMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

	    JsonNode json2 = mapper.readTree(json.toString());
	    
	    final Object obj = jsonMapper.treeToValue(json2, Object.class);
	    final String jsonString = jsonMapper.writeValueAsString(obj);

	    LOG.debug("convertJsonToYaml: json=" + jsonString);
	    
	    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        String jsonAsYaml = mapper.writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
        
	}
	
    static String convertYamlToJson(String yaml) throws Exception {
	    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
	    Object obj = yamlReader.readValue(yaml, Object.class);

	    ObjectMapper jsonWriter = new ObjectMapper();
	    return jsonWriter.writeValueAsString(obj);
	}
	
    
	static JSONObject readJSON(String fileName, boolean errorOK) throws Exception {
		try {
			String path = fileName.replaceFirst("^~", System.getProperty("user.home"));
	        File file = new File(path);
	        String content = FileUtils.readFileToString(file, "utf-8");
	        return new JSONObject(content); 
		} catch(Exception ex) {
			if(!errorOK) throw(ex);
			return new JSONObject();
		}
    }
	
	static List<String> extractResources(JSONObject swagger) {
		List<String> res = new ArrayList<>();
		
		if(isOpenAPIv2(swagger)) {
			swagger.getJSONObject("paths").keySet().forEach( path ->  {
	
				JSONObject pathObj = swagger.getJSONObject("paths").getJSONObject(path);
				pathObj.keySet().forEach( op -> {
					JSONObject opObj = pathObj.getJSONObject(op);
					opObj.getJSONObject("responses").keySet().forEach( resp -> {
						if(!"default".equals(resp) && Integer.parseInt(resp)<300) {
							JSONObject respObj = opObj.getJSONObject("responses").getJSONObject(resp);
							if(respObj.has("schema")) {
								JSONObject schema = respObj.getJSONObject("schema");
								if(schema.has("$ref")) {
									res.add(schema.getString("$ref"));
								} else if(schema.has("items") && schema.getJSONObject("items").has("$ref")) {
									res.add(schema.getJSONObject("items").getString("$ref"));
								}
							}
						}
					});
				});
			});
		} else {
			
			swagger.getJSONObject("paths").keySet().forEach( path ->  {	
				JSONObject pathObj = swagger.getJSONObject("paths").getJSONObject(path);
				pathObj.keySet().forEach( op -> {
					JSONObject opObj = pathObj.getJSONObject(op);
					opObj.getJSONObject("responses").keySet().forEach( resp -> {
						if(!"default".equals(resp) && Integer.parseInt(resp)<300) {
							JSONObject respObj = opObj.getJSONObject("responses").getJSONObject(resp);
							if(respObj.has("content")) {
								JSONObject content = respObj.getJSONObject("content");
								Optional<String> key = content.keySet().stream().filter(k -> k.startsWith("application/json")).findFirst();
								if(key.isPresent()) content=content.optJSONObject(key.get());
								if(content.has("schema")) {
									JSONObject schema = content.getJSONObject("schema");
									if(schema.has("$ref")) {
										res.add(schema.getString("$ref"));
									} else if(schema.has("items") && schema.getJSONObject("items").has("$ref")) {
										res.add(schema.getJSONObject("items").getString("$ref"));
									}
								}
							}
						}
					});
				});
			});
		}
			
		return res;
		
	}
	
	private static boolean isOpenAPIv2(JSONObject swagger) {
		return !swagger.has("openapi");
	}


	static Collection<String> getAllDefinitions(JSONObject swagger) {
		return getDefinitions(swagger).keySet();
	}
	
	public static String dump(Collection<String> collection) {
		return collection.stream().collect(Collectors.joining(", "));
	}
	
	static TreeSet<String> intersection(List<String> al, Set<String> b) {
		Set<String> a = new TreeSet<>(al);
		return intersection(a,b);
	}
	
	static TreeSet<String> intersection(Set<String> a, Set<String> b) {
	    if (a.size() > b.size()) {
	        return intersection(b, a);
	    }

	    TreeSet<String> results = new TreeSet<>();

	    for (String element : a) {
	        if (b.contains(element)) {
	            results.add(element);
	        }
	    }

	    return results;
	}
	
	static Set<String> difference(List<String> al, List<String> bl) {		
		Set<String> a = new TreeSet<>(al);
		Set<String> b = new TreeSet<>(bl);
		
		a.removeAll(b);

	    return a;
	}
	
	static Set<String> difference(List<String> al, Set<String> b) {
		Set<String> a = new TreeSet<>(al);
		a.removeAll(b);
	    return a;
	}
	
	static Set<String> difference(Set<String> al, Set<String> b) {
		Set<String> a = new TreeSet<>(al);
		a.removeAll(b);
	    return a;
	}
	
	static Set<String> union(List<String> al, List<String> bl) {		
		Set<String> a = new TreeSet<>(al);
		Set<String> b = new TreeSet<>(bl);
		
		a.addAll(b);

	    return a;
	}
	
	static Set<String> union(Set<String> a, Set<String> b) {	
		Set<String> res = new TreeSet<>(a);
		
		res.addAll(b);

	    return res;
	}


	public static List<String> getEnumValues(JSONObject swagger, String node) {
		List<String> res = new LinkedList<>();
		JSONObject def = Utils.getDefinition(swagger,node);
		if(def!=null && def.has("enum")) {
			JSONArray values = def.optJSONArray("enum");
			if(values!=null) res.addAll(values.toList().stream().map(Object::toString).collect(Collectors.toList()));
		}
		return res;
	}


	public static JSONObject getDefinition(JSONObject swagger, String node) {
		JSONObject res=null;
		JSONObject definitions = Utils.getDefinitions(swagger);
		if(definitions!=null) {
			res = definitions.optJSONObject(node);
		}
		return res;	
	}


	public static Collection<String> getAllReferenced(JSONObject swagger) {
		List<String> res = new LinkedList<>();
		
		getAllDefinitions(swagger).forEach(resource -> {
			res.addAll(getAllReferenced(swagger,resource));
		});
		
		return res.stream().distinct().collect(Collectors.toList());
	}


	private static Collection<? extends String> getAllReferenced(JSONObject swagger, String resource) {
		List<String> res = new LinkedList<>();
		
		JSONObject def = getDefinition(swagger, resource);
		if(def.has("properties") && def.optJSONObject("properties")!=null) {
			final JSONObject property = def.optJSONObject("properties");
			property.keySet().forEach(prop -> {
				JSONObject o = property.optJSONObject(prop);
				if(o.has("$ref")) {
					String ref = o.optString("$ref");
					if(!ref.isEmpty()) res.add( lastElement(ref,"/") );
				} else if (o.has("items")) {
					String ref = o.optJSONObject("items").optString("$ref");
					if(!ref.isEmpty()) res.add( lastElement(ref,"/") );
				}
			});
		}
		
		return res;		
	}


	private static String lastElement(String ref, String delim) {
		String[] s = ref.split(delim);
		return s[s.length-1];
	}


	public static JSONObject getDefinitions(JSONObject swagger) {
		JSONObject res=null;
		if(Utils.isOpenAPIv2(swagger))
			res=swagger.getJSONObject("definitions");
		else {
			JSONObject components = swagger.optJSONObject("components");
			if(components!=null) res = components.optJSONObject("schemas");
		}
		return res;
	}


	public static List<String> extractResourcesFromRules(String rulesFile) {
		List<String> res = new LinkedList<>();
		try {
			JSONObject rules = readYamlAsJSON(rulesFile,true);
			Iterator<String> iter = rules.keySet().iterator();
			if(iter.hasNext()) {
				String apiKey = iter.next();
				if(rules.has(apiKey)) rules = rules.optJSONObject(apiKey);
				if(rules!=null && rules.has("resources")) {
					JSONArray resources = rules.optJSONArray("resources");
					res = resources.toList().stream().map(Object::toString).collect(Collectors.toList());
				}
			}
		} catch(Exception e) {
			LOG.error("unable to read API rules from " + rulesFile);
		}
		return res;

	}


	public static boolean isBaseType(String pivot, String resource) {
		boolean res=false;
		JSONObject config = CONFIG.getBaseTypes();
		if(config.has(pivot)) {
			JSONArray baseTypes = config.optJSONArray(pivot);
			res = baseTypes.toList().stream().map(Object::toString).anyMatch(s -> s.contentEquals(resource));
		} else if(config.has("common")) {
			JSONArray baseTypes = config.optJSONArray("common");
			res = baseTypes.toList().stream().map(Object::toString).anyMatch(s -> s.contentEquals(resource));			
		}
 		return res;
	}
	
	
}
