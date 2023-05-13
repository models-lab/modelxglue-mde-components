package org.ml2;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationItem {

	@JsonProperty
	private final String ids;
	@JsonProperty
	private final String owner;
	@JsonProperty
    private final String target;
	@JsonProperty
	private final String context;
	
    public RecommendationItem(String ids, String owner, String target, List<String> context){
        this.ids = ids;
        this.owner = owner;
    	this.target = target;
        this.context = String.join(" ", context);
    }

    /*
    public String getTarget() {
        return target;
    }

    public Map<String, List<String>> getContext() {
        return context;
    }

    public void addContext(String featureName, String ... elements){
        if (context.containsKey(featureName)){
            context.get(featureName).addAll(Arrays.asList(elements));
        } else {
            context.put(featureName, Arrays.asList(elements));
        }
    }
	*/

    @JsonIgnore
    public boolean isValid(){
        return target != null && !context.isEmpty();
    }
}
