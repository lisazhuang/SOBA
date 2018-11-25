package edu.eur.absa.model;

import java.util.HashMap;
import java.util.HashSet;

/**
 * To be implemented by anything that can have annotations. For instance, <code>Word</code>, <code>Span</code>, and 
 * <code>SpanRelation</code> all implement <code>Annotatable</code>.
 *  
 * @author Kim Schouten
 *
 */
public interface Annotatable extends Comparable<Annotatable> {
	
	
	public Annotations getAnnotations();

	public Dataset getDataset();
	
	public int getId();
	
	public Relations getRelations();
	
	
}
