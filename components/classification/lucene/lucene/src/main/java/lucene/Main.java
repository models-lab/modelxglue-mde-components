package lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;


public class Main {
	
	public static void main(String[] args) throws Exception {
		String root = args[0];
		String mode = args[1];
		String hyper = args[2];
		
		String indexDir = root + File.separator + "index";
		Model model = new Model(indexDir, getK(hyper));
		
		if (mode.equals("train")) {
			String file = root + File.separator + "X.json";
			List<String> paths = getPaths(root, file);
			List<String> labels = getLabels(root + File.separator + "y.json");
			System.out.println("Running training!");
			System.out.println("Models: " + paths.size());
			System.out.println("Labels: " + labels.size());
			model.train(paths, labels);
		} else if (mode.equals("test")) {
			String file = root + File.separator + "X.json";
			List<String> paths = getPaths(root, file);
			System.out.println("Running prediction!");
			System.out.println("Models: " + paths.size());
			saveOutput(model.predict(paths), root + File.separator + "y_pred.json");
			model.removeDirectory();
		} else {
			throw new UnsupportedOperationException();
		}
		
		
	}
	
	private static List<String> getPaths(String root, String file) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(new File(file));
		List<String> paths = new ArrayList<String>();
		ArrayNode list = (ArrayNode) rootNode;
		for (JsonNode jsonNode : list) {
			String xmiPath = jsonNode.get("xmi_path").textValue();
			xmiPath = root + File.separator + xmiPath;
			paths.add(xmiPath);
		}
		return paths;
	}
	
	private static List<String> getLabels(String file) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(new File(file));
		List<String> labels = new ArrayList<String>();
		ArrayNode list = (ArrayNode) rootNode;
		for (JsonNode jsonNode : list) {
			labels.add(jsonNode.asText());
		}
		return labels;
	}
	
	private static void saveOutput(List<String> output, String file) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer();
		writer.writeValue(new File(file), output);
	}
	
	private static int getK(String hyper) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(new File(hyper));
		return rootNode.get("hyper").get("k").asInt();
	}
	
}
