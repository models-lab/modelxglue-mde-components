package org.ml2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) throws IOException, ParseException {
        // logger
        BasicConfigurator.configure();

        // set up emf
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap( ).put("*", new XMIResourceFactoryImpl());

        // Options
        Options options = setUpOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        List<RecommendationItem> result = new ArrayList<>();
        
        String recommendationType = cmd.getOptionValue("recommendation-type");
        String root = cmd.getOptionValue("root");
        boolean fullContext = "full".equals(cmd.getOptionValue("context-type"));
        Map<String, ItemGenerator> supported = getSupportedItemGenerators(fullContext);
        if (supported.containsKey(recommendationType)) {
        	ObjectMapper objectMapper = new ObjectMapper();
        	
        	String file = root + File.separator + "X.json";
        	String transformFile = root + File.separator + "transformed.json";
        	
        	JsonNode rootNode = objectMapper.readTree(new File(file));
        	ArrayNode list = (ArrayNode) rootNode;
        	for (JsonNode jsonNode : list) {
        		String id = jsonNode.get("ids").textValue();
        		
        		Model m;
        		if (jsonNode.has("xmi_path")) {
        			String xmiPath = jsonNode.get("xmi_path").textValue();
        			m = Model.fromFile(id, xmiPath);
        		} else {
        			String xmi = jsonNode.get("xmi").textValue();
        			m = Model.fromContent(id, xmi);
        		}
        		
        		ItemGenerator itemGenerator = supported.get(recommendationType);
        		if (jsonNode.has("owner") && jsonNode.has("target")) {
        			// In this case we just want the context for the owner
        			String owner = jsonNode.get("owner").textValue();
        			String target = jsonNode.get("target").textValue();
        			RecommendationItem item = itemGenerator.generate(m, owner, target);
        			if (item != null) {
        				result.add(item);
        			} else {
        				System.out.println("Can't create item for " + m.getId() + " - " + owner);
        			}
        		} else {
        			List<RecommendationItem> items = itemGenerator.generate(m);
        			result.addAll(items);        			
        		}
        	}
        	

        	objectMapper.writer().writeValue(new FileWriter(transformFile), result);
        } else {
            throw new UnsupportedOperationException("No recommendation type: " + recommendationType);
        }
    }

    private static Options setUpOptions(){
        Options options = new Options();

        Option modelSetPath = new Option("t", "recommendation-type", true,
                "Recommendation type");
        options.addOption(modelSetPath);
        Option rootFolder = new Option("r", "root", true,
                "Root folder");
        Option contextType = new Option("c", "context-type", true,
                "Context type");
        options.addOption(modelSetPath);
        options.addOption(rootFolder);
        options.addOption(contextType);
        return options;
    }

    private static Map<String, ItemGenerator> getSupportedItemGenerators(boolean fullContext){
        Map<String, ItemGenerator> map = new HashMap<>();
        map.put("feature", new EcoreFeatureItemGenerator(fullContext));
        return map;
    }
}