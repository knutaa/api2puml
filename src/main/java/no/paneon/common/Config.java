package no.paneon.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import no.paneon.swagger2puml.Utils;

public class Config {
    
    private static Logger LOG;

	private static void initLogger() {
		if(LOG==null) LOG = Logger.getLogger(Config.class);
	}

    private static String defaults = null;
    
    public static void setDefaults(String file) {
    	if(file!=null) {
    		defaults = file;
    		// _config = new Config(true);
    		JSONObject defaults = Utils.readJSONOrYaml(file);
    		
    		Config config = getConfig();
    		config.addConfiguration(defaults);
    	}
    }
    
    private static Config _config = null; // new Config(true);
    
    public static String CARDINALITY_MANDATORY = "1";
    public static String CARDINALITY_OPTIONAL = "0..1";

    private JSONObject json = null;
    private boolean _initialized = false;

    public static Config getConfig() {
		initLogger();
    	if(_config==null) _config = new Config();

		LOG.debug("getConfig() _config=" + _config);
				
    	return _config;
    }
            
    private Config() {
    	initLogger();

    	LOG.debug("Config()");
		this.json = new JSONObject();
		init();    	
    }

//	private Config(boolean force) {
//		initLogger();
//
//		LOG.debug("Config(force=" + force + ")");
//		this.json = new JSONObject();
//		init(force);    	
//    }
    
	public void usage() {				
		if(json!=null) {
			try {
				System.out.println(
						"Default configuration json (--default option):" + "\n" + 
						json.toString(2)
						);
				System.out.println();
			} catch(Exception ex) {
				
			}
		}
	}
	
//	private void init() {
//		init(false);
//	}
//    
	private void init() {

		LOG.debug("init() _init=" + _initialized);

    	_initialized = true;

		LOG.debug("init() before read");
		
	    List<String> configFiles = new LinkedList<>();
	    if(defaults!=null) configFiles.add(defaults);
	    
    	try {
    		InputStream is ;
			is = new ClassPathResource("configuration.json").getInputStream();
			addConfiguration(json,is,"configuration.json");
	   		
    		for(String file : configFiles) {
    			System.out.println("... adding configuration from file " + file);

    			is = new BufferedInputStream(new FileInputStream(new File(file)));
    			addConfiguration(json,is,file);
    		}
    		  	   
		} catch (Exception e) {
			System.err.println("Error processing configuration files: " + e);
			System.exit(0);
		}
    	
		
    }
	
//	private void init(boolean force) {
//
//    	if(_initialized && !force) return;
//
//		LOG.debug("init() _init=" + _initialized);
//
//    	_initialized = true;
//
//		LOG.debug("init() before read");
//		
//	    List<String> configFiles = new LinkedList<>();
//	    if(defaults!=null) configFiles.add(defaults);
//	    
//    	try {
//    		InputStream is ;
//			is = new ClassPathResource("configuration.json").getInputStream();
//			addConfiguration(json,is,"configuration.json");
//	   		
//    		for(String file : configFiles) {
//    			System.out.println("... adding configuration from file " + file);
//
//    			is = new BufferedInputStream(new FileInputStream(new File(file)));
//    			addConfiguration(json,is,file);
//    		}
//    		  	   
//    	    System.out.println("init: json=" + json.toString(2));
//
//		} catch (Exception e) {
//			System.err.println("Error processing configuration files: " + e);
//			System.exit(0);
//		}
//    	
//		
//    }
	    

	private void addConfiguration(JSONObject json, InputStream is, String name) throws Exception {
	    String config = IOUtils.toString(is, StandardCharsets.UTF_8.name());
	    
	    if(name.endsWith("yaml") || name.endsWith("yml")) config = convertYamlToJson(config);
	    
	    JSONObject deltaJSON = new JSONObject(config); 
	    
	    addConfiguration(json, deltaJSON);
   		    
	}

	public void addConfiguration(JSONObject json, JSONObject deltaJSON) {  	 		
	    for(String key : deltaJSON.keySet()) {	    	
	    	json.put(key, deltaJSON.get(key));
	    }	   	
	}
	
