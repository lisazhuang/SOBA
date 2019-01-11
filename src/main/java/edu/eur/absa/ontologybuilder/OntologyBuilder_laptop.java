package edu.eur.absa.ontologybuilder;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.json.JSONException;
import org.json.JSONObject;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.impl.file.Morphology;
import edu.eur.absa.Framework;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.seminarhelper.Synonyms;
import edu.eur.absa.seminarhelper.TermFrequencyYelp;
import edu.eur.absa.seminarhelper.SeminarOntology;
import edu.eur.absa.seminarhelper.WordSenseDisambiguation;
import edu.eur.absa.seminarhelper.Wu_Palmer;
import edu.eur.absa.seminarhelper.readJSON;
import edu.eur.absa.nlp.*;
import edu.eur.absa.Framework;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase.*;
import edu.cmu.lti.ws4j.*;

/**
 * A method that builds an ontology semi-automatically.
 * 
 * @author Karoliina Ranta
 * Adapted by Suzanne Veltman
 * Adapted by Lisa Zhuang
 */
public class OntologyBuilder_laptop {

	/* The base ontology. */
	private SeminarOntology base;
	private HashMap<String, HashSet<String>> aspectCategories;
	private String domain;
	private Dataset reviewData;
	private Dataset reviewData1;
	private JSONObject wordFrequencyReview;
	private JSONObject wordFrequencyDocument;
	private HashMap<String, HashMap<String, Integer>> contrastData;
	private int numRejectTerms;   //only words
	private int numAcceptTerms;
	private int numRejectOverall;  //words + parent-relations
	private int numAcceptOverall;
	private int numRev;
	private HashSet<String> remove;
	private double threshold;
	private double invThreshold;
	private double[] fraction;
	private HashMap<String, HashSet<String>> relatedNouns;
	private boolean relations;
	private HashMap<String, HashSet<String>> nounsWithSynset;
	private HashSet<String> synonymsAccepted;

	/**
	 * A constructor for the OntologyBuilder class.
	 * @param baseOnt, the base ontology from which the final ontology is further constructed
	 * @param aspectCat, the aspect categories of the domain
	 * @param dom, the domain name
	 * @param thres, the threshold to use for the subsumption method
	 * @param frac, the top fraction of terms to suggest
	 */
	public OntologyBuilder_laptop(SeminarOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, double thres, double[] frac, boolean r) {
		this(baseOnt, aspectCat, dom, thres, thres, frac, r );
	}

