package ecore2text;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestElement {
	
	@JsonProperty
	private String ids;
	@JsonProperty
	private String owner;
	@JsonProperty
	private String target;
	@JsonProperty
	private String tree;

	public TestElement(String id, String owner, String target, String tree) {
		this.ids = id;
		this.owner = owner;
		this.target = target;
		this.tree = tree;
	}

}
