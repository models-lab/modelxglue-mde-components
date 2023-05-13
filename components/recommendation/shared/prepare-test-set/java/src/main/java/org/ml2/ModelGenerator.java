package org.ml2;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ModelGenerator {

    public List<TestElement> generate(Model model) throws IOException {
        List<TestElement> result = new ArrayList<>();
        
    	Resource r = model.getResource();
        TreeIterator<EObject> it = r.getAllContents();
        while (it.hasNext()) {
            EObject obj = it.next();
            if (isTarget(obj)) {
            	List<TestElement> elements = generateTestElement(obj, model);
            	result.addAll(elements);
            	/*
                RecommendationItem item = generateItem(obj);
                if (item != null && item.isValid())
                    items.add(generateItem(obj));
                    */
            }

        }
        return result;
    }

    private List<TestElement> generateTestElement(EObject obj, Model model) {
    	List<TestElement> elements = new ArrayList<>();
    	
    	EClass c = (EClass) obj;
    	System.out.println("Generating for " + model.getId() + " - " + c.getName());
    	
    	// This is very specific of Memorec
    	if (c.getEStructuralFeatures().size() == 1) {
    		System.out.println("Skipping because it only has one element");
    		return new ArrayList<>();
    	}
    	
    	int i = 0;
    	FEATURES:
    	for (EStructuralFeature f : new ArrayList<>(c.getEStructuralFeatures())) {
    		Collection<Setting> settings = EcoreUtil.UsageCrossReferencer.find(f, model.getResource());
    		for (Setting setting : settings) {
    			EStructuralFeature feature = setting.getEStructuralFeature();
    			if (feature.isDerived())
    				continue;
    			
    			i++;
				continue FEATURES;
			}
    		
    		/*
    		if (f instanceof EReference && ((EReference) f).getEOpposite() != null) {
				i++;
				continue;
			}
			*/
			
			System.out.println("  - " + f.getName());
			
			//EcoreUtil.delete(f);
    		c.getEStructuralFeatures().remove(i);
			
    		
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			try {
				model.getResource().save(stream, null);
			} catch (IOException e) {
				//e.printStackTrace();
				// This happens in repo-ecore-all/data/tue-mdse/ocl-dataset/dataset/repos/damenac/puzzle/plugins/fr.inria.diverse.puzzle.metrics/testdata/VisualInterface.ecore
				// because there is an eKeys pointing to the feature...
				//return new ArrayList<>();
				throw new RuntimeException(e);
			}
    		
			String xmi = stream.toString();
			elements.add(new TestElement(model.getId(), xmi, c.getName(), Collections.singletonList(f.getName())) );
			
			c.getEStructuralFeatures().add(i, f);			
    		i++;
		}
    	
		return elements;
	}

	public boolean isTarget(EObject obj) {
    	return obj instanceof EClass;
    }

}
