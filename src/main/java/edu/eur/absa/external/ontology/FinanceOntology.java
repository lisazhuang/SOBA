package edu.eur.absa.external.ontology;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;

public class FinanceOntology implements IOntology {

	public final String NS = "http://www.kimschouten.com/finance/"; 
	public final String URI_ActionMention = NS+"ActionMention";
	public final String URI_EntityMention = NS+"#EntityMention";
	public final String URI_PropertyMention = NS+"PropertyMention";
	public final String URI_NamedEntityMention = NS+"NamedEntityMention";
	public final String URI_Statement = NS+"#Statement";
	public final String URI_Positive = NS+"#Positive";
	public final String URI_Negative = NS+"#Negative";
	public final String URI_Decrease = NS+"#Decrease";
	public final String URI_Increase = NS+"#Increase";
	public final String URI_NegativeEntityMention = NS+"#NegativeEntityMention";
	public final String URI_PositiveEntityMention = NS+"#PositiveEntityMention";
	public final String URI_TransitiveMention = NS+"TransitiveChangeMention";
	public final String URI_Mention = NS+"Mention";
	
	
	
	private OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
	
	
	
	public FinanceOntology(String ontologyFile){

		// use the FileManager to find the input file
		 InputStream in = FileManager.get().open( ontologyFile );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + ontologyFile + " not found");
		}

		
		// read the RDF/XML file
		ontology.read(in, null);
	}
	
	public String addIndividual(String lemma, String classURI, String... additionalClasses){
		Individual indiv = ontology.createIndividual(NS+"I"+lemma, ontology.getResource(classURI));
		for (String addClass :additionalClasses){
			indiv.addOntClass(ontology.getResource(addClass));
		}
		return indiv.getURI();
	}
	
	
	
	public void addProperty(String individualURI, String propertyURI, String objectURI){
		ontology.getIndividual(individualURI).addProperty(ontology.getProperty(propertyURI), ontology.getResource(objectURI));
	}
	
	public HashSet<String> getLexicalizedConcepts(String superclassURI, String lemma){
		Literal literal;
		if (lemma == null){
			literal = null;
		} else {
			literal = ontology.createLiteral(lemma);
		}
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null, ontology.getProperty(NS+"lex"),literal));
		
		
		HashSet<String> ontoConcepts=new HashSet<String>();
//		System.out.println(literal);
		while (iter.hasNext()) {
			
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();     // get the subject
			StmtIterator iter2 = ontology.listStatements(new SimpleSelector(
					subject, 
					ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
					ontology.getResource(superclassURI)));
			if (iter2.hasNext())
				ontoConcepts.add(subject.getURI());
			
//			System.out.println(subject.toString());
		}
		return ontoConcepts;
	}
	
	/**
	 * Get (the) one subclass of ActionMention with this lemma. If there are more, just one of them is returned.
	 * @param lemma
	 * @return
	 */
	public String getLexicalizedAction(String lemma){
		HashSet<String> res = getLexicalizedConcepts(this.URI_ActionMention,lemma);
		if (res.isEmpty()){
			return null;
		} else {
			return res.iterator().next();	
		}
		
	}
	/**
	 * Get (the) one subclass of EntityMention with this lemma. If there are more, just one of them is returned.
	 * @param lemma
	 * @return
	 */
	public String getLexicalizedEntity(String lemma){
		HashSet<String> res = getLexicalizedConcepts(this.URI_EntityMention,lemma);
		if (res.isEmpty()){
			return null;
		} else {
			return res.iterator().next();	
		}
	}
	
	/**
	 * Get (the) one subclass of EntityMention with this lemma. If there are more, just one of them is returned.
	 * @param lemma
	 * @return
	 */
	public String getLexicalizedProperty(String lemma){
		HashSet<String> res = getLexicalizedConcepts(this.URI_PropertyMention,lemma);
		if (res.isEmpty()){
			return null;
		} else {
			return res.iterator().next();	
		}
	}
	
	public HashSet<String> test(String literal){
		
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null, ontology.getProperty(NS+"lex"),ontology.createLiteral(literal)));
		
		
		HashSet<String> ontoConcepts=new HashSet<String>();
//		System.out.println(literal);
		while (iter.hasNext()) {
			
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();     // get the subject
			StmtIterator iter2 = ontology.listStatements(new SimpleSelector(
					subject, 
					ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
					ontology.getResource(NS+"ActionMention")));
			if (iter2.hasNext())
				ontoConcepts.add(subject.getURI());
			
//			System.out.println(subject.toString());
		}
		return ontoConcepts;
	}
	
	public HashMap<String, HashSet<String>> getConceptRelations(String conceptURI){
		HashMap<String, HashSet<String>> relations = new HashMap<>();
		
		Resource concept = ontology.getResource(conceptURI);
		if (concept == null){
			return relations;
		}
		// get all statements where the given concept is the subject
		StmtIterator iter = ontology.listStatements(new SimpleSelector(concept,	null,(RDFNode)null));
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
			
		    if (!relations.containsKey(predicate.toString())){
		    	relations.put(predicate.toString(), new HashSet<String>());
		    }
		    relations.get(predicate.toString()).add(object.toString());		    
		}
		
		// get all statements where the given concept is the object
				iter = ontology.listStatements(new SimpleSelector(null,	null,(RDFNode)concept));
				while (iter.hasNext()) {
				    Statement stmt      = iter.nextStatement();  // get next statement
				    Resource  subject   = stmt.getSubject();     // get the subject
				    Property  predicate = stmt.getPredicate();   // get the predicate
				    RDFNode   object    = stmt.getObject();      // get the object
					
				
				    if (!relations.containsKey("inverse::"+predicate.toString())){
				    	relations.put("inverse::"+predicate.toString(), new HashSet<String>());
				    }
				    relations.get("inverse::"+predicate.toString()).add(subject.toString());		    
				}
		
		return relations;
	}
	
	/**
	 * Get all (inferred) superclasses of a target lexicalization
	 * @param targetURI
	 * @return
	 */
	public HashSet<String> getSuperclasses(String classURI){
		return getObjects(classURI, "http://www.w3.org/2000/01/rdf-schema#subClassOf");
	}
	
	public HashSet<String> getClasses(String indivURI){
		return getObjects(indivURI, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}	
	public HashSet<String> getObjects(String subjectURI, String predicateURI){
		StmtIterator iter = ontology.listStatements(new SimpleSelector(ontology.getResource(subjectURI), ontology.getProperty(predicateURI),(RDFNode)null));
		HashSet<String> targetTypes = new HashSet<String>();
		while (iter.hasNext()) {
			Statement stmt      = iter.nextStatement();  // get next statement
			RDFNode object    = stmt.getObject();      // get the object
			targetTypes.add(object.asResource().getURI());
		}
		targetTypes.remove(null);
		return targetTypes;
	}
	
	public HashMap<String, String> lexToURI(){
		StmtIterator iter = ontology.listStatements(new SimpleSelector(null, ontology.getProperty(NS+"lex"),(Literal)null));
		
		
		HashMap<String, String> ontoConcepts=new HashMap<String,String>();
//		System.out.println(literal);
		while (iter.hasNext()) {
			
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();     // get the subject
			RDFNode lex = stmt.getObject();
			StmtIterator iter2 = ontology.listStatements(new SimpleSelector(
					subject, 
					ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
					ontology.getResource(this.URI_NamedEntityMention)));
			if (iter2.hasNext())
				ontoConcepts.put(lex.toString(), subject.getURI());
			
//			System.out.println(subject.toString());
		}
		return ontoConcepts;
	}
	
}
