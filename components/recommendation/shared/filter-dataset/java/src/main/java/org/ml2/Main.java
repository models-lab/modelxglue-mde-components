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

        int maxPackages = Integer.parseInt(cmd.getOptionValue("maxpkg", Integer.toString(Integer.MAX_VALUE)));
        int minClasses = Integer.parseInt(cmd.getOptionValue("minclasses", "0"));
        int maxClasses = Integer.parseInt(cmd.getOptionValue("maxclasses", Integer.toString(Integer.MAX_VALUE)));

        System.out.println("Filtering with: \n\t " + "maxpkg: " + maxPackages + "\n\t" + "minclasses: " + minClasses + "\n\t" + "maxclasses: " + maxClasses);
        
        String root = cmd.getOptionValue("root");
 
        ObjectMapper objectMapper = new ObjectMapper();
        	
    	String file = root + File.separator + "X.json";
    	String transformFile = root + File.separator + "transformed.json";
    	
    	JsonNode rootNode = objectMapper.readTree(new File(file));
    	
    	List<JsonNode> result = new ArrayList<>();
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
    		
    		if (m.isValid(maxPackages, minClasses, maxClasses)) {
    			result.add(jsonNode);
    		} else {
    			System.out.println("Filter out: " + id);
    		}
    		
		}
    	
    	objectMapper.writer().writeValue(new FileWriter(transformFile), result);
    
    }

    private static Options setUpOptions(){
        Options options = new Options();

        Option maxPackages = new Option("maxpkg", "maxpackages", true,
                "Maximum number of packages");
        maxPackages.setOptionalArg(true);
        options.addOption(maxPackages);
        
        Option minClasses = new Option("minclasses", "minclasses", true,
                "Min number of classes");
        minClasses.setOptionalArg(true);
        options.addOption(minClasses);
        
        Option maxClasses = new Option("maxclasses", "maxclasses", true,
                "Max number of classes");
        maxClasses.setOptionalArg(true);
        
        options.addOption(maxClasses);
        
        Option rootFolder = new Option("r", "root", true,
                "Root folder");
        options.addOption(rootFolder);
        return options;
    }

}