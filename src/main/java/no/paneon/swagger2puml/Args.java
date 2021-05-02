package no.paneon.swagger2puml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Level;

import com.beust.jcommander.Parameter;

public class Args {
	  @Parameter(names = { "-t", "--target" }, description = "Target directory for .puml files")
	  public String target = ".";

	  @Parameter(names = { "-f", "--file" }, description = "Input swagger file (or optionally as default argument)")
	  public String file;
	  
	  @Parameter(names = { "-c", "--config" }, description = "Arguments and configuration settings")
	  public String config = null;
	  
	  @Parameter(names = { "--layout" }, description = "Layout configuration settings")
	  public String layout = null;
	  
	  @Parameter(names = { "-d", "--debug" }, description = "Debug mode (off,all,info,debug,error,trace,warn,fatal)")
	  public String debug = "off";
	  
	  @Parameter(names = { "-s", "--source" }, description = "Include source details in footer (0=no, 1=basic, >1 include filename)")
	  public Integer source = 0;
	  
	  @Parameter(names = { "-r", "--resource" }, description = "Specific resource to process (default is all)")
	  public String resource = null;
	  
	  @Parameter(names = { "--defaults" }, description = "Default settings (file name)")
	  public String defaults = null;
	  
	  @Parameter(names = { "-h", "--help" }, description = "Usage details", help = true)
	  public boolean help = false;
	  
	  @Parameter(names = { "--include-puml-comments" }, description = "Add comments in .puml file")
	  public boolean pumlComments = false;
	  
	  @Parameter(names = { "--show-all-cardinality" }, description = "Include cardinality details for all properties (including default cardinality)")
	  public boolean showAllCardinality = false;
	  
	  @Parameter(names = { "--include-description" }, description = "Include description in class diagrams (default false)")
	  public boolean includeDescription = false;
	  
	  @Parameter(names = { "--highlight-required" }, description = "Highlight required properties and relationships (default false)")
	  public boolean highlightRequired = false;
	  
	  @Parameter(names = { "--include-orphan-enums" }, description = "Include all enums not linked to any specific resource")
	  public boolean includeOrphanEnums = false;
	  
	  @Parameter(names = { "--orphan-enum-config" }, description = "Include / show orphan enums for the list of identified resources")
	  public String orphanEnumConfig = null;

	  @Parameter(names = { "--floating-enums" }, description = "Floating enums - do not place enums close to the referring resource")
	  public boolean floatingEnums = false;
	    
	  @Parameter(names = { "--rules" }, description = "API rules file")
	  public String rulesFile = null;
	  
	  @Parameter(description = "Files")
	  public List<String> files = new ArrayList<>();
	  
	  
	  public String toString() {
		  String res="";
		  
		  res = res + "target=" + target + "\n";
		  res = res + "file=" + file + "\n";
		  res = res + "config=" + config + "\n";
		  res = res + "debug=" + debug + "\n";
		  res = res + "source=" + source + "\n";
		  res = res + "resource=" + resource + "\n";
		  res = res + "defaults=" + defaults + "\n";
		  res = res + "help=" + help + "\n";
		  res = res + "pumlComments=" + pumlComments + "\n";
		  res = res + "showAllCardinality=" + showAllCardinality + "\n";
		  res = res + "includeDescription=" + includeDescription + "\n";
		  res = res + "highlightRequired=" + highlightRequired + "\n";
		  res = res + "file=" + file + "\n";
		
		  return res;
		  
	  }
}
