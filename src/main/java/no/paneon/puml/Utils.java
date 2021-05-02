package no.paneon.puml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import no.paneon.common.Config;

public class Utils {

	protected static Logger LOG = Logger.getLogger(Utils.class);
    private static Config CONFIG = Config.getConfig();

	public Utils() {
		// TODO Auto-generated constructor stub
	}

	public static String formatDescription(String s, String indent) {
		String res=s;
		int idx = s.indexOf(':');
		if(idx>0) {
			res = s.substring(idx+1);
			res = res.replaceFirst("[ ]+", "");
		}
		List<String> parts = splitString(res,getMaxLineLength());
		res = parts.stream()
				.map(p -> { return indent + "{field} //" + p + "//"; })
				.collect(Collectors.joining("\n")) + "\n";

		return res;
	}

    private static int getMaxLineLength() {
    	return CONFIG.getMaxLineLength();
	}
    
    public static List<String> splitString(String msg, int lineSize) {
        List<String> res = new ArrayList<>();

        Pattern p = Pattern.compile("\\b.{1," + (lineSize-1) + "}\\b\\W?");
        Matcher m = p.matcher(msg);
        
        while(m.find()) {
        	res.add(m.group());
        }
        return res;
    }

}