	/**
	 * A constructor for the OntologyBuilder class.
	 * @param baseOnt, the base ontology from which the final ontology is further constructed
	 * @param aspectCat, the aspect categories of the domain
	 * @param dom, the domain name
	 * @param thres, the threshold to use for the subsumption method
	 * @param invThres, the second threshold for the subsumption method
	 * @param frac, the top fraction of terms to suggest
	 */
	public OntologyBuilder_laptop(SeminarOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, double thres, double invThres, double[] frac, boolean r) {

		/* Initialise the base ontology, aspect categories, and domain name. */
		base = baseOnt;
		aspectCategories = aspectCat;
		domain = dom;
		if (domain.equals("laptop")) {
			numRev = 5001;
		} else {
			numRev = 5001;
		}
		threshold = thres;
		invThreshold = invThres;
		numRejectTerms = 0;
		numAcceptTerms = 0;
		numRejectOverall = 0;
		numAcceptOverall = 0;
		fraction = frac;
		relations = r;

		remove = new HashSet<String>();
		remove.add("http://www.w3.org/2000/01/rdf-schema#Resource");
		remove.add("http://www.w3.org/2002/07/owl#Thing");
		remove.add(base.URI_Mention);
		remove.add(base.URI_Sentiment);
		remove.add(base.NS + "#" + domain.substring(0, 1).toUpperCase() + domain.substring(1).toLowerCase() + "Mention");

		HashMap<String, HashSet<String>> aspectTypes = groupAspects();

		synonymsAccepted = new HashSet<String>();
		HashSet<String> doneAspects = new HashSet<String>();

		/* Loop over the aspect category entities. */

		//create a hashmap with synsets as value of the entities (key), and add as synset property during loop
		HashMap<String, String> entitySynsets = new HashMap<String, String>();
		entitySynsets.put("laptop", "laptop#noun#1");
		entitySynsets.put("display", "display#noun#6");
		entitySynsets.put("keyboard", "keyboard#noun#1");
		entitySynsets.put("mouse", "mouse#noun#4");
		entitySynsets.put("motherboard", "motherboard#noun#0"); 
		entitySynsets.put("cpu", "cpu#noun#1");
		entitySynsets.put("fans_cooling", "cooling#noun#2");
		entitySynsets.put("ports", "port#noun#5");
		entitySynsets.put("memory", "memory#noun#4");
		entitySynsets.put("power_supply", "power#noun#9");
		entitySynsets.put("optical_drives", "disk drive#noun#1");
		entitySynsets.put("battery", "battery#noun#2");
		entitySynsets.put("graphics", "graphic#noun#1");
		entitySynsets.put("hard_disk", "hard disk#noun#1");
		entitySynsets.put("multimedia_devices", "multimedia#noun#1");
		entitySynsets.put("hardware", "hardware#noun#3");
		entitySynsets.put("software", "software#noun#1");
		entitySynsets.put("os", "OS#noun#3");
		entitySynsets.put("warranty", "warranty#noun#1");
		entitySynsets.put("shipping", "shipping#noun#1");
		entitySynsets.put("support", "support#noun#1");
		entitySynsets.put("company", "company#noun#1");

		for (String entity : aspectCat.keySet()) {
			if (entity.equals("software")) {
				entity = "software";
			}
			HashSet<String> aspectSet = aspectCat.get(entity);
			/* Each entity should have its own AspectMention class. */
			HashSet<String> aspects = new HashSet<String>();
			String synset = entitySynsets.get(entity);
			for (String aspect : aspectSet) {//
				/* Don't add miscellaneous to the ontology. */
				if (!aspect.equals("miscellaneous")) {
					aspects.add(entity.toUpperCase() + "#" + aspect.toUpperCase());
				}
			}
			String newClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "Mention", true, entity, aspects, base.URI_EntityMention);
			/* The domain entity doesn't get sentiment classes. */
			if (!entity.equals(domain)) {
				/* Create the SentimentMention classes (positive and negative) related to the entity. */
				String aspectPropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PropertyMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_PropertyMention);
				String aspectActionClassURI =  base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "ActionMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_ActionMention);
				String positivePropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveProperty", false, entity.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Positive);
				String negativePropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeProperty", false, entity.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI,  base.URI_Negative);
				String positiveActionClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveAction", false, entity.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Positive);
				String negativeActionClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeAction", false, entity.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Negative);
				String positiveEntityClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveEntity", false, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_Positive);
				String negativeEntityClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeEntity", false, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_Negative);
				this.suggestSynonyms(entity, newClassURI, aspectPropertyClassURI, aspectActionClassURI);
				if (entity.contains("&")) {
					HashSet<String> lexs = new HashSet<String>();
					String[] parts = entity.split("&");
					lexs.add(parts[0]);
					lexs.add(parts[1]);
					base.addLexicalizations(newClassURI, lexs);
					base.addLexicalizations(aspectPropertyClassURI, lexs);
					base.addLexicalizations(aspectActionClassURI, lexs);
					this.suggestSynonyms(parts[0], newClassURI, aspectPropertyClassURI, aspectActionClassURI);
					this.suggestSynonyms(parts[1], newClassURI, aspectPropertyClassURI, aspectActionClassURI);
				}
				if (entity.contains("_")) { 
					HashSet<String> lexs = new HashSet<String>();
					String[] parts = entity.split("_");
					lexs.add(parts[0]);
					lexs.add(parts[1]);
					base.addLexicalizations(newClassURI, lexs);
					base.addLexicalizations(aspectPropertyClassURI, lexs);
					base.addLexicalizations(aspectActionClassURI, lexs);
					this.suggestSynonyms(parts[0], newClassURI, aspectPropertyClassURI, aspectActionClassURI);
					this.suggestSynonyms(parts[1], newClassURI, aspectPropertyClassURI, aspectActionClassURI);
				}
			} else {
				this.suggestSynonyms(entity, newClassURI);
			}
			/* Create AspectMention and SentimentMention subclasses for all aspects except for general and miscellaneous. */
			for (String aspectName : aspectTypes.keySet()) {
				if (!aspectName.equals("general") && !aspectName.equals("miscellaneous") && !doneAspects.contains(aspectName)) {
					doneAspects.add(aspectName);

					/* Create the AspectMention class. */
					HashSet<String> aspectsAsp = new HashSet<String>();
					for (String entityName : aspectTypes.get(aspectName)) {
						aspectsAsp.add(entityName.toUpperCase() + "#" + aspectName.toUpperCase());
					}
					String newClassURIAspect = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "Mention", true, aspectName, aspectsAsp, base.URI_EntityMention);
					/* Create the SentimentMention classes. */
					String aspectPropertyClassURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PropertyMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_PropertyMention);
					String aspectActionClassURI =  base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "ActionMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_ActionMention);
					String positivePropertyURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveProperty", false, aspectName.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Positive);
					String negativePropertyURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeProperty", false, aspectName.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Negative);
					String positiveActionURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveAction", false, aspectName.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Positive);
					String negativeActionURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeAction", false, aspectName.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Negative);
					String positiveEntityURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveEntity", false, aspectName.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_Positive);
					String negativeEntityURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeEntity", false, aspectName.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_Negative);					
					this.suggestSynonyms(aspectName, newClassURIAspect, aspectPropertyClassURI, aspectActionClassURI);
					if (aspectName.contains("&")) {
						HashSet<String> lexs = new HashSet<String>();
						String[] parts = aspectName.split("&");
						lexs.add(parts[0]);
						lexs.add(parts[1]);
						base.addLexicalizations(newClassURIAspect, lexs);
						base.addLexicalizations(aspectPropertyClassURI, lexs);
						base.addLexicalizations(aspectActionClassURI, lexs);
						this.suggestSynonyms(parts[0], newClassURIAspect, aspectPropertyClassURI, aspectActionClassURI);
						this.suggestSynonyms(parts[1], newClassURIAspect, aspectPropertyClassURI, aspectActionClassURI);
					}
					if (aspectName.contains("_")) { 
						HashSet<String> lexs = new HashSet<String>();
						String[] parts = aspectName.split("_");
						lexs.add(parts[0]);
						lexs.add(parts[1]);
						base.addLexicalizations(newClassURIAspect, lexs);
						base.addLexicalizations(aspectPropertyClassURI, lexs);
						base.addLexicalizations(aspectActionClassURI, lexs);
						this.suggestSynonyms(parts[0], newClassURIAspect, aspectPropertyClassURI, aspectActionClassURI);
						this.suggestSynonyms(parts[1], newClassURIAspect, aspectPropertyClassURI, aspectActionClassURI);
					}
				}
			}			
		}
	}

	/**
	 * A method that suggests the synonyms of a word and adds it as a lexicalization to the concepts.
	 * @param classURI, the concepts to which to add the lexicalizations
	 * @param word, the word of which to find synonyms
	 */
	public void suggestSynonyms(String word, String... classURI) {
		HashSet<String> accepted = new HashSet<String>();
		Synonyms syn = new Synonyms(word);
		System.out.println("Enter 'a' to accept and 'r' to reject the synonym: ");
		Scanner input = new Scanner(System.in);
		int i = 0;
		for (String synonym : syn.synonyms()) {
			i++;
			if (i > 20) {
				break;
			}
			if (synonym.equals(word)) {
				continue;
			}

			System.out.println("synonym: " + word + " --> " + synonym);
			if (input.next().equals("a")) {
				numAcceptOverall++;
				accepted.add(synonym);
				synonymsAccepted.add(synonym);
			} else {
				numRejectOverall++;
			}
		}
		for (String URI : classURI) {
			base.addLexicalizations(URI, accepted);
		}
	}

	/**
	 * Creates an object that stores all the aspect types and for each aspect which entities have this aspect.
	 * @return The HashMap containing the aspects and corresponding entities.
	 */
	public HashMap<String, HashSet<String>> groupAspects() {
		HashMap<String, HashSet<String>> aspectTypes = new HashMap<String, HashSet<String>>();

		/* Loop over the entities. */
		for (String entity : aspectCategories.keySet()) {

			/* Loop over the aspects of the entity. */
			for (String aspect : aspectCategories.get(entity)) {
				HashSet<String> entities;

				/* Check if the set already contains the aspect. */
				if (aspectTypes.containsKey(aspect)) {
					entities = aspectTypes.get(aspect);
				} else {
					entities = new HashSet<String>();
				}
				entities.add(entity);
				aspectTypes.put(aspect, entities);
			}
		}
		return aspectTypes;
	}

	/**
	 * A method that loads the external reviews in order to be handled.
	 */
	public void loadReviews() throws Exception {

		/* Check which domain is being considered. */
		if (domain.equals("restaurant")) {

			TermFrequencyYelp.getFrequency();
			/* Read in the restaurant review data. */
			reviewData =  (new DatasetJSONReader()).read(new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_review_restaurant_auto5001.json"));	
			//reviewData = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Train.json"));
			/* Load the review and document frequencies of all appearing words. */
			try {
				this.wordFrequencyReview = readJSON.readJSONObjectFile(Framework.EXTERNALDATA_PATH + "wordFrequencyReviewYelpAuto5001_multiwords.json");
			} catch (ClassNotFoundException | JSONException | IOException e) {
				e.printStackTrace();
			}
			try {
				this.wordFrequencyDocument = readJSON.readJSONObjectFile(Framework.EXTERNALDATA_PATH + "wordFrequencyDocumentYelpAuto5001_multiwords.json");
			} catch (ClassNotFoundException | JSONException | IOException e) {
				e.printStackTrace();
			}
			

			
		} else if (domain.equals("laptop")) {

			/* Read in the laptop review data. */
			reviewData =  (new DatasetJSONReader()).read(new File(Framework.EXTERNALDATA_PATH+"amazon_review_laptop5001.json"));	
			//reviewData = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Laptops-Train.json"));
			/* Load the review and document frequencies of all appearing words. */
			try {
				this.wordFrequencyReview = readJSON.readJSONObjectFile(Framework.EXTERNALDATA_PATH + "wordFrequencyReviewAmazon5001.json");
			} catch (ClassNotFoundException | JSONException | IOException e) {
				e.printStackTrace();
			}
			try {
				this.wordFrequencyDocument = readJSON.readJSONObjectFile(Framework.EXTERNALDATA_PATH + "wordFrequencyDocumentAmazon5001.json");
			} catch (ClassNotFoundException | JSONException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method that loads the external alternate domain documents.
	 */
	public void loadContrast() {

		/* Load Alice in Wonderland. */
		File alice = new File(Framework.EXTERNALDATA_PATH+"alice30.txt");

		/* Loop over all the words and keep track of their frequencies. */
		HashMap<String, Integer> aliceFreq = new HashMap<String, Integer>();
		try {
			Scanner sc = new Scanner(alice);
			sc.useDelimiter("[^a-zA-Z]+");
			while (sc.hasNext()) {
				String word = sc.next().toLowerCase();

				/* Update the frequency of the word. */
				if (aliceFreq.containsKey(word)) {
					aliceFreq.put(word, aliceFreq.get(word) + 1);

				} else  {
					aliceFreq.put(word, 1);
				}
			}
			sc.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		contrastData = new HashMap<String, HashMap<String, Integer>>();
		this.contrastData.put("Alice", aliceFreq);

		/* Load Pride and Prejudice. */
		File pride = new File(Framework.EXTERNALDATA_PATH+"prideandprejudice.txt");

		/* Loop over all the words and keep track of their frequencies. */
		HashMap<String, Integer> prideFreq = new HashMap<String, Integer>();
		try {
			Scanner sc = new Scanner(pride);
			sc.useDelimiter("[^a-zA-Z]+");
			while (sc.hasNext()) {
				String word = sc.next().toLowerCase();

				/* Update the frequency of the word. */
				if (prideFreq.containsKey(word)) {
					prideFreq.put(word, prideFreq.get(word) + 1);

				} else  {
					prideFreq.put(word, 1);
				}
			}
			sc.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		contrastData = new HashMap<String, HashMap<String, Integer>>();
		this.contrastData.put("Pride", prideFreq);

		/* Load Sherlock Holmes. */
		File sherlock = new File(Framework.EXTERNALDATA_PATH+"sherlockholmes.txt");

		/* Loop over all the words and keep track of their frequencies. */
		HashMap<String, Integer> sherlockFreq = new HashMap<String, Integer>();
		try {
			Scanner sc = new Scanner(sherlock);
			sc.useDelimiter("[^a-zA-Z]+");
			while (sc.hasNext()) {
				String word = sc.next().toLowerCase();

				/* Update the frequency of the word. */
				if (sherlockFreq.containsKey(word)) {
					sherlockFreq.put(word, sherlockFreq.get(word) + 1);

				} else  {
					sherlockFreq.put(word, 1);
				}
			}
			sc.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		contrastData = new HashMap<String, HashMap<String, Integer>>();
		this.contrastData.put("Sherlock", sherlockFreq);

		/* Load Tom Sawyer. */
		File tom = new File(Framework.EXTERNALDATA_PATH+"tomsawyer.txt");

		/* Loop over all the words and keep track of their frequencies. */
		HashMap<String, Integer> tomFreq = new HashMap<String, Integer>();
		try {
			Scanner sc = new Scanner(tom);
			sc.useDelimiter("[^a-zA-Z]+");
			while (sc.hasNext()) {
				String word = sc.next().toLowerCase();

				/* Update the frequency of the word. */
				if (tomFreq.containsKey(word)) {
					tomFreq.put(word, tomFreq.get(word) + 1);

				} else  {
					tomFreq.put(word, 1);
				}
			}
			sc.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		contrastData = new HashMap<String, HashMap<String, Integer>>();
		this.contrastData.put("Tom", tomFreq);

		/* Load Great Expectations */
		File great = new File(Framework.EXTERNALDATA_PATH+"greatexpectations.txt");

		/* Loop over all the words and keep track of their frequencies. */
		HashMap<String, Integer> greatFreq = new HashMap<String, Integer>();
		try {
			Scanner sc = new Scanner(great);
			sc.useDelimiter("[^a-zA-Z]+");
			while (sc.hasNext()) {
				String word = sc.next().toLowerCase();

				/* Update the frequency of the word. */
				if (greatFreq.containsKey(word)) {
					greatFreq.put(word, greatFreq.get(word) + 1);

				} else  {
					greatFreq.put(word, 1);
				}
			}
			sc.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		contrastData = new HashMap<String, HashMap<String, Integer>>();
		this.contrastData.put("Great", greatFreq);

		/* Load Sense and Sensibility. */
		File sense = new File(Framework.EXTERNALDATA_PATH+"senseandsensibility.txt");

		/* Loop over all the words and keep track of their frequencies. */
		HashMap<String, Integer> senseFreq = new HashMap<String, Integer>();
		try {
			Scanner sc = new Scanner(sense);
			sc.useDelimiter("[^a-zA-Z]+");
			while (sc.hasNext()) {
				String word = sc.next().toLowerCase();

				/* Update the frequency of the word. */
				if (senseFreq.containsKey(word)) {
					senseFreq.put(word, senseFreq.get(word) + 1);

				} else  {
					senseFreq.put(word, 1);
				}
			}
			sc.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		contrastData = new HashMap<String, HashMap<String, Integer>>();
		this.contrastData.put("Sense", senseFreq);
	}

	/**
	 * A method for finding important terms. 
	 * @param nn, true if searching for nouns
	 * @param adj, true if searching for adjectives
	 * @param alpha,
	 * @param beta, 
	 * @throws IOException 
	 * @throws IllegalSpanException 
	 * @throws JSONException 
	 * @throws ClassNotFoundException 
	 */
	public void findTerms(boolean nn, boolean adj, boolean vrb, double alpha, double beta) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException {

		HashMap<String, Double> nouns = new HashMap<String, Double>(); //entities
		HashMap<String, Double> adjectives = new HashMap<String, Double>(); //actions
		HashMap<String, Double> verbs = new HashMap<String, Double>(); //properties

		double fractionNouns = fraction[0];
		double fractionAdj = fraction[1]; 
		double fractionVerb = fraction[2]; 	
		HashMap<String, Integer> nounSenses = new HashMap<String, Integer>(); 
		HashMap<String, String> nounContext = new HashMap<String, String>(); 
		HashMap<String, String> verbContext = new HashMap<String, String>();
		HashMap<String, String> adjContext = new HashMap<String, String>();

		HashMap<String, Double> aspectNouns = new HashMap<String, Double>();
		HashMap<String, Double> sentimentNouns = new HashMap<String, Double>();
		HashMap<String, Double> aspectAdjectives = new HashMap<String, Double>();
		HashMap<String, Double> sentimentAdjectives = new HashMap<String, Double>();
		HashMap<String, Double> aspectVerbs = new HashMap<String, Double>();
		HashMap<String, Double> sentimentVerbs = new HashMap<String, Double>();

		relatedNouns = new HashMap<String, HashSet<String>>();
		nounsWithSynset = new HashMap<String, HashSet<String>>();

		HashMap<String, Double> DPs = new HashMap<String, Double>(); 
		HashMap<String, Double> DCs = new HashMap<String, Double>(); 

		double maxDP = 0.0;
		double maxDC = 0.0;

		int max_length = 3; //only consider phrases up until a length of 4 words (max_length = 3)

		//Create a HashMap with all novels
		HashMap<String, File> contrastTexts = new HashMap<String, File>();
		File alice = new File(Framework.EXTERNALDATA_PATH+"alice30.txt");
		File pride = new File(Framework.EXTERNALDATA_PATH+"prideandprejudice.txt");
		File sherlock = new File(Framework.EXTERNALDATA_PATH+"sherlockholmes.txt");
		File tom = new File(Framework.EXTERNALDATA_PATH+"tomsawyer.txt");
		File great = new File(Framework.EXTERNALDATA_PATH+"greatexpectations.txt");
		File sensesensib = new File(Framework.EXTERNALDATA_PATH+"senseandsensibility.txt");

		HashMap<String, HashMap<String, HashMap<String, Integer>>> wordFrequencies = new HashMap<String, HashMap<String, HashMap<String, Integer>>>();
		contrastTexts.put("Alice in Wonderland", alice);
		contrastTexts.put("Pride and Prejudice", pride);
		contrastTexts.put("Sherlock Holmes", sherlock);
		contrastTexts.put("Tom Sawyer", tom);
		contrastTexts.put("Great Expectations", great);
		contrastTexts.put("Sense and Sensibility", sensesensib);
		/* Loop over all the reviews in the dataset. */
		for (Span review : reviewData.getSpans("review")){

			//HashSet<Word> wordsToRemove = new HashSet<>();
			int wordsToSkip = 0;
			/* Loop over all words in the review, but give longer phrases priority over shorter phrases. */
			Span scope = review.getTextualUnit();
			for(Word word : scope){
				File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
				System.setProperty("wordnet.database.dir", f.toString());
				WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();
				if (wordsToSkip > 0) {
					wordsToSkip--;
					continue;
				}

				String candidateMultiWord = word.getWord();
				String candidateMultiLemma = word.getLemma();
				Word endWord = word;
				String multiWord = candidateMultiLemma;
				Word nextWord = word;
				//if (nextWord.equals(".")||nextWord.equals(",")||nextWord.equals("!")||nextWord.equals("?")) {
				//	continue;
				//}
				boolean proceed = true;
				int j = 0;

				while (nextWord.hasNextWord()&&proceed && j < max_length) {
					nextWord = nextWord.getNextWord();
					if (nextWord.getWord().startsWith(".")||nextWord.getWord().startsWith("'")){
						candidateMultiLemma = candidateMultiWord + nextWord.getLemma();
						candidateMultiWord += nextWord.getWord();
					} 
					else {
						candidateMultiLemma = candidateMultiWord + " " + nextWord.getLemma();
						candidateMultiWord += " " + nextWord.getWord();
					}
					j++;
					//System.out.println(candidateMultiLemma);
					if (wordDatabase.getSynsets(candidateMultiLemma).length > 0) {
						endWord = nextWord;
						multiWord = candidateMultiLemma;
					}
				}
				if (wordDatabase.getSynsets(multiWord).length<1)
				{
					continue;
				}
				String lemma = word.getLemma();
				String pos = word.getAnnotation("pos");
				if (!multiWord.equals(word.toString()) && !multiWord.equals(lemma)){
					lemma = multiWord.toLowerCase();
					String type = wordDatabase.getSynsets(multiWord)[0].getType().toString();
					if (type.equals("1")) //noun
					{
						pos = "NN";
					}
					else if (type.equals("2")) //verb
					{
						pos = "VB";
					}
					else if (type.equals("3")) //adjective
					{
						pos = "JJ";
					}
				}

				/* Loop over all the words to find the types. */
				//Word word = w;
				//String pos = word.getAnnotation("pos");
				//String lemma = word.getAnnotation("lemma", String.class);

				if (vrb && (pos.equals("VB")||pos.equals("VBD")||pos.equals("VBG")||pos.equals("VBN")||pos.equals("VBN")||pos.equals("VBP"))) {
					lemma = verbConvertion(lemma);
				}
				if (lemma == null) {
					continue;
				}
				if (lemma.length() < 2 ) {
					continue;
				}

				//boolean mult = lemma.contains(" ");
				//if (mult)
				//{
				//		wordFrequencies = TermFrequencyYelp.getFrequency(lemma);
				//	}

				/* Calculate the domain pertinence. */
				double domainFreq = 0.0;
				//	if (mult) //if the lemma is a multi-word phrase, wordfrequency still has to be calculated
				//	{
				//		HashMap<String, HashMap<String,Integer>> wordFreqDocCast = wordFrequencies.get("wordFrequencyDocumentCast");
				//		HashMap<String,Integer> wordFreqDoc = wordFreqDocCast.get("wordFrequencyDocument"); 
				//		Integer ints = wordFreqDoc.get(lemma);
				//		domainFreq = (double) ints;
				//	}
				//	else
				//	{
				if (wordFrequencyDocument.has(lemma)) {
					domainFreq = (double) (int) wordFrequencyDocument.get(lemma);
				}
				//	}

				/* Find the maximum frequency of lemma in the contrasting corpus. */
				double contrastFreq = 0.0;
				//	if (mult) //if the lemma is a multi-word phrase, wordfrequency still has to be calculated
				//	{


				//				/* For each contrast text, calculate frequency and store max*/
				//				for (String name : contrastTexts.keySet()) // for each novel
				//				{
				//					Integer phraseFreq = 0; // number of times the lemma phrase appears
				//					String text = contrastTexts.get(name).toString(); // get the novel text as String
				//					int index = text.indexOf(lemma);
				//					while (index != -1) {
				//						phraseFreq = phraseFreq +1;  
				//						text = text.substring(index + 1);
				//						index = text.indexOf(lemma);
				//					}
				//					if (phraseFreq > contrastFreq)
				//					{
				//						contrastFreq = phraseFreq;
				//					}
				///				}
				//
				//				}
				//				else {
				for (String dom : contrastData.keySet()) {
					HashMap<String, Integer> frequencies = contrastData.get(dom);
					if (frequencies.containsKey(lemma)) {
						double freq = (double) (int) frequencies.get(lemma);
						/* Find the maximum frequency. */
						if (freq > contrastFreq) {
							contrastFreq = freq;
						}
					}
				}
				//	}

				double DP = domainFreq / contrastFreq;
				if (DP == Double.POSITIVE_INFINITY) {
					DP = 0.0;
				}
				DPs.put(lemma, DP);

				/* Find the maximum domain pertinence score. */
				if (DP > maxDP) {
					maxDP = DP;
				}

				/* Calculate the domain consensus. */
				double DC = 0.0;


				double maxDomainFreq = 0.0;
				//if (mult) //if multiwordphrase
				//{
				//	/* Find the maximum frequency of lemma across the reviews. */
				//	for (String revId : wordFrequencies.get("wordFrequencyReview").keySet())
				//	{
				//		if (wordFrequencyReview.getJSONObject(revId).has(lemma))
				//		{
				//			double revFreq = (double) (int) wordFrequencyReview.getJSONObject(revId).get(lemma);
				//			if (revFreq > maxDomainFreq) {
				//				maxDomainFreq = revFreq;
				//			}
				//		}
				//	}

				/* Loop over all the reviews. */
				//	for (String revId2 : wordFrequencies.get("wordFrequencyReview").keySet()) {
				//		if (wordFrequencyReview.getJSONObject(revId2).has(lemma)) {
				//			double frequency = (double) (int) wordFrequencyReview.getJSONObject(revId2).get(lemma);
				//			double normFreq = frequency / maxDomainFreq;
				//			DC += -1 * (normFreq) * Math.log(normFreq);
				//		}
				//	}
				//}
				//else
				//{
				/* Find the maximum frequency of lemma across the reviews. */
				for (String revId : wordFrequencyReview.keySet()) {
					if (wordFrequencyReview.getJSONObject(revId).has(lemma)) {
						double revFreq = (double) (int) wordFrequencyReview.getJSONObject(revId).get(lemma);
						if (revFreq > maxDomainFreq) {
							maxDomainFreq = revFreq;
						}
					}

				}

				/* Loop over all the reviews. */
				for (String revId2 : wordFrequencyReview.keySet()) {
					if (wordFrequencyReview.getJSONObject(revId2).has(lemma)) {
						double frequency = (double) (int) wordFrequencyReview.getJSONObject(revId2).get(lemma);
						double normFreq = frequency / maxDomainFreq;
						DC += -1 * (normFreq) * Math.log(normFreq);
					}
				}
				//}



				DCs.put(lemma, DC);

				/* Find the maximum domain consensus score. */
				if (DC > maxDC) {
					maxDC = DC;
				}
				/* If DC DP are not both 0.0 add it to nouns/adjectives. */
				if (!(DP == 0.0 && DC == 0.0)) {
					if (adj && (pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS"))) {
						adjectives.put(lemma, 0.0);
						int i = 0;
						String text = lemma;
						if (relations) {
							for (Relation rel : word.getRelations().getAllRelationsToParents()) {
								if (rel.getAnnotation("relationShortName").equals("nsubj") || rel.getAnnotation("relationShortName").equals("amod")) {
									Word nword = (Word) rel.getParent();
									String relNoun = nword.getAnnotation("lemma", String.class);
									HashSet<String> related;
									if (relatedNouns.containsKey(lemma)) {
										related = relatedNouns.get(lemma);
									} else {
										related = new HashSet<String>();
									}
									related.add(relNoun);
									relatedNouns.put(lemma, related);
								}
							}
						}

						for (Word context : review.getWords()) {
							String pos1 = context.getAnnotation("pos");
							if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
								text = text + " " + context.getAnnotation("lemma", String.class);
							}
						}
						//store context for WSD
						//Word word2 = word;
						//while (word2.hasPreviousWord() && i<5) {
						//	String pos1 = word2.getPreviousWord().getAnnotation("pos");
						//	if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
						//		String lemma2 = word2.getPreviousWord().getAnnotation("lemma", String.class);
						//		text = text +" "+ lemma2;
						//		word2 = word2.getPreviousWord();
						//		i++;
						//	}
						//	else {
						//		word2 = word2.getPreviousWord();
						///		i++;
						//	}
						//}
						//Word word3 = endWord;
						//while (word3.hasNextWord() && i<10) {
						//	String pos2 = word3.getNextWord().getAnnotation("pos");
						//	if(pos2.equals("NN") || pos2.equals("NNS") || pos2.equals("NNP") || pos2.equals("VB")||pos2.equals("VBD")||pos2.equals("VBG")||pos2.equals("VBN")||pos2.equals("VBN")||pos2.equals("VBP")||pos2.equals("JJ")) {
						//		String lemma3 = word3.getNextWord().getAnnotation("lemma", String.class);
						//		text = text +" "+ lemma3;
						//		word3 = word3.getNextWord();
						//		i++;
						//	}
						//	else {
						//		word3 = word3.getNextWord();
						//		i++;
						//	}

						//}

						if(adjContext.containsKey(lemma)) {
							adjContext.put(lemma, adjContext.get(lemma)+" "+text);
						}
						else {
							adjContext.put(lemma, text);
						}

					} else if (nn && (pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") )){
						nouns.put(lemma, 0.0);
						int i = 0;
						String text = lemma;


						for (Word context : review.getWords()) {
							String pos1 = context.getAnnotation("pos");
							if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
								text = text + " " + context.getAnnotation("lemma", String.class);
							}
						}
						//Word word2 = word;
						//while (word2.hasPreviousWord() && i<5) {
						//	String pos1 = word2.getPreviousWord().getAnnotation("pos");
						//	if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
						//		String lemma2 = word2.getPreviousWord().getAnnotation("lemma", String.class);
						//		text = text +" "+ lemma2;
						//		word2 = word2.getPreviousWord();
						//		i++;
						//	}
						//	else {
						//		word2 = word2.getPreviousWord();
						//		i++;
						//	}


						//}
						//Word word3 = endWord;
						//while (word3.hasNextWord() && i<10) {
						//	String pos2 = word3.getNextWord().getAnnotation("pos");
						//	if(pos2.equals("NN") || pos2.equals("NNS") || pos2.equals("NNP") || pos2.equals("VB")||pos2.equals("VBD")||pos2.equals("VBG")||pos2.equals("VBN")||pos2.equals("VBN")||pos2.equals("VBP")||pos2.equals("JJ")) {
						//		String lemma3 = word3.getNextWord().getAnnotation("lemma", String.class);
						//		text = text +" "+ lemma3;
						//		word3 = word3.getNextWord();
						//		i++;
						//	}
						//	else { 
						//		word3 = word3.getNextWord();
						//		i++;
						//	}

						//}

						if(nounContext.containsKey(lemma)) {
							nounContext.put(lemma, nounContext.get(lemma)+" "+text);
						}
						else {
							nounContext.put(lemma, text);
						}


					} else if (vrb && (pos.equals("VB")||pos.equals("VBD")||pos.equals("VBG")||pos.equals("VBN")||pos.equals("VBN")||pos.equals("VBP"))) {
						verbs.put(lemma, 0.0);
						//store context for WSD
						int i = 0;
						String text = lemma;

						for (Word context : review.getWords()) {
							String pos1 = context.getAnnotation("pos");
							if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
								text = text + " " + context.getAnnotation("lemma", String.class);
							}
						}
						//Word word2 = word;
						//while (word2.hasPreviousWord() && i<5) {
						//	String pos1 = word2.getPreviousWord().getAnnotation("pos");
						//	if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
						//		String lemma2 = word2.getPreviousWord().getAnnotation("lemma", String.class);
						//		text = text +" "+ lemma2;
						//		word2 = word2.getPreviousWord();
						//		i++;
						//	}
						//	else {
						//		word2 = word2.getPreviousWord();
						//		i++;
						//	}
						//}
						//Word word3 = endWord;
						//while (word3.hasNextWord() && i<10) {
						//	String pos2 = word3.getNextWord().getAnnotation("pos");
						//	if(pos2.equals("NN") || pos2.equals("NNS") || pos2.equals("NNP") || pos2.equals("VB")||pos2.equals("VBD")||pos2.equals("VBG")||pos2.equals("VBN")||pos2.equals("VBN")||pos2.equals("VBP")||pos2.equals("JJ")) {
						//		String lemma3 = word3.getNextWord().getAnnotation("lemma", String.class);
						//		text = text +" "+ lemma3;
						//		word3 = word3.getNextWord();
						//		i++;
						//	}
						//	else {
						//		word3 = word3.getNextWord();
						//		i++;
						//	}

						//}

						if(verbContext.containsKey(lemma)) {
							verbContext.put(lemma, verbContext.get(lemma)+" "+text);
						}
						else {
							verbContext.put(lemma, text);
						}

					}
				}
			}
		}

		if (vrb) {
			verbs.keySet().removeAll(synonymsAccepted);
			/* Calculate the scores. */
			Double[] scores = new Double[verbs.size()];
			int i = 0;
			for (String w : verbs.keySet()) {
				double score = alpha * (DPs.get(w) / maxDP) + beta * (DCs.get(w) / maxDC);
				verbs.put(w, score);
				scores[i] = score;
				i++;
			}
			/* Find the threshold value to get only the top n% of the terms. */
			Arrays.sort(scores);
			double ind = fractionVerb * verbs.size();
			int index = (int) ind;			
			double scoreThreshold = scores[verbs.size() - 1 - index];	

			System.out.println("Enter 's' to indicate sentimental Verb, 'a' to indicate aspect verb, and 'r' to reject verb: ");
			Scanner in = new Scanner(System.in);
			for (String w : verbs.keySet()) {
				double score = verbs.get(w);
				if (score > scoreThreshold) {
					System.out.println("Verb: " + w);
					String input = in.next();
					if (input.equals("a")) { //aspectverb
						aspectVerbs.put(w, score);
						numAcceptTerms++;
						numAcceptOverall++;
					} else if (input.equals("s")) { //sentimentVerbs
						sentimentVerbs.put(w, score);
						numAcceptTerms++;
						numAcceptOverall++;

					} else {
						numRejectTerms++;
						numRejectOverall++;

					}
				}
			}
		}
		this.subsumptionAspect(aspectVerbs, "verb", verbContext);
		this.subsumptionSentiment(sentimentVerbs, "verb", verbContext);
		if (nn) {
			/* Calculate the scores. */
			nouns.keySet().removeAll(sentimentVerbs.keySet());
			nouns.keySet().removeAll(aspectVerbs.keySet());
			nouns.keySet().removeAll(synonymsAccepted);
			Double[] scores = new Double[nouns.size()];
			int i = 0;
			File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
			System.setProperty("wordnet.database.dir", f.toString());
			WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();
			for (String w : nouns.keySet()) {
				Synset[] synsets = wordDatabase.getSynsets(w);
				if (synsets.length > 1){	
					if(synsets[1].getType().toString().equals("1")) {
						double score = alpha * (DPs.get(w) / maxDP) + beta * (DCs.get(w) / maxDC);
						nouns.put(w, score);
						scores[i] = score;
						i++;
					} else {
						scores[i] = 0.0;
						i++;
					}
				} else {
					scores[i] = 0.0;
					i++;
				}
			}

			/* Find the threshold value to get only the top n% of the terms. */
			Arrays.sort(scores);
			double ind = fractionNouns * nouns.size();
			int index = (int) ind;
			double scoreThreshold = scores[nouns.size() - 1 - index];

			//for (String l : nouns.keySet()) {
			//	double score1 = nouns.get(l);
			//	if (score1 > scoreThreshold) {
			//		String t = nounContext.get(l);
			//		String[] A = WordSenseDisambiguation.wordsToArray(t);
			//		int sense = WordSenseDisambiguation.Sense(A, l);
			//		nounSenses.put(l,sense);
			//	}
			//}

			System.out.println("Enter 'a' to accept the noun as aspectbased, enter 's' to accept the noun as sentimentbased");
			System.out.println("and 'r' to reject the noun: ");
			Scanner in = new Scanner(System.in);
			for (String w : nouns.keySet()) {
				double score = nouns.get(w);
				if (score > scoreThreshold) {
					System.out.println("noun: " + w);
					String input = in.next();
					if (input.equals("a")) {
						HashSet<String> a = new HashSet<String>(); 	//added
						a.add(w);									//added
						//						System.out.println(a);
						nounsWithSynset.put(w, a);
						//						System.out.println(nounsWithSynset);
						aspectNouns.put(w, score);
						numAcceptTerms++;
						numAcceptOverall++;
						nouns.put(w, 0.0);
						//for (String w2 : nouns.keySet()) {
						//	double score2 = nouns.get(w2);
						//	if (score2 > scoreThreshold) {
						//		double r = Wu_Palmer.WupSimilarity(w,nounSenses.get(w),w2,nounSenses.get(w2),"n");
						//		if (r>0.88) {
						//			//									acceptedNouns.put(w2, score);
						//			nounsWithSynset.get(w).add(w2);
						//			numAcceptTerms++;
						//			numAcceptOverall++;
						//			nouns.put(w2, 0.0);
						//		}
						//	}
						//}
					} else if (input.equals("s")) {
						HashSet<String> a = new HashSet<String>(); 	//added
						a.add(w);									//added
						//System.out.println(a);
						nounsWithSynset.put(w, a);
						//System.out.println(nounsWithSynset);
						sentimentNouns.put(w, score);
						numAcceptTerms++;
						numAcceptOverall++;
						nouns.put(w, 0.0);
						//for (String w2 : nouns.keySet()) {
						//	double score2 = nouns.get(w2);
						//	if (score2 > scoreThreshold) {
						//		double r = Wu_Palmer.WupSimilarity(w,nounSenses.get(w),w2,nounSenses.get(w2),"n");
						//		if (r>0.88) {
						//			sentimentNouns.put(w2, score);
						//			nounsWithSynset.get(w).add(w2);
						//			numAcceptTerms++;
						//			numAcceptOverall++;
						//			nouns.put(w2, 0.0);
						//		}
						//	}
						//}
					} else {
						numRejectTerms++;
						numRejectOverall++; //reject all other comparable words.
						nouns.put(w, 0.0);
						//for (String w2 : nouns.keySet()) {
						//	double score2 = nouns.get(w2);
						//	if (score2 > scoreThreshold) {
						//		double r = Wu_Palmer.WupSimilarity(w,nounSenses.get(w),w2,nounSenses.get(w2),"n");
						//		if (r>0.88) {
						//			numRejectTerms++;
						//			numRejectOverall++;
						//			nouns.put(w2, 0.0);
						//		}
						//	}
						//}
					}
				}
			}
		}
		this.subsumptionAspect(aspectNouns, "noun", nounContext);
		this.subsumptionSentiment(sentimentNouns, "noun", nounContext);
		this.save("OntologyTussenProduct.owl");
		if (adj) {
			adjectives.keySet().removeAll(sentimentNouns.keySet());
			adjectives.keySet().removeAll(aspectNouns.keySet());
			adjectives.keySet().removeAll(sentimentVerbs.keySet());
			adjectives.keySet().removeAll(aspectVerbs.keySet());
			adjectives.keySet().removeAll(synonymsAccepted);
			/* Calculate the scores. */
			Double[] scores = new Double[adjectives.size()];
			int i = 0;
			for (String w : adjectives.keySet()) {
				double score = alpha * (DPs.get(w) / maxDP) + beta * (DCs.get(w) / maxDC);
				adjectives.put(w, score);
				scores[i] = score;
				i++;
			}
			/* Find the threshold value to get only the top n% of the terms. */
			Arrays.sort(scores);
			double ind = fractionAdj * adjectives.size();
			int index = (int) ind;			
			double scoreThreshold = scores[adjectives.size() - 1 - index];	

			//System.out.println("Enter 'a' to accept and 'r' to reject the adjective: ");
			//Scanner in = new Scanner(System.in);
			//for (String w : adjectives.keySet()) {
			//double score = adjectives.get(w);
			//if (score > scoreThreshold) {
			//	System.out.println("adjective: " + w);
			//	if (in.next().equals("a")) {
			//		acceptedAdjectives.put(w, score);
			//numAcceptTerms++;
			//	numAcceptOverall++;
			//} else {
			//	numRejectTerms++;
			//	numRejectOverall++;
			//			}
			//		}
			//	}

			System.out.println("Enter 'a' to accept the adjective as aspectbased, enter 's' to accept the adjective as sentimentbased");
			System.out.println("and 'r' to reject the adjective: ");
			Scanner in = new Scanner(System.in);
			for (String w : adjectives.keySet()) {
				double score = adjectives.get(w);
				if (score > scoreThreshold) {
					System.out.println("adjective: " + w);
					String input = in.next();
					if (input.equals("a")) {
						HashSet<String> a = new HashSet<String>(); 	//added
						a.add(w);									//added
						aspectAdjectives.put(w, score);
						numAcceptTerms++;
						numAcceptOverall++;
						adjectives.put(w, 0.0);
					} else if (input.equals("s")) {
						HashSet<String> a = new HashSet<String>(); 	//added
						a.add(w);									//added
						sentimentAdjectives.put(w, score);
						numAcceptTerms++;
						numAcceptOverall++;
						adjectives.put(w, 0.0);
					} else {
						numRejectTerms++;
						numRejectOverall++; //reject all other comparable words.
						adjectives.put(w, 0.0);
					}
				}
			}
		}

		this.subsumptionSentiment(sentimentAdjectives, "adjective", adjContext);
		this.subsumptionAspect(aspectAdjectives, "adjective", adjContext);

	}


	/**
	 * A method that uses the subsumption method to determine the Aspect superclasses of the accepted terms
	 * and adds them to the ontology.
	 * @param words, the accepted terms to be added to the ontology.
	 */
	public void subsumptionAspect(HashMap<String, Double> words, String pos, HashMap<String, String> context) {		//yes to noun, pos = "noun" or "verb" or "adjective"
		HashSet<String> set;

		//set = base.getSubclasses(base.URI_SentimentMention);
		//set.remove(base.URI_SentimentMention);
		//HashSet<String> temp = base.getSubclasses(base.URI_AspectMention);
		//temp.removeAll(base.getSubclasses(base.URI_SentimentMention));
		//set.removeAll(temp);

		if (pos.equals("noun")) { //TODO: check of je gewoon alle apsectEntityMentions krijgt, en same voor action and property
			set = base.getSubclasses(base.URI_EntityMention); 
			set.remove(base.URI_EntityMention);
			HashSet<String> temp = base.getSubclasses(base.URI_ActionMention);
			temp.addAll(base.getSubclasses(base.URI_PropertyMention));
			temp.addAll(base.getSubclasses(base.URI_Sentiment));
			//temp.removeAll(base.getSubclasses(base.URI_EntityMention));
			set.removeAll(temp);
		}
		else if (pos.equals("verb")) {
			set = base.getSubclasses(base.URI_ActionMention); 
			set.remove(base.URI_ActionMention);
			HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
			//HashSet<String> temp = base.getSubclasses(base.URI_EntityMention);
			//temp.addAll(base.getSubclasses(base.URI_PropertyMention));
			//temp.removeAll(base.getSubclasses(base.URI_ActionMention));
			set.removeAll(temp);
		}
		else {
			set = base.getSubclasses(base.URI_PropertyMention); 
			set.remove(base.URI_PropertyMention);
			HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
			set.removeAll(temp);
		}

		/* Loop over all the accepted words. */
		for (String word : words.keySet()) {

			double numWord = 0.0;

			/* Only increment the parent during the first loop over the reviews. */
			boolean checkWord = true;

			HashMap<String, Double> parents = new HashMap<String, Double>();
			TreeMap<Double, String> scoreParents = new TreeMap<Double, String>();

			for (String parentURI : set) {
				HashSet<String> parent = base.getLexicalizations(parentURI);

				double score = 0.0;

				double numParent = 0.0;
				double numParentCond = 0.0;

				/* Loop over all the reviews to see if they appear with the word. */
				for (Span review : reviewData.getSpans("review")){
					String revId = review.getAnnotation("id", String.class);
					JSONObject obj = wordFrequencyReview.getJSONObject(revId);

					/* Update the counters. */
					for (String par : parent) {
						if (obj.has(par)) {
							numParent++;
							break;
						} 
					}
					if (obj.has(word)) {
						if (checkWord) {
							numWord++;
						}
						for (String par : parent) {
							if (obj.has(par)) {
								numParentCond++;
								break;
							} 
						}
					}
				}
				checkWord = false;

				double wordProb = ( numParentCond / numRev ) / ( numWord / numRev );
				score += wordProb;

				/* Check if a possible parent. */
				double wordProbInv = ( numParentCond / numRev ) / ( numParent / numRev );
				if (wordProb >= threshold && wordProbInv < invThreshold ) {
					while (scoreParents.containsKey(-score)) {
						score += 0.000000000000001;
					}
					scoreParents.put(-score, parentURI);
					parents.put(parentURI, score);
				}
			}

			/* Suggest the parents, highest score first, till one or none is accepted. */
			if (!parents.isEmpty()) {
				System.out.println("Enter 'i'if there are no relations (anymore) for "+word+ ". Enter 'a' to accept and 'r' to reject the relation: ");
			}
			Scanner in = new Scanner(System.in);

			/* Loop over all the possible parents. */
			boolean accept = false;
			for (double parentScore : scoreParents.keySet()) {
				if (!accept) { //not accepted as either negative of positive generic.
					String finalParentURI = scoreParents.get(parentScore);	
					parentScore = -parentScore;			
					System.out.println("Type 'a' to accept and 'r' to reject relation: " + word + " --> " + finalParentURI + " : " + parentScore + " type 'i'if no more parent classes");
					String input= in.next();
					if (input.equals("a")) {
						numAcceptOverall++;
						//System.out.println("Does this concept carry sentiment? Type 'p' if strictly positive and 'n' if stricly negative, 'r' if else");
						//String input_sent = in.next();
						//if (input_sent.equals("positive"))
						//{
						if (nounsWithSynset.keySet().contains(word)) {
							for (String wInSynset : nounsWithSynset.get(word)) {
								String newConcept = base.addClass(pos, context, wInSynset.substring(0, 1).toUpperCase() + wInSynset.substring(1).toLowerCase(), true, wInSynset, new HashSet<String>(), finalParentURI);
								//this.suggestSynonyms(wInSynset, newConcept);
							}
						}
						else {
							String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), finalParentURI);
							//this.suggestSynonyms(word, newConcept);
						}
						//}
						// for type three we disregard breaking, but add to more parent classes break;
					} else if (input.equals("i")) {
						numRejectOverall++;
						break;
					} else { numRejectOverall++;}
				}
			}
		}
	}

	/**
	 * A method that uses the subsumption method to determine the Sentiment based superclasses of the accepted terms
	 * and adds them to the ontology.
	 * @param words, the accepted terms to be added to the ontology.
	 */
	public void subsumptionSentiment(HashMap<String, Double> words, String pos, HashMap<String, String> context) { //noun=false, pos = verb, noun or adjective	
		HashSet<String> set;
		double numerator = 0.0;
		double denominator = 0.0;
		if (pos.equals("noun")) { // TODO; check of je gewoon alle aspectEntity/Action/Property classes krijgt, niet de sentiment classes
			set = base.getSubclasses(base.URI_EntityMention); 
			set.remove(base.URI_EntityMention);
			HashSet<String> temp = base.getSubclasses(base.URI_ActionMention);
			temp.addAll(base.getSubclasses(base.URI_PropertyMention));
			temp.addAll(base.getSubclasses(base.URI_Sentiment));
			//temp.removeAll(base.getSubclasses(base.URI_EntityMention));
			set.removeAll(temp);
		}
		else if (pos.equals("verb")) {
			set = base.getSubclasses(base.URI_ActionMention); 
			set.remove(base.URI_ActionMention);
			HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
			//HashSet<String> temp = base.getSubclasses(base.URI_EntityMention);
			//temp.addAll(base.getSubclasses(base.URI_PropertyMention));
			//temp.removeAll(base.getSubclasses(base.URI_ActionMention));
			set.removeAll(temp);
		}
		else {
			set = base.getSubclasses(base.URI_PropertyMention); 
			set.remove(base.URI_PropertyMention);
			HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
			set.removeAll(temp);
		}
		//if (pos.equals("noun")) {
		//	set= base.getSubclasses(base.URI_Sentiment);
		//	set.remove(base.URI_Sentiment);
		//	HashSet<String> temp = base.getSubclasses(base.URI_ActionMention);
		//	temp.addAll(base.getSubclasses(base.URI_PropertyMention));
		//	//temp.removeAll(base.getSubclasses(base.URI_EntityMention));
		//	set.removeAll(temp);
		//}
		//else if (pos.equals("verb")) {
		//	set = base.getSubclasses(base.URI_Sentiment); 
		//	set.remove(base.URI_Sentiment);
		//	HashSet<String> temp = base.getSubclasses(base.URI_EntityMention);
		//	temp.removeAll(base.getSubclasses(base.URI_ActionMention));
		//	temp.addAll(base.getSubclasses(base.URI_PropertyMention));
		//	set.removeAll(temp);
		//}
		//else {
		//	set = base.getSubclasses(base.URI_Sentiment); 
		//	set.remove(base.URI_Sentiment);
		//	HashSet<String> temp = base.getSubclasses(base.URI_EntityMention);
		//	temp.removeAll(base.getSubclasses(base.URI_PropertyMention));
		//	temp.addAll(base.getSubclasses(base.URI_ActionMention));
		//	set.removeAll(temp);
		//}

		/* Loop over all the nouns. */
		for (String word : words.keySet()) {

			double numWord = 0.0;

			/* Only increment the parent during the first loop over the reviews. */
			boolean checkWord = true;

			HashMap<String, Double> parents = new HashMap<String, Double>();
			TreeMap<Double, String> scoreParents = new TreeMap<Double, String>();


			for (String parentURI : set) {
				HashSet<String> parent = base.getLexicalizations(parentURI);

				double score = 0.0;

				double numParent = 0.0;
				double numParentCond = 0.0;

				/* Loop over all the reviews to see if they appear with the word. */
				for (Span review : reviewData.getSpans("review")){
					String revId = review.getAnnotation("id", String.class);
					JSONObject obj = wordFrequencyReview.getJSONObject(revId);

					double stars;
					try {
						stars = Double.parseDouble(review.getAnnotation("stars"));						}
					catch (NumberFormatException e) {
						stars = 0.0;
					}

					/* Update the counters. */
					double subNumerator = 0.0;
					for (String w: words.keySet()) {
						if (obj.has(w)) {
							subNumerator += (double) (int) obj.get(w); //add all frequencies of word w from each review together
						}
					}
					double frequency = 0.0;
					if (obj.has(word)) {
						frequency = (double) (int) obj.get(word);
					}
					if (subNumerator != 0.0) {
						denominator += (stars/5.0)*(frequency/subNumerator); //get weighted average stars-score for that word
					}

					/* Update the numerator with the score. */
					numerator += (stars / 5.0);

					/* Update the counters. */
					for (String par : parent) {
						if (obj.has(par)) {
							numParent++;
							break;
						} 
					}
					if (obj.has(word)) {
						if (checkWord) {
							numWord++;
						}
						for (String par : parent) {
							if (obj.has(par)) {
								numParentCond++;
								break;
							} 
						}
					}
				}
				checkWord = false;

				double wordProb = ( numParentCond / numRev ) / ( numWord / numRev );
				score += wordProb;

				/* Check if a possible parent. */
				double wordProbInv = ( numParentCond / numRev ) / ( numParent / numRev );
				if (wordProb >= threshold && wordProbInv < invThreshold ) {

					/* If the adjective is related to a word related to a parent class, double the score. */
					// if ( relatedNouns.containsKey(word) && relations) { //is relatedNouns always empty?
					//	HashSet<String> relN = relatedNouns.get(word);
					//	relN.retainAll(parent);
					//	if (!relN.isEmpty()) {
					//		//	score = 2.0 * score;
					//		while (scoreParents.containsKey(-score)) {
					//			score += 0.000000000000001;
					//		}
					//		scoreParents.put(-score, parentURI);
					//		parents.put(parentURI, score);
					//	} else {
					//		while (scoreParents.containsKey(-score)) {
					//			score += 0.000000000000001;
					//		}
					//		scoreParents.put(-score, parentURI);
					//		parents.put(parentURI, score);
					//	}
					//} else {
					while (scoreParents.containsKey(-score)) {
						score += 0.000000000000001;
					}
					scoreParents.put(-score, parentURI);
					parents.put(parentURI, score);
					//}
				}
			}
			double sentimentScore = denominator/numerator;

			/* Suggest the parents, highest score first, till one or none is accepted. */
			if (!parents.isEmpty()) {
				System.out.println("Enter 'a' to accept and 'r' to reject the relation: ");
			}
			Scanner in = new Scanner(System.in);

			/* Dependent on the score put the more likely sentiment classes first  */
			/* Give priority to the sentiment suggested by score. */
			//if (sentimentScore > 0.025) { // TODOï¼š check 0.025!!
			//		for (String URI : parents.keySet()) {
			//			double parentScore = parents.get(URI);
			//			if (URI.contains("Positive")) {
			//				//scoreParents.remove(-parentScore);
			//				//parentScore = 2.0 * parentScore;
			//				while (scoreParents.containsKey(-parentScore)) {
			//					parentScore += 0.000000000000001;
			//				}
			//				scoreParents.put(-parentScore, URI);
			//			}
			//		}
			//		System.out.println(word + " positive");
			//	} else {
			//		for (String URI : parents.keySet()) {
			//			double parentScore = parents.get(URI);
			//			if (URI.contains("Negative")) {
			//				scoreParents.remove(-parentScore);
			//				//parentScore = 2.0 * parentScore;
			//					while (scoreParents.containsKey(-parentScore)) {
			//							parentScore += 0.000000000000001;
			//						}
			//					scoreParents.put(-parentScore, URI);
			//				}
			//			}
			//			System.out.println(word + " negative");
			//		}

			/* Loop over all the possible parents, start with Generic */
			boolean accept = false;
			if  (sentimentScore > 0.025) {
				System.out.println(word + "sentimentScore : " + sentimentScore);
				String parentClass = "";
				if (pos.equals("noun")) {
					parentClass = base.URI_GenericPositiveEntity;
				}
				else if (pos.equals("verb")) {
					parentClass = base.URI_GenericPositiveAction;
				}
				else {
					parentClass = base.URI_GenericPositiveProperty;
				}
				System.out.println("relation: " + word + " --> " + parentClass);
				if (in.next().equals("a")) {
					numAcceptOverall++;
					accept = true;
					base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClass);
				} else {
					numRejectOverall++;
					/* Suggest the opposite sentiment. */
					parentClass = "";
					if (pos.equals("noun")) {
						parentClass = base.URI_GenericNegativeEntity;
					}
					else if (pos.equals("verb")) {
						parentClass = base.URI_GenericNegativeAction;
					}
					else {
						parentClass = base.URI_GenericNegativeProperty;
					}
					System.out.println("relation: " + word + " --> " + parentClass);
					if (in.next().equals("a")) {
						numAcceptOverall++;
						accept = true;
						base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClass);				
					} else {
						numRejectOverall++;
					}
				}
			} else { //sentimentScore indicates negative sentiment
				System.out.println(word + " : " + sentimentScore);
				String parentClass = "";
				if (pos.equals("noun")) {
					parentClass = base.URI_GenericNegativeEntity;
				}
				else if (pos.equals("verb")) {
					parentClass = base.URI_GenericNegativeAction;
				}
				else {
					parentClass = base.URI_GenericNegativeProperty;
				}
				System.out.println("relation: " + word + " --> " + parentClass);
				if (in.next().equals("a")) {
					numAcceptOverall++;
					accept = true;
					base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClass);									
				} else {
					numRejectOverall++;
					/* Suggest the opposite sentiment. */
					parentClass = "";
					if (pos.equals("noun")) {
						parentClass = base.URI_GenericPositiveEntity;
					}
					else if (pos.equals("verb")) {
						parentClass = base.URI_GenericPositiveAction;
					}
					else {
						parentClass = base.URI_GenericPositiveProperty;
					}
					System.out.println("relation: " + word + " --> " + parentClass);
					if (in.next().equals("a")) {
						numAcceptOverall++;
						accept = true;
						base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClass);				
					} else {
						numRejectOverall++;
					}
				}
			}

			HashSet<String> containsURI = new HashSet<String>();
			HashMap<String, String> savedParents = new HashMap<String, String>();
			for (double parentScore : scoreParents.keySet()) {
				if (!accept) { //not accepted as generic
					String finalParentURI = scoreParents.get(parentScore);		
					parentScore = -parentScore;
					String finalParentURIshort = "";
					//String group = "";
					//if (pos.equals("verb"))
					//{
					//	finalParentURIshort = finalParentURI.substring(0, finalParentURI.length()-13);
					//	//group = "Action";
					//}
					//else if (pos.equals("noun"))
					//{
					//	finalParentURIshort = finalParentURI.substring(0, finalParentURI.length()-13);
					//	//group = "Entity";
					//}
					//else
					//{
					//	finalParentURIshort = finalParentURI.substring(0, finalParentURI.length()-15);
					//	//group = "Property";
					//}
					finalParentURIshort = finalParentURI.substring(0, finalParentURI.length()-7);
					int number = 0;
					if (domain.equals("restaurant")) {
						number = 79;
					}
					else {
						number = 79;
					}
					System.out.println("Enter 'a' to accept and 'r' to reject the relation: ");
					String genericTest = finalParentURI.substring(0,number) + "Generic";
					if (!containsURI.contains(finalParentURIshort) && !finalParentURIshort.equals(genericTest)) {
						String sentiment = "";
						if (sentimentScore > 0.025)
						{
							sentiment = "Positive";
							System.out.println(word + ": "+ finalParentURIshort + sentiment );
							String inputt = in.next();
							if (inputt.equals("a")) {
								numAcceptOverall++;
								containsURI.add(finalParentURIshort);
								savedParents.put(finalParentURIshort, "p");
							} else if (inputt.equals("i")){
								numRejectOverall++;
								break;
							} else {
								numRejectOverall++;

								/* suggest opposite sentiment*/
								sentiment = "Negative";
								System.out.println(word + ": "+ finalParentURIshort + sentiment );
								inputt = in.next();
								if (inputt.equals("a")) {
									numAcceptOverall++;
									containsURI.add(finalParentURIshort);
									savedParents.put(finalParentURIshort, "n");
								} else {
									numRejectOverall++; //whole aspect category is rejected
								}
							}
						} else { //if negative sentiment is indicated by sentimentScore
							sentiment = "Negative";
							System.out.println(word + ": "+ finalParentURIshort + sentiment );
							String inputt = in.next();
							if (inputt.equals("a")) {
								numAcceptOverall++;
								containsURI.add(finalParentURIshort);
								savedParents.put(finalParentURIshort, "n");
							} else {
								numRejectOverall++;

								/* suggest opposite sentiment*/
								sentiment = "Positive";
								System.out.println(word + ": "+ finalParentURIshort + sentiment );
								inputt = in.next();
								if (inputt.equals("a")) {
									numAcceptOverall++;
									containsURI.add(finalParentURIshort);
									savedParents.put(finalParentURIshort, "p");
								} else {
									numRejectOverall++; //whole aspect category is rejected
								}
							}
						}

						//if (inputt.equals("n")) {
						//containsURI.add(finalParentURIshort);
						//	savedParents.put(finalParentURIshort, "n");

						//							} else if (inputt.equals("i")) {
						//								numRejectOverall++;
						//								break;
						//							} else {
						//								numRejectOverall++;
						//								containsURI.add(finalParentURIshort);
						//	}			
						//}
					}
				}
			}
			//System.out.println(word + " : " + "type 'p' for generic positive sentiment and 'n' for generic negative sentiment.");
			//System.out.println("Type 'i' to not add, type 'r' to reject a Generic sentiment and move on to Aspectbased sentiments.");
			//String input = in.next();
			//if (input.equals("p")) { //add to positive generic sentiment class
			//numAcceptOverall++;
			//accept = true;

			//String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), base.URI_Positive);
			//this.suggestSynonyms(word, newConcept);
			//} else if (input.equals("n")) { //add to negative sentiment class
			//numAcceptOverall++;
			//accept = true;

			/* Add the term as a subclass of the correct parent. */
			//String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), base.URI_Negative);
			//this.suggestSynonyms(word, newConcept);

			//} else if (input.equals("i")) {
			//	numRejectOverall++;
			//	accept = true;
			//} else { //input is r, so move to aspect-based
			//	numRejectOverall++;
			//}

			//type 2
			if (savedParents.size() ==1) {
				for(String P : savedParents.keySet()) { //get shortURIs
					String answer = savedParents.get(P);
					if (answer.equals("p")) {
						numAcceptOverall++;
						String parentClassName = "";
						if (pos.equals("verb"))
						{
							parentClassName = P+"PositiveAction";
						}
						else if (pos.equals("noun"))
						{
							parentClassName = P+"PositiveEntity";
						}
						else
						{
							parentClassName = P + "PositiveProperty";
						}
						String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClassName);
						//this.suggestSynonyms(word, newConcept);
					} else if (answer.equals("n")) {
						numAcceptOverall++;
						String parentClassName = "";
						if (pos.equals("verb"))
						{
							parentClassName = P+"NegativeAction";
						}
						else if (pos.equals("noun"))
						{
							parentClassName = P+"NegativeEntity";
						}
						else
						{
							parentClassName = P + "NegativeProperty";
						}
						String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClassName);
						//this.suggestSynonyms(word, newConcept);
					}
				}

			}
			else if(savedParents.size() > 1) { //type 3
				//String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), base.URI_ContextDependent);
				//for (String parent : savedParents.keySet())
				//{
				for(String P : savedParents.keySet()) {
					String answer = savedParents.get(P);
					if (answer.equals("p")) {
						numAcceptOverall++;
						String parentClassName = "";
						if (pos.equals("verb"))
						{
							parentClassName = P+"PositiveAction";
						}
						else if (pos.equals("noun"))
						{
							parentClassName = P+"PositiveEntity";
						}
						else
						{
							parentClassName = P + "PositiveProperty";
						}
						String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClassName);
						//this.suggestSynonyms(word, newConcept);
					} else if (answer.equals("n")) {
						numAcceptOverall++;
						String parentClassName = "";
						if (pos.equals("verb"))
						{
							parentClassName = P+"NegativeAction";
						}
						else if (pos.equals("noun"))
						{
							parentClassName = P+"NegativeEntity";
						}
						else
						{
							parentClassName = P + "NegativeProperty";
						}
						String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), parentClassName);
						//this.suggestSynonyms(word, newConcept);
					}
					//}
				}

				for(String P : savedParents.keySet()) { 
					String answer = savedParents.get(P);
					if (answer.equals("p")) {
						numAcceptOverall++;

						base.addClass2(base.NS + "#" + (word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()).replaceAll(" ", "") ,  P+"Mention" , base.URI_Positive);

					} else if (answer.equals("n")) {
						numAcceptOverall++;


						base.addClass2(base.NS + "#" + (word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()).replaceAll(" ", "") ,  P+"Mention" , base.URI_Negative);		

					}
				}



			}

		}
	}

	/**
	 * A method that returns the number of accepted and rejected terms.
	 * @return an array with first the number of accepted and second number of rejected terms
	 */
	public int[] getStats() {
		int[] stats = new int[3];
		stats[0] = numAcceptOverall; //change to numAcceptTerms if you only need words and no parent-relations
		stats[1] = numRejectOverall;
		return stats;
	}

	/**
	 * converts a verb to its most simple form.
	 * @param verb to be converted
	 * @return simple form of the verb
	 */
	public String verbConvertion(String verb) {

		System.setProperty("wordnet.database.dir", "C:\\Users\\HP\\Documents\\Advanced programming\\.metadata\\.plugins\\org.eclipse.ltk.core.refactoring\\.refactorings\\.workspace\\absa_software\\target\\classes\\externalData\\WordNet-3.0\\dict");
		WordNetDatabase database = WordNetDatabase.getFileInstance();

		Morphology id = Morphology.getInstance();

		String[] arr = id.getBaseFormCandidates(verb, SynsetType.VERB);
		if (arr.length>0) {
			return arr[0];
		}
		return verb;

	}

	/**
	 * Save the built ontology.
	 * @param file, the name of the file to which to save the ontology
	 */
	public void save(String file) {
		base.save(file);
	}
}