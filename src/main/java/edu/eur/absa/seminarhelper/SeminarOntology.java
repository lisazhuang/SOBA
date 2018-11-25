package edu.eur.absa.seminarhelper;

import java.io.File;
import edu.eur.absa.external.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.jena.*;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import edu.eur.absa.seminarhelper.WordSenseDisambiguation;
import edu.smu.tspell.wordnet.Synset;
import edu.eur.absa.Framework;
import edu.eur.absa.external.ontology.IOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

/**
 * ReasoningOntology (@author Kim Schouten)  adapted to work with a different kind of ontology.
 * 
 * @author Karoliina Ranta
 *
 */
public class SeminarOntology implements IOntology {

	public final String NS = "http://www.semanticweb.org/lisa.zhuang/ontologies/2018/5/RestaurantOntologyBase";
	//public final String NS = "http://www.semanticweb.org/lisa.zhuang/ontologies/2018/5/LaptopOntologyBase";
	//public final String URI_AspectMention = NS + "#AspectMention";
	//public final String URI_SentimentMention = NS + "#SentimentMention";
	public final String URI_Mention= NS +"#Mention";
	public final String URI_Sentiment = NS + "#Sentiment";
	//public final String URI_ContextDependent = NS + "#ContextDependentSentimentMention";
	//public final String URI_GenericPositive = NS + "#GenericPositiveSentiment";
	//public final String URI_GenericNegative = NS + "#GenericNegativeSentiment";
	public final String URI_EntityMention = NS +"#EntityMention";
	public final String URI_PropertyMention = NS + "#PropertyMention";
	public final String URI_ActionMention = NS + "#ActionMention";
	public final String URI_Positive = NS + "#Positive";
	public final String URI_Negative = NS + "#Negative";
	public final String URI_GenericPositiveAction = NS +"#GenericPositiveAction";
	public final String URI_GenericNegativeAction = NS +"#GenericNegativeAction";
	public final String URI_GenericPositiveProperty = NS + "#GenericPositiveProperty";
	public final String URI_GenericNegativeProperty = NS + "#GenericNegativeProperty";
	public final String URI_GenericPositiveEntity = NS + "#GenericPositiveEntity";
	public final String URI_GenericNegativeEntity = NS + "#GenericNegativeEntity";
	

	private OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);

	private HashMap<String, HashSet<String>> superclasses = new HashMap<>();

	/**
	 * A constructor for the ontology of seminar 2017 B.
	 * 
	 * @param ontologyFile, the .owl file containing the (basic) ontology
	 */
	public SeminarOntology(String ontologyFile) {
		/* Use the FileManager to find the input file */
		InputStream in = FileManager.get().open(ontologyFile);
		if (in == null) {
			throw new IllegalArgumentException("File: " + ontologyFile + " not found");
		}
		/* Read the RDF/XML file */
		ontology.read(in, null);
	}

	/**
	 * A method that saves the ontology to the ontology .owl file.
	 * 
	 * @param ontologyFile, String of the name of the .owl file
	 */
	public void save(String ontologyFile) {
		try {
			ontology.write(new FileOutputStream(new File(Framework.EXTERNALDATA_PATH + ontologyFile)), "RDF/XML", null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A method that adds an individual (word) to the ontology.
	 * 
	 * @param lemma, the lemmatized String of the individual being added
	 * @param classURI, the URI of the class to which the individual is being added
	 * @param additionalClasses, other classes to which one wants to add the individual
	 * @return the URI of the newly added individual
	 */
	public String addIndividual(String lemma, String classURI, String... additionalClasses) {
		Individual indiv = ontology.createIndividual(NS + "I" + lemma, ontology.getResource(classURI));
		for (String addClass : additionalClasses) {
			System.out.println(addClass + "\t" + ontology.getOntClass(addClass));
			indiv.addOntClass(ontology.getOntClass(addClass));
		}
		return indiv.getURI();
	}

	/**
	 * A method that adds a new class to the ontology. (Warning, can only add one 
	 * 
	 * @param lemmaURI, the URI of the new class
	 * @param lex, true if the lex property should be set
	 * @param lexName, the lexicalization to add
	 * @param, the string to set for the lex property
	 * @param classURIs, the direct superclasses of the new class
	 * @return The URI of the new class
	 */
	public String addClass(String lemmaURI, boolean lex, String lexName, String... classURIs) {
		return this.addClass(lemmaURI, lex, lexName, new HashSet<String>(), classURIs);
		//		OLD CODE WITHOUT ASPECT CATEGORIES
		//		String URI = NS + "#" + lemmaURI.replaceAll(" ", "");
		//			OntClass newClass = ontology.createClass(URI);
		//			newClass.addProperty(ontology.getProperty(NS + "#lex"), lemmaURI.toLowerCase());
		//			for (String classURI : classURIs) {
		//				newClass.addSuperClass(ontology.getResource(classURI));
		//			}
		//			return newClass.getURI();
	}

	/**
	 * A method that adds a new class to the ontology.
	 * 
	 * @param lemmaURI, the URI of the new class
	 * @param lex, true if the lex property should be set
	 * @param lexName, the lexicalization to add
	 * @param aspectProperties, the aspect categories to link to the new class through the aspect property
	 * @param classURIs, the direct superclasses of the new class
	 * @return The URI of the new class
	 */
	public String addClass(String lemmaURI, boolean lex, String lexName, HashSet<String> aspectProperties, String... classURIs ) {
		String URI = NS + "#" + lemmaURI.replaceAll(" ", "");
		OntClass newClass = ontology.createClass(URI);

		/* Add the lex property. */
		if (lex) {
			newClass.addProperty(ontology.getProperty(NS + "#lex"), lexName.toLowerCase());

		}
		//TODO: comment away this method and see if everything still running


		/* Add the given aspect properties. */
		for (String aspectCategory : aspectProperties) {
			newClass.addProperty(ontology.getProperty(NS + "#aspect"), aspectCategory.toUpperCase());
		}
		for (String classURI : classURIs) {
			newClass.addSuperClass(ontology.getResource(classURI));
		}

		return newClass.getURI();
	}
	
	/**
	 * A method that adds a new class to the ontology.
	 * 
	 * @param lemmaURI, the URI of the new class
	 * @param lex, true if the lex property should be set
	 * @param lexName, the lexicalization to add
	 * @param aspectProperties, the aspect categories to link to the new class through the aspect property
	 * @param classURIs, the direct superclasses of the new class
	 * @return The URI of the new class
	 */
	public String addClass(String synset, String lemmaURI, boolean lex, String lexName, HashSet<String> aspectProperties, String... classURIs ) {
		String URI = NS + "#" + lemmaURI.replaceAll(" ", "");
		OntClass newClass = ontology.createClass(URI);

		/* Add the lex property. */
		if (lex) {
			newClass.addProperty(ontology.getProperty(NS + "#lex"), lexName.toLowerCase());

		}

		//add synset property
		newClass.addProperty(ontology.getProperty(NS + "#sense"), synset);
		
		/* Add the given aspect properties. */
		for (String aspectCategory : aspectProperties) {
			newClass.addProperty(ontology.getProperty(NS + "#aspect"), aspectCategory.toUpperCase());
		}
		for (String classURI : classURIs) {
			newClass.addSuperClass(ontology.getResource(classURI));
		}

		return newClass.getURI();
	}

	public String addClass(String pos, HashMap<String, String> context, String lemmaURI, boolean lex, String lexName, HashSet<String> aspectProperties, String... classURIs) { //pos = "noun" or "verb"
		String URI = NS + "#" + lemmaURI.replaceAll(" ", "");
		OntClass newClass = ontology.createClass(URI);

		/* Add the lex property. */
		if (lex) {
			newClass.addProperty(ontology.getProperty(NS + "#lex"), lexName.toLowerCase());
		}

		// find synonyms and add via lex annotation property

		String[] contextWords = WordSenseDisambiguation.wordsToArray(context.get(lexName));
		Synset synset = WordSenseDisambiguation.findSynset(contextWords, lexName, pos);
		String[] words = synset.getWordForms();
		for (int i = 0; i < words.length; i++)
		{
			newClass.addProperty(ontology.getProperty(NS + "#lex"), words[i].toLowerCase());
		}

		//add synset property
		int sense = WordSenseDisambiguation.Sense(contextWords, lexName, pos);
				newClass.addProperty(ontology.getProperty(NS + "#sense"), lexName+"#"+pos+"#"+sense);
		
		/* Add the given aspect properties. */
		for (String aspectCategory : aspectProperties) {
			newClass.addProperty(ontology.getProperty(NS + "#aspect"), aspectCategory.toUpperCase());
		}
		for (String classURI : classURIs) {
			newClass.addSuperClass(ontology.getResource(classURI));
		}

		return newClass.getURI();
	}

	/**
	 * A method that adds a new intersection class of two classes to the ontology.
	 * 
	 * @param lemmaURISub1, the URI of the first class, which should already exist
	 * @param lemmaURISub2, the URI of the second class, which should already exist
	 * @param lex, true if the lex property should be set
	 * @param lexName, the lexicalization to add
	 * @param aspectProperties, the aspect categories to link to the new class through the aspect property
	 * @param IC The classes that comprise the intersection
	 * @param classURI, the (single) direct superclass of the new class
	 * @return The URI of the new class
	 */
	public String addClass2(String lemmaURISub1, String lemmaURISub2, String classURI ) {

		RDFNode[] elements = new RDFNode[2];
		elements[0] = ontology.getResource(lemmaURISub1);
		elements[1] = ontology.getResource(lemmaURISub2);


		RDFList list = ontology.createList(elements);

		OntClass newClass = ontology.createIntersectionClass(null, list);


		newClass.addSuperClass(ontology.getResource(classURI));

		return newClass.getURI();
	}

	/**
	 * A method that adds lexicalizations to a class.
	 * @param classURI, the URI of the class to which to add lexicalizations
	 * @param lex, the lexicalizations to add to the class
	 */
	public void addLexicalizations(String classURI, HashSet<String> lex) { //classURI: the class to which the terms are to be added, lex: the accepted terms to add
		Resource concept = ontology.getResource(classURI);
		for (String lexical : lex) {
			concept.addProperty(ontology.getProperty(NS + "#lex"), lexical.toLowerCase());
		}
	}

	/**
	 * A method that finds all subclasses of the given superclass with the given
	 * object for the lex property.
	 * 
	 * @param superclassURI, the URI of the superclass
	 * @param lemma, the necessary object
	 * @return a HashSet of the URI's of the classes
	 */
	public HashSet<String> getLexicalizedConcepts(String superclassURI, String lemma) {
		return getLexicalizedConcepts(superclassURI, NS + "#lex", lemma);
	}

	/**
	 * A method that finds all subclasses of the given superclass with the given
	 * object for the given property.
	 * 
	 * @param superclassURI, the URI of the superclass
	 * @param annotationType, the necessary property
	 * @param lemma, the necessary object
	 * @return a HashSet of the URI's of the classes
	 */
	public HashSet<String> getLexicalizedConcepts(String superclassURI, String annotationType, String lemma) {
		/* Create a literal of the lemma. */
		Literal literal;
		if (lemma == null) {
			literal = null;
		} else {
			literal = ontology.createLiteral(lemma);
		}
		/* Get all statements with the given property and object. */
		StmtIterator iter = ontology
				.listStatements(new SimpleSelector(null, ontology.getProperty(annotationType), literal));

		/* Retain only the classes that are subclasses of the superclass. */
		HashSet<String> ontoConcepts = new HashSet<String>();

		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource subject = stmt.getSubject();
			/* If superclass is equal to null the superclass is owl:thing */
			if (superclassURI == null) {
				ontoConcepts.add(subject.getURI());
			} else {
				StmtIterator iter2 = ontology.listStatements(
						new SimpleSelector(subject, ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
								ontology.getResource(superclassURI)));
				if (iter2.hasNext()) {
					ontoConcepts.add(subject.getURI());
				}
			}
		}
		return ontoConcepts;
	}

	/**
	 * Get (the) one subclass of ActionMention with this lemma. If there are
	 * more, just one of them is returned.
	 * 
	 * @param lemma
	 * @return the URI of the action, null is empty
	 */
	/*public String getLexicalizedAction(String lemma) {
		HashSet<String> res = getLexicalizedConcepts(this.URI_ActionMention, lemma);
		if (res.isEmpty()) {
			return null;
		} else {
			return res.iterator().next();
		}
	}*/

	/**
	 * Get (the) one subclass of EntityMention with this lemma. If there are
	 * more, just one of them is returned.
	 * 
	 * @param lemma
	 * @return the URI of the entity, null is empty
	 */
	public String getLexicalizedEntity(String lemma) {
		HashSet<String> res = getLexicalizedConcepts(this.URI_EntityMention, lemma);
		if (res.isEmpty()) {
			return null;
		} else {
			return res.iterator().next();
		}
	}

	/**
	 * Get (the) one subclass of SentimentMention with this lemma. If there are
	 * more, just one of them is returned.
	 * 
	 * @param lemma
	 * @return the URI of the property, null is empty
	 */
	public String getLexicalizedSentiment(String lemma) {
		HashSet<String> res = getLexicalizedConcepts(this.URI_Sentiment, lemma);
		if (res.isEmpty()) {
			return null;
		} else {
			return res.iterator().next();
		}
	}

	/**
	 * Gets all relations related to the concept.
	 * @param conceptURI, the URI of the concept
	 * @return a HashMap of the relations in which the concept is the subject or the object.
	 */
	public HashMap<String, HashSet<String>> getConceptRelations(String conceptURI) {
		HashMap<String, HashSet<String>> relations = new HashMap<>();

		Resource concept = ontology.getResource(conceptURI);
		if (concept == null) {
			return relations;
		}
		/* Get all statements where the given concept is the subject */
		StmtIterator iter = ontology.listStatements(new SimpleSelector(concept, null, (RDFNode) null));
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement(); 
			Resource subject = stmt.getSubject(); 
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();
			/* Add relations that are not yet in the set */
			if (!relations.containsKey(predicate.toString())) {
				relations.put(predicate.toString(), new HashSet<String>());
			}
			relations.get(predicate.toString()).add(object.toString());
		}

		/* Get all statements where the given concept is the object */
		iter = ontology.listStatements(new SimpleSelector(null, null, (RDFNode) concept));
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource subject = stmt.getSubject(); 
			Property predicate = stmt.getPredicate(); 
			RDFNode object = stmt.getObject(); 

			if (!relations.containsKey("inverse::" + predicate.toString())) {
				relations.put("inverse::" + predicate.toString(), new HashSet<String>());
			}
			relations.get("inverse::" + predicate.toString()).add(subject.toString());
		}

		return relations;
	}

	/**
	 * Get all (inferred) superclasses of a target lexicalization
	 * 
	 * @param classURI, the URI of the inputted class
	 * @return a HashSet containing the URI's of the inputted class's superclasses
	 */
	public HashSet<String> getSuperclasses(String classURI) {
		if (!superclasses.containsKey(classURI)) {
			superclasses.put(classURI, new HashSet<>());
			superclasses.get(classURI).addAll(getObjects(classURI, "http://www.w3.org/2000/01/rdf-schema#subClassOf"));
		}
		return superclasses.get(classURI);
	}

	/**
	 * Get all classes to which an individual belongs.
	 * @param indivURI, the URI of the individual
	 * @return a HashSet of the URIs of the classes to which the individual belongs
	 */
	public HashSet<String> getClasses(String indivURI) {
		return getObjects(indivURI, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}

	/**
	 * Get the subclasses of the class.
	 * @param classURI, the URI of the class
	 * @return a HashSet containing all subclasses of the input.
	 */
	public HashSet<String> getSubclasses(String classURI) {
		return getSubjects(classURI, "http://www.w3.org/2000/01/rdf-schema#subClassOf");
	}

	/**
	 * Find all classes with the given predicate and subject.
	 * @param subjectURI, the URI of the subject the class must have
	 * @param predicateURI, the URI of the predicate the class must have
	 * @return a HashSet of the object URI's
	 */
	public HashSet<String> getObjects(String subjectURI, String predicateURI) {
		/* get all statements with the given predicate and subject */
		StmtIterator iter = ontology.listStatements(new SimpleSelector(ontology.getResource(subjectURI),
				ontology.getProperty(predicateURI), (RDFNode) null));
		HashSet<String> targetTypes = new HashSet<String>();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			RDFNode object = stmt.getObject();
			if (object.isResource()) {
				targetTypes.add(object.asResource().getURI());
			} else if (object.isLiteral()) {
				targetTypes.add(object.asLiteral().getLexicalForm());
			}
		}
		targetTypes.remove(null);
		return targetTypes;
	}

	/**
	 * Find all classes with the given predicate and object.
	 * @param objectURI, the URI of the object the class must have
	 * @param predicateURI, the URI of the predicate the class must have
	 * @return a HashSet of the subject URI's
	 */
	public HashSet<String> getSubjects(String objectURI, String predicateURI) {
		/* find all statements with the given predicate and object */
		StmtIterator iter = ontology.listStatements(
				new SimpleSelector(null, ontology.getProperty(predicateURI), ontology.getResource(objectURI)));
		HashSet<String> targetTypes = new HashSet<String>();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource subject = stmt.getSubject();
			targetTypes.add(subject.asResource().getURI());
		}
		targetTypes.remove(null);
		return targetTypes;
	}

	/**
	 * A method that finds all classes with the lex property.
	 * 
	 * @return a HashMap containing the lex and the URI of the class.
	 */
	public HashMap<String, String> lexToURI() {
		/* Find all classes with the lex property */
		StmtIterator iter = ontology
				.listStatements(new SimpleSelector(null, ontology.getProperty(NS + "#lex"), (Literal) null));

		HashMap<String, String> ontoConcepts = new HashMap<String, String>();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource subject = stmt.getSubject();
			RDFNode lex = stmt.getObject();
			//			StmtIterator iter2 = ontology.listStatements(
			//					new SimpleSelector(subject, ontology.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
			//							ontology.getResource(this.URI)));
			//			if (iter2.hasNext())
			ontoConcepts.put(lex.toString(), subject.getURI());
		}
		return ontoConcepts;
	}

	/**
	 * Gets the lexicalizations of a resource.
	 * @param uri, the URI of the resource of which you want the lexicalization.
	 * @return a HashSet containing the lexicalizations
	 */
	public HashSet<String> getLexicalizations(String uri) {
		return getObjects(uri, NS + "#lex");
	}

	/**
	 * Finds the namespace of the ontology.
	 * @return the name space
	 */
	public String getNS() {
		return NS;
	}

	/**
	 * Return concepts within the sentence with multiple word lexicalizations.
	 * @param sentenceText, the sentence in which to search
	 * @param superclassURI, the superclass of the classes in which to search
	 * @return a HashMap of the lexicalizations and the URI's of the classes
	 */
	public HashMap<String, String> getMultiWordConcepts(String sentenceText, String superclassURI) {
		HashSet<String> foundClasses = getSubclasses(superclassURI);
		HashMap<String, String> foundTargets = new HashMap<>();
		for (String uri : foundClasses) {
			for (String lex : getLexicalizations(uri)) {
				if (lex.contains(" ") && sentenceText.contains(lex)) {
					/* Found a multiword concept */
					foundTargets.put(lex, uri);
				}
			}
		}
		return foundTargets;
	}
}