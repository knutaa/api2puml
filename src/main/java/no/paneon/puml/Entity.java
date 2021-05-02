package no.paneon.puml;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Entity extends Core {
	
	List<Comment> comments;
	
	Entity() {
		comments = new LinkedList<>();
	}
	
	public Entity addComment(Comment c) {
		comments.add(c);
		return this;
	}
	
	public String getCommentsBefore(int to) {
		String res="";
		for(Comment c : comments.stream().filter(c -> c.seq<=to).collect(Collectors.toList())) {
			res = res + c.comment + " (seq=" + c.seq + ")" + '\n';
		}
		return res;
	}
	
	public String getCommentsAfter(int to) {
		String res="";
		for(Comment c : comments.stream().filter(c -> c.seq>=to).collect(Collectors.toList())) {
			res = res + c.comment + " (seq=" + c.seq + ")" + '\n';
		}
		return res;
	}
	
	public String getComments(int from, int to) {
		String res="";
		for(Comment c : comments.stream().filter(c -> c.seq>=from && c.seq<=to).collect(Collectors.toList())) {
			res = res + c.comment + " (seq=" + c.seq + ")" + '\n';
		}
		return res;
	}
	
}
