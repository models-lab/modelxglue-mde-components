package lucene;


import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.UpperCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;


public class Model {
	
	private final CustomAnalyzer analyzer;
	private final Directory directory;
	private final ResourceSet resourceSet;
	private final int k;
	private final String indexPath;
	
	public Model(String indexPath, int k) throws IOException {
		analyzer = CustomAnalyzer.builder()
	            .withTokenizer(StandardTokenizerFactory.class)
	            .addTokenFilter(EnglishPossessiveFilterFactory.class)
	            .addTokenFilter(UpperCaseFilterFactory.class)
	            .addTokenFilter(LowerCaseFilterFactory.class)
	            .addTokenFilter(StopFilterFactory.class)
	            .addTokenFilter(PorterStemFilterFactory.class).build();
		resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
                "ecore", new EcoreResourceFactoryImpl());
        this.directory = FSDirectory.open(Paths.get(indexPath));
        this.k = k;
        this.indexPath = indexPath;
	}
	
	public void removeDirectory() throws IOException {
		FileUtils.deleteDirectory(new File(indexPath));
	}
	
	public void train(List<String> models, List<String> labels) throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter iw = new IndexWriter(directory, iwc);
		for (int i=0; i<models.size();++i) {
			String label = labels.get(i);
			String model = models.get(i);
			Document document = generateDocument(parseEcore(new File(model)), label);
			iw.addDocument(document);
		}
		iw.commit();
		iw.close();
	}
	
	public List<String> predict(List<String> models) throws Exception {
		List<String> result = new ArrayList<String>();
		for (String model : models) {
			String querystr = getQuery(model);
			// System.out.println(querystr);
			QueryParser qp = new QueryParser("<default field>", analyzer);
            Query q = qp.parse(querystr);

            IndexReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            TopDocs docs = searcher.search(q, k);

            ArrayList<String> labels = new ArrayList<String>();
            for(ScoreDoc t:docs.scoreDocs){
                Document d = searcher.doc(t.doc);
                labels.add(d.getField("label").stringValue());

            }
            result.add(getMode(labels));
		}
		return result;
	}
	
	public static String getMode(List<String> list) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (String i : list) {
            counts.put(i, counts.getOrDefault(i, 0) + 1);
        }

        String mode = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mode = entry.getKey();
            }
        }

        return mode;
    }
	
	public String getQuery(String model) {
		StringBuilder querystr = new StringBuilder();
		Map<String, List<String>> ecoreInfo = parseEcore(new File(model));

        int securityCounter = 0; //lucene query cannot exceed 1024 elements

        for(String ecoreElement:ecoreInfo.keySet()){

            if(ecoreElement.equals("class")) {
                for(String clazz:ecoreInfo.get("class")){
                    if(securityCounter<1024&&clazz.length()>=1) {
                        querystr.append("class:").append(clazz).append("^2.5").append(" OR ");
                        securityCounter += 1;
                    }
                }
            }
            if(ecoreElement.equals("packages")){
                for(String pack:ecoreInfo.get("packages")){
                    if(securityCounter<1024&&pack!=null&&pack.length()>=1) {
                        querystr.append("packages:").append(pack).append("^1.0").append(" OR ");
                        securityCounter += 1;
                    }
                }
            }
            if(ecoreElement.equals("attributes")){
                for(String attrib:ecoreInfo.get("attributes")){
                    if(securityCounter<1024&&attrib.length()>=1) {
                        querystr.append("attributes:").append(attrib).append("^1.0").append(" OR ");
                        securityCounter += 1;
                    }
                }

            }
            if(ecoreElement.equals("references")){
                for(String ref:ecoreInfo.get("references")){
                    if(securityCounter<1024&&ref.length()>=1) {
                        querystr.append("references:").append(ref).append("^1.0").append(" OR ");
                        securityCounter += 1;
                    }
                }
            }
            if(ecoreElement.equals("dataType")){
                for(String dt:ecoreInfo.get("dataType")){
                    if(securityCounter<1024&&dt.length()>=1) {
                        querystr.append("dataType:").append(dt).append("^1.0").append(" OR ");
                        securityCounter += 1;
                    }
                }
            }
            if(ecoreElement.equals("eEnum")){
                for(String eenum:ecoreInfo.get("eEnum")){
                    if(securityCounter<1024&&eenum.length()>=1) {
                        querystr.append("eEnum:").append(eenum).append("^1.0").append(" OR ");
                        securityCounter += 1;
                    }
                }
            }

        }
        int lastOR = querystr.lastIndexOf("OR");
        if(querystr.length()>1){return querystr.replace(lastOR,lastOR+2,"").toString();}
        else{return null;}
	}
	
	private Document generateDocument(Map<String, List<String>> parsedEcore, String label) {
		Document doc = new Document();
		for (String key: parsedEcore.keySet()) {
			List<String> elements = parsedEcore.get(key);
			for (String element: elements) {
				Field eClass = new TextField(key, element, Field.Store.YES);
                doc.add(eClass);
			}
		}
		FieldType fieldType = new FieldType();
		fieldType.setStored(true);
		fieldType.setIndexOptions(IndexOptions.NONE);
		Field field = new Field("label", label, fieldType);
		doc.add(field);
		return doc;
	}
	
	private Map<String, List<String>> parseEcore(File ecoreFile){
		Resource myMetaModel = null;

        Map<String,List<String>> ecoreElements = new HashMap<String, List<String>>();

        try {
            myMetaModel = resourceSet.getResource(URI.createFileURI(String.valueOf(ecoreFile)), true);
        }
        catch(Exception e){
            System.out.println(e);
            System.out.println(ecoreFile);
        }

        if(myMetaModel!=null) {
            try {

                TreeIterator<EObject> eAllContents = myMetaModel.getAllContents();


                while (eAllContents.hasNext()) {
                    EObject next = eAllContents.next();
                    if (next instanceof EClass) {
                        EClass eClass = (EClass) next;
                        if(ecoreElements.get("class")!=null){
                        	List<String> classes = ecoreElements.get("class");
                            classes.addAll(Utils.splitString(eClass.getName()));
                            ecoreElements.put("class",classes);
                        }
                        else{
                        	List<String> classes = new ArrayList<String>();
                            classes.addAll(Utils.splitString(eClass.getName()));
                            ecoreElements.put("class",classes);
                        }
                    }
                    if(next instanceof EAttribute) {
                        EAttribute eAttribute = (EAttribute) next;
                        if (ecoreElements.get("attributes") != null) {
                        	List<String> attributes = ecoreElements.get("attributes");
                            attributes.addAll(Utils.splitString(eAttribute.getName()));
                            ecoreElements.put("attributes", attributes);
                        } else {
                        	List<String> attributes = new ArrayList<String>();
                            attributes.addAll(Utils.splitString(eAttribute.getName()));
                            ecoreElements.put("attributes", attributes);
                        }
                    }
                    if(next instanceof EReference){
                        EReference eReference = (EReference) next;
                        if(ecoreElements.get("references")!=null){
                        	List<String> references = ecoreElements.get("references");
                            references.addAll(Utils.splitString(eReference.getName()));
                            ecoreElements.put("references",references);
                        }
                        else{
                        	List<String> references = new ArrayList<String>();
                            references.addAll(Utils.splitString(eReference.getName()));
                            ecoreElements.put("references",references);
                        }
                    }
                    if(next instanceof EDataType){
                        EDataType eDataType = (EDataType) next;
                        if(ecoreElements.get("dataType")!=null){
                        	List<String> eDataTypes = ecoreElements.get("dataType");
                            eDataTypes.addAll(Utils.splitString(eDataType.getName()));
                            ecoreElements.put("dataType",eDataTypes);
                        }
                        else{
                        	List<String> eDataTypes = new ArrayList<String>();
                            eDataTypes.addAll(Utils.splitString(eDataType.getName()));
                            ecoreElements.put("dataType",eDataTypes);
                        }
                    }
                    if(next instanceof EPackage){
                        EPackage ePackage = (EPackage) next;
                        if(ecoreElements.get("packages")!=null){
                        	List<String> packages = ecoreElements.get("packages");
                            packages.addAll(Utils.splitString(ePackage.getName()));
                            ecoreElements.put("packages",packages);
                        }
                        else{
                        	List<String> packages = new ArrayList<String>();
                            packages.addAll(Utils.splitString(ePackage.getName()));
                            ecoreElements.put("packages",packages);
                        }
                    }
                    if(next instanceof EEnum){
                        EEnum eEnum = (EEnum) next;
                        if(ecoreElements.get("eEnum")!=null){
                        	List<String> eEnums = ecoreElements.get("eEnum");
                            eEnums.addAll(Utils.splitString(eEnum.getName()));
                            ecoreElements.put("eEnum",eEnums);
                        }
                        else{
                        	List<String> eEnums = new ArrayList<String>();
                            eEnums.addAll(Utils.splitString(eEnum.getName()));
                            ecoreElements.put("eEnum",eEnums);
                        }
                    }
                }
            }
            catch (Exception e) {
                System.out.println(e);
                System.out.println(ecoreFile);
            }

        }

        return ecoreElements;
	}
	
	

}
