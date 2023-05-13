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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Main {

    private static final Map<String, ModelGenerator> supported = getSupportedTestGenerators();
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

        String recommendationType = cmd.getOptionValue("recommendation-type");
        String root = cmd.getOptionValue("root");
        if (supported.containsKey(recommendationType)) {
        	ObjectMapper objectMapper = new ObjectMapper();
        	
        	String file = root + File.separator + "X.json";
        	String transformFile = root + File.separator + "transformed.json";
        	
        	JsonNode rootNode = objectMapper.readTree(new File(file));
        	
        	List<TestElement> result = new ArrayList<>();
        	ArrayNode list = (ArrayNode) rootNode;
        	for (JsonNode jsonNode : list) {
        		//ItemGenerator itemGenerator = supported.get(recommendationType);
        		ModelGenerator generator = new ModelGenerator();
        		
        		String id = jsonNode.get("ids").textValue();
        		
        		Model m;
        		if (jsonNode.has("xmi_path")) {
        			String xmiPath = jsonNode.get("xmi_path").textValue();
        			m = Model.fromFile(id, xmiPath);
        		} else {
        			String xmi = jsonNode.get("xmi").textValue();
        			m = Model.fromContent(id, xmi);
        		}
        		
        		List<TestElement> items = generator.generate(m);
        		result.addAll(items);
			}
        	
        	objectMapper.writer().writeValue(new FileWriter(transformFile), result);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static Options setUpOptions(){
        Options options = new Options();

        Option modelSetPath = new Option("t", "recommendation-type", true,
                "Recommendation type");
        options.addOption(modelSetPath);
        Option rootFolder = new Option("r", "root", true,
                "Root folder");
        options.addOption(rootFolder);
        return options;
    }

    private static Map<String, ModelGenerator> getSupportedTestGenerators(){
        Map<String, ModelGenerator> map = new HashMap<>();
        map.put("attribute", new ModelGenerator());
        return map;
    }
}