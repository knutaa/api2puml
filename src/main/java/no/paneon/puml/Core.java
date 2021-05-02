package no.paneon.puml;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

public class Core {

	protected static Logger LOG = Logger.getLogger(Core.class);

	protected static String INDENT = "    ";
	protected static String BLANK =  "                                        ";

	static int sequenceNumber = 0;
			
	int seq;
	
	protected static List<Core> coreItems = new LinkedList<>();
	
	Core() {
		this.seq = (++sequenceNumber);
		coreItems.add(this);
	}
	
	public int getSeq() {
		return seq;
	}
	
	public static void reset() {
		coreItems.clear();
		sequenceNumber = 0;
	}
	
	public static Stream<Core> stream(int from) {
        LOG.debug("Core: stream starting from seq=" + from);
		return coreItems.stream().filter(n -> n.seq>from);
	}

}
