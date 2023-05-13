package org.ml2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

public abstract class ItemGenerator {

    public List<RecommendationItem> generate(Model model) throws IOException {
        Resource r = model.getResource();
        TreeIterator<EObject> it = r.getAllContents();
        List<RecommendationItem> items = new ArrayList<RecommendationItem>();
        while (it.hasNext()) {
            EObject obj = it.next();
            if (isTarget(obj)){
                RecommendationItem item = generateItem(model, obj);
                if (item != null && item.isValid())
                    items.add(item);
            }

        }
        return items;
    }

    /**
     * This is to complete an existing item with its context.
     */
    public abstract RecommendationItem generate(Model m, String owner, String target);

    public abstract boolean isTarget(EObject obj);

    public abstract RecommendationItem generateItem(Model model, EObject obj);



}
