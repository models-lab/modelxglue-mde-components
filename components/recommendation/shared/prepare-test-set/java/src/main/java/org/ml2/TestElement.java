package org.ml2;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestElement {
	
	@JsonProperty
	private String ids;
	@JsonProperty
	private String xmi;
	@JsonProperty
	private String owner;
	@JsonProperty
	private String target;

	public TestElement(String id, String xmi, String owner, List<String> target) {
		this.ids = id;
		this.xmi = xmi;
		this.owner = owner;
		this.target = String.join(",", target);
	}

}
