package org.ml2;

import org.apache.commons.io.IOUtils;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import java.io.IOException;

public class Model {
	private Resource resource;
	private String id;
	
    public Model(String id, Resource resource) {
        this.id = id;
    	this.resource = resource;
    }

    public String getId() {
		return id;
	}
    
    public Resource getResource() {
        return resource;
    }
    
	public static Model fromContent(String id, String content) throws IOException {
		ResourceSet rs = new ResourceSetImpl();
        Resource resource = rs.createResource(URI.createURI("uri"));
        resource.load(IOUtils.toInputStream(content), null);
        return new Model(id, resource);
	}


	public static Model fromFile(String id, String xmiPath) {
        ResourceSet rs = new ResourceSetImpl();
        Resource resource = rs.getResource(URI.createFileURI(xmiPath), true);
        return new Model(id, resource);
	}

	public boolean isValid(int maxPackages, int minClasses, int maxClasses) {
		TreeIterator<EObject> it = resource.getAllContents();
		int numPackages = 0;
		int numClasses = 0;
		while (it.hasNext()) {
			EObject obj = it.next();
			if (obj instanceof EPackage) numPackages++;
			else if (obj instanceof EClass) numClasses++;
		
			if (numPackages > maxPackages || numClasses > maxClasses) 
				return false;
		}
		
		return numClasses >= minClasses;
	}

}
