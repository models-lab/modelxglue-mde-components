package org.ml2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class EcoreFeatureItemGenerator extends ItemGenerator {

    private boolean fullContext;

	public EcoreFeatureItemGenerator(boolean fullContext) {
		this.fullContext = fullContext;
	}

	@Override
    public boolean isTarget(EObject obj) {
    	return obj instanceof EStructuralFeature && (((EStructuralFeature) obj).getEContainingClass() != null);
    }

    @Override
    public RecommendationItem generateItem(Model m, EObject obj) {
        EStructuralFeature f = (EStructuralFeature) obj;
        String target = f.getName();
        List<String> context = generateContext(f, f.getEContainingClass());
        
        RecommendationItem item = new RecommendationItem(m.getId(), f.getEContainingClass().getName(), target, context);
        return item;
    }

    protected List<String> generateContext(EStructuralFeature f, EClass c) {
    	// Do not add c.getName() because it is already in the context by being RecommendationItem owner
    	List<String> simpleContext = Arrays.asList(c.getEPackage().getName());
    	if (! fullContext) { 
    		return simpleContext;
    	}
    	
    	List<String> context = new ArrayList<>(simpleContext);
    	for (EStructuralFeature f2 : c.getEStructuralFeatures()) {
			if (f == f2)
				continue;
			context.add(f2.getName());
		}
    	return context;
    }
    
	@Override
	public RecommendationItem generate(Model m, String owner, String target) {
		TreeIterator<EObject> it = m.getResource().getAllContents();
		while (it.hasNext()) {
			EObject obj = it.next();
			if (obj instanceof EClass) {
				EClass c = (EClass) obj;
				if (c.getName().equals(owner)) {
					List<String> ctx = generateContext(null, c);
					return new RecommendationItem(m.getId(), owner, target, ctx);
				}
			}
		}
		return null;
	}
}