	public void addConfiguration(JSONObject deltaJSON) {  	 		
		addConfiguration(this.json, deltaJSON);
	}

	
	public List<String> getSimpleTypes() {

		if(has("simpleTypes")) {
			return get("simpleTypes");
		} else {
			return Arrays.asList("TimePeriod", "Money", "Quantity", "Tax", 
								 "Value", "Any", "object", "Number", "Date");
		}
	}
	
	public List<String> getSimpleEndings() {
		
		if(has("simpleEndings")) {
			return get("simpleEndings");
		} else {
			return Arrays.asList("Type", "Error");
		}
	}

	public List<String> getNonSimpleEndings() {
		if(has("nonSimpleEndings")) {
			return get("nonSimpleEndings");
		} else {
			return Arrays.asList("RefType", "TypeRef");
		}
	}  
	
	public String getPuml() {
		if(has("puml")) {
			return String.join("\n", get("puml"));
		} else {
			return "@startuml" + "\n" +
			"'default config" + "\n" + 
            "hide circle" + "\n" +
            "hide methods" + "\n" +
            "hide stereotype" + "\n" +
            "show <<Enumeration>> stereotype" + "\n" +
            "skinparam class {" + "\n" +
            "   BackgroundColor<<Enumeration>> #E6F5F7" + "\n" +
            "   BackgroundColor<<Ref>> #FFFFE0" + "\n" +
            "   BackgroundColor<<Pivot>> #FFFFFFF" + "\n" +
            "   BackgroundColor #FCF2E3" + "\n" +
            "}" + "\n" +
            "\n";
		}
	}
	
	public boolean includeHiddenEdges() {
		if(has("includeHiddenEdges")) {
			return getBoolean("includeHiddenEdges");
		} else {
			return false;
		}
	}
	
	public int getMaxLineLength() {
		int res=80;
		if(has("maxLineLength")) {
			res=json.getInt("maxLineLength");
		}
		return res;
	}
	
	private boolean has(String property) {
		// init();
		return json!=null && json.has(property);
	}
	
	private List<String> get(String property) {
		List<String> res = new LinkedList<>();
		
		JSONArray o = json.optJSONArray(property);
		if(o!=null)
			res.addAll(o.toList().stream().map(Object::toString).collect(Collectors.toList()));
		return res;
	}
	
	private boolean getBoolean(String property) {
		boolean res = json.getBoolean(property);
		return res;
	}

	public boolean showDefaultCardinality() {
		if(has("showDefaultCardinality")) {
			return getBoolean("showDefaultCardinality");
		} else {
			return false;
		}
	}

	public String getDefaultCardinality() {
		if(has("defaultCardinality")) {
			return getString("defaultCardinality");
		} else {
			return "0..1";
		}
	}

	private Optional<Boolean> showAllCardinality = Optional.empty();
	public void setShowAllCardinality(boolean value) {
		showAllCardinality=Optional.of(value);
	}

	public boolean hideCardinalty(String cardinality) {		
		if(showAllCardinality.isPresent() && showAllCardinality.get())  
			return false;
		
		if(showDefaultCardinality()) 
			return false;
		
		return cardinality.equals(getDefaultCardinality());
	}
	
	private String getString(String property) {
		String res = "";
		try {
			res = json.getString(property);
		} catch(Exception e) {
			
		}
		return res;
	}

	public String getRequiredFormatting() {
		if(optRequiredHighlighting.isPresent() && optRequiredHighlighting.get() && has("requiredHighlighting")) {
			return getString("requiredHighlighting");
		} else {
			return "%s";
		}
	}
	
	private Optional<Boolean> optRequiredHighlighting = Optional.empty();
	public void setRequiredHighlighting(boolean value) {
		optRequiredHighlighting=Optional.of(value);
	}
	
	public boolean getUseRequiredHighlighting() {
		LOG.debug("useRequiredHighlighting: opt=" + optIncludeDescription);
		if(has("useRequiredHighlighting")) {
			return getBoolean("useRequiredHighlighting");
		} else if(optRequiredHighlighting.isPresent()) {
			return optRequiredHighlighting.get();
		} else {
			return false;
		}
	}
	
	public boolean includeDescription() {
		LOG.debug("includeDescription: opt=" + optIncludeDescription);
		if(optIncludeDescription.isPresent()) 
			return optIncludeDescription.get();
		else if(has("includeDescription")) {
			return getBoolean("includeDescription");
		} else {
			return false;
		}
	}
	
	private Optional<Boolean> optIncludeDescription = Optional.empty();
	public void setIncludeDescription(boolean value) {
		this.optIncludeDescription = Optional.of(value);
	}

	public List<String> getOrphanEnums() {
		List<String> res = new LinkedList<>();
		if(has("orphan-enums")) {
			res = this.get("orphan-enums");
		}
		return res;
	}
	
	public List<String> getOrphanEnums(String resource) {
		List<String> res = new LinkedList<>();
		
		JSONObject config = getJSONObject("orphan-enums-by-resource");
		
		if(config.has(resource)) {
			JSONArray enums = config.optJSONArray(resource);
			if(enums!=null)
				res = enums.toList().stream().map(Object::toString).collect(Collectors.toList());
		}
				
		return res;
	}

	public boolean getIncludeOrphanEnums() {
		boolean res = false;
		if(has("include-orphan-enums")) {
			res = this.getBoolean("include-orphan-enums");
		}
		return res;
	}
	
	public void setOrphanEnums(String configFile) {
		try {
			JSONObject enum_config = null;
			if(configFile!=null) {
				LOG.debug("setOrphanEnums:: configFile=" + configFile);
				enum_config = Config.readJSONOrYaml(configFile);
			
				List<String> resources=new LinkedList<>();
				if(enum_config.has("orphan-enums") && enum_config.optJSONArray("orphan-enums")!=null) {
					resources.addAll( enum_config.getJSONArray("orphan-enums").toList().stream().map(Object::toString).collect(Collectors.toList()) );
					
				}
				
				JSONObject config = enum_config.optJSONObject("orphan-enums-by-resource");
				if(config!=null) {
					json.put("orphan-enums-by-resource", config);
					resources.addAll( config.keySet().stream()
										.filter(item -> !resources.contains(item))
										.collect(Collectors.toList()));
										
				}

				if(!resources.isEmpty()) json.put("orphan-enums", resources);

			}
			
		} catch(Exception e) {
			System.out.println("error reading file: " + configFile);
			System.exit(0);
		}
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

	public boolean getFloatingEnums() {
		boolean res=false;
		if(has("floatingEnums")) 
			res=getBoolean("floatingEnums");
		else
			res=floatingEnums.isPresent() && floatingEnums.get();
		return res;
	}

	Optional<Boolean> floatingEnums = Optional.empty();
	public void setFloatingEnums(boolean floatingEnums) {
		this.floatingEnums=Optional.of(floatingEnums);
	}

	public void setArguments(String argfile) {
		if(argfile!=null) {
			 JSONObject args = readJSONOrYaml(argfile);
			 for(String key : args.keySet() ) {
				 json.put(key, args.get(key));
			 }
		}
	}

	Optional<Boolean> includeDebug = Optional.empty();
	public void setIncludeDebug(boolean value) {
		LOG.debug("setIncludeDebug: " + value);

		this.includeDebug=Optional.of(value);
	}
	
	public boolean getIncludeDebug() {
		boolean res=false;
		if(has("includeDebug")) 
			res=getBoolean("includeDebug");
		else
			res=includeDebug.isPresent() && includeDebug.get();
		
		LOG.debug("getIncludeDebug: has=" + has("includeDebug"));
		LOG.debug("getIncludeDebug: " + res);
		
		return res;
	}

	public JSONObject getLayout() {
		return getJSONObject("layout");
	}
	
	private JSONObject getJSONObject(String label) {
		if(json.optJSONObject(label)!=null) 
			return json.optJSONObject(label);
		else
			return new JSONObject();
	}
	
	private void set(String label, Object value) {
		json.put(label,value);
	}

	public void setLayout(String layout) {
		if(layout!=null) {
			JSONObject o = readJSONOrYaml(layout);
			if(!o.isEmpty()) set("layout",o);
		}
	}

	public void setConfig(String config) {
		if(config!=null) {
			JSONObject o = readJSONOrYaml(config);
			this.addConfiguration(o);
		}
	}

	public boolean hideCardinaltyForProperty(String cardinality) {
		return !getBoolean("showCardinalitySimpleProperties");
	}

	public JSONObject getBaseTypes() {
		return getJSONObject("baseTypes");
	}

	
}
