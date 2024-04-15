package ecore2text;

import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TreeTransformation {
	
	public static void main(String[] args) throws IOException {
		String root = args[0];
		String file = root + File.separator + "X.json";
		String transformFile = root + File.separator + "transformed.json";
		
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(new File(file));
		
		List<TestElement> result = new ArrayList<>();
		ArrayNode list = (ArrayNode) rootNode;
		for (JsonNode jsonNode : list) {
			String xmiPath = jsonNode.get("xmi_path").textValue();
			xmiPath = root + File.separator + xmiPath;
			try {
				String id = jsonNode.get("ids").textValue();
				System.out.println("Generating for id " + id + " at " + xmiPath);
				String jsonResult = new EcoreToTextTransformation().generate(xmiPath);
				result.add(new TestElement(id,
						jsonNode.get("owner").textValue(), 
						jsonNode.get("target").textValue(), jsonResult));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		objectMapper.writer().writeValue(new FileWriter(transformFile), result);
	}
	
}
