package no.paneon.puml;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import no.paneon.common.Config;

public class ClassProperty extends Entity {

	   private static Config CONFIG = Config.getConfig();

		String name;
		String type;
		String cardinality;
		boolean required;
		
		public ClassProperty(String name, String type, String cardinality, boolean required) {
			super();
			this.name = name;
			this.type = type;
			this.cardinality = cardinality;
			this.required=required;
		}
		
		public ClassProperty(String name, String type) {
			super();
			this.name = name;
			this.type = type;
			this.cardinality = "";
		}
		
		public ClassProperty(String name, String value, boolean required) {
			super();
			this.name = name;
			this.type = value;
			this.cardinality = "";
			this.required=required;
		}

		public String getName() {
			return name;
		}

		public String toString() {
			String res="";
			String stype = type;
			int nlen = name.length()+3;
			List<String> lines = Utils.splitString(stype, CONFIG.getMaxLineLength()-nlen);
			if(lines.size()>1) {
				String indent = "                                       ".substring(nlen);
				stype = lines.get(0) + "\n";
				lines.remove(0);
				stype = stype + lines.stream()
						.map(p -> { return "{field} //" + indent + p + "//"; })
						.collect(Collectors.joining("\n")) + "\n";
			}
			if(name.length()==0)
				res=stype;
			else {
				if(required && CONFIG.getUseRequiredHighlighting()) {
					String format = CONFIG.getRequiredFormatting();
					String sname = String.format(format,name);
					res = sname + " : " + type;
				} else 
					res = name + " : " + type;
			}
			
			if(!cardinality.isEmpty() && !CONFIG.hideCardinaltyForProperty(cardinality)) res = res + " [" + cardinality + "]";
			
			return res;
		}
		
	}
			

