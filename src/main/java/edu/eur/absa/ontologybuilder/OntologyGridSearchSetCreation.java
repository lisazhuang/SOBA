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
import edu.eur.absa.seminarhelper.*;
import edu.cmu.lti.ws4j.*;

/**
 * A method that builds an ontology semi-automatically.
 * 
 * @author Karoliina Ranta
 *
 */
public class OntologyGridSearchSetCreation {
	
	/* The base ontology. */
	private SeminarOntology base;
	private HashMap<String, HashSet<String>> aspectCategories;
	private String domain;
	private Dataset reviewData;
	private JSONObject wordFrequencyReview;
	private JSONObject wordFrequencyDocument;
	private HashMap<String, HashMap<String, Integer>> contrastData;
	private int numReject;
	private int numAccept;
	private int numRev;
	private HashSet<String> remove;
	private double threshold;
	private double invThreshold;
	private double fraction;
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
	public OntologyGridSearchSetCreation(SeminarOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, double thres, double frac, boolean r) {
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
	public OntologyGridSearchSetCreation(SeminarOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, double thres, double invThres, double frac, boolean r) {
		
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
		numReject = 0;
		numAccept = 0;
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


		//add synonyms of verbs hating and enjoy in the the general positive and general negative aspect categories
		String negativeActionURI1 = base.addClass("dislike#verb#1", "Dislike", true, "dislike", new HashSet<String>(), base.URI_GenericNegativeAction);
		//this.suggestSynonyms("dislike", negativeActionURI1);
		String negativeActionURI2 = base.addClass("loathe#verb#1", "Loathe", true, "loathe", new HashSet<String>(), base.URI_GenericNegativeAction);
		//this.suggestSynonyms("loathe", negativeActionURI2);
		String positiveActionURI1 = base.addClass("enjoy#verb#1", "Enjoy", true, "enjoy", new HashSet<String>(), base.URI_GenericPositiveAction);
		//this.suggestSynonyms("enjoy", positiveActionURI1);
		String positiveActionURI2 = base.addClass("appreciate#verb#1", "Appreciate", true, "appreciate", new HashSet<String>(), base.URI_GenericPositiveAction);
		//this.suggestSynonyms("appreciate", positiveActionURI2);
		//TODO: add love and hate and like, and also maybe for properties and entities?
		/* Loop over the aspect category entities. */

		//create a hashmap with synsets as value of the entities (key), and add as synset property during loop

		HashMap<String, String> entitySynsets = new HashMap<String, String>();
		entitySynsets.put("ambience", "ambience#noun#1");
		entitySynsets.put("service", "service#noun#15");
		entitySynsets.put("restaurant", "restaurant#noun#1");
		entitySynsets.put("location", "location#noun#1");
		entitySynsets.put("sustenance", "sustenance#noun#1"); //add drinks and food to sustenance

		for (String entity : aspectCat.keySet()) {
			HashSet<String> aspectSet = aspectCat.get(entity);
			/* Each entity should have its own AspectMention class. */
			HashSet<String> aspects = new HashSet<String>();
			String synset = entitySynsets.get(entity);
			for (String aspect : aspectSet) {

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

				//	this.suggestSynonyms(entity, newClassURI);
			} else {
				//	this.suggestSynonyms(entity, newClassURI);
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
					//this.suggestSynonyms(aspectName, newClassURIAspect);

					if (aspectName.contains("&")) {
						HashSet<String> lexs = new HashSet<String>();
						String[] parts = aspectName.split("&");
						lexs.add(parts[0]);
						lexs.add(parts[1]);
						base.addLexicalizations(newClassURIAspect, lexs);
						//this.suggestSynonyms(parts[0], newClassURIAspect);
						//this.suggestSynonyms(parts[1], newClassURIAspect);
					}
					if (aspectName.contains("_")) { 
						HashSet<String> lexs = new HashSet<String>();
						String[] parts = aspectName.split("_");
						lexs.add(parts[0]);
						lexs.add(parts[1]);
						base.addLexicalizations(newClassURIAspect, lexs);
						//this.suggestSynonyms(parts[0], newClassURIAspect);
						//this.suggestSynonyms(parts[1], newClassURIAspect);
					}
				}
			}			
		}

		//add Food and DrinksMention to Sustenance Class

		String FoodMentionClassURI = base.addClass("food#noun#1", "FoodMention",true, "food", aspectCat.get("sustenance"), base.NS + "#SustenanceMention");
		String FoodMentionActionClassURI = base.addClass("food#noun#1", "FoodActionMention",true, "food", aspectCat.get("sustenance"), base.NS + "#SustenanceActionMention");
		String FoodMentionPropertyClassURI = base.addClass("food#noun#1",  "FoodPropertyMention", true, "food", aspectCat.get("sustenance"), base.NS + "#SustenancePropertyMention");
		//this.suggestSynonyms("food", FoodMentionClassURI, FoodMentionActionClassURI, FoodMentionPropertyClassURI);
		String DrinksMentionClassURI = base.addClass("drinks#noun#1", "DrinksMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "#SustenanceMention");
		String DrinksMentionActionClassURI = base.addClass("drinks#noun#1", "DrinksActionMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "#SustenanceActionMention");
		String DrinksMentionPropertyClassURI = base.addClass("drinks#noun#1", "DrinksPropertyMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "SustenancePropertyMention");
		//this.suggestSynonyms("drinks", DrinksMentionClassURI, DrinksMentionActionClassURI, DrinksMentionPropertyClassURI);

		//add a few extra EntityMention classes
		//ExperienceMention
		HashSet<String> experienceAspects = new HashSet<String>();
		experienceAspects.add("RESTAURANT#MISCELLANEOUS");
		String ExperienceMentionClassURI = base.addClass("experience#noun#3", "Experience" + "Mention", true, "experience", experienceAspects, base.URI_EntityMention);
		//this.suggestSynonyms("experience", ExperienceMentionClassURI);
		//PersonMention
		HashSet<String> personAspects = new HashSet<String>();
		String PersonMentionClassURI = base.addClass("person#noun#1", "Person" + "Mention", true, "person", personAspects, base.URI_EntityMention);
		//this.suggestSynonyms("person", PersonMentionClassURI);
		//TimeMention
		HashSet<String> timeAspects = new HashSet<String>();
		String TimeMentionClassURI = base.addClass("time#noun#2", "Time" + "Mention", true, "time", timeAspects, base.URI_EntityMention);
		//this.suggestSynonyms("time", TimeMentionClassURI);
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
			
			/* Read in the restaurant review data. */
			reviewData =  (new DatasetJSONReader()).read(new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_review_restaurant_auto5001.json"));	
			
			/* Load the review and document frequencies of all appearing words. */
			try {
				this.wordFrequencyReview = readJSON.readJSONObjectFile(Framework.EXTERNALDATA_PATH + "wordFrequencyReviewYelpAuto5001.json");
			} catch (ClassNotFoundException | JSONException | IOException e) {
				e.printStackTrace();
			}
			try {
				this.wordFrequencyDocument = readJSON.readJSONObjectFile(Framework.EXTERNALDATA_PATH + "wordFrequencyDocumentYelpAuto5001.json");
			} catch (ClassNotFoundException | JSONException | IOException e) {
				e.printStackTrace();
			}
		} else if (domain.equals("laptop")) {
			 
			/* Read in the laptop review data. */
			reviewData =  (new DatasetJSONReader()).read(new File(Framework.EXTERNALDATA_PATH+"amazon_review_laptop5001.json"));	
			
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
	 */
	public HashMap<String, Double> findTerms(boolean nn, boolean adj, boolean vrb, double alpha, double beta) {
		
		HashMap<String, Double> nouns = new HashMap<String, Double>(); 
		HashMap<String, Double> adjectives = new HashMap<String, Double>(); 
		HashMap<String, Double> verbs = new HashMap<String, Double>();
		
		HashMap<String, Integer> nounSenses = new HashMap<String, Integer>(); 
		HashMap<String, String> nounContext = new HashMap<String, String>(); 
		
		HashMap<String, Double> aspectNouns = new HashMap<String, Double>();
		HashMap<String, Double> sentimentNouns = new HashMap<String, Double>();
		HashMap<String, Double> aspectVerbs = new HashMap<String, Double>();
		HashMap<String, Double> sentimentVerbs = new HashMap<String, Double>();
		HashMap<String, Double> aspectAdjectives = new HashMap<String, Double>();
		HashMap<String, Double>	sentimentAdjectives = new HashMap<String, Double>();
		
		relatedNouns = new HashMap<String, HashSet<String>>();
		nounsWithSynset = new HashMap<String, HashSet<String>>();
		
		HashMap<String, Double> DPs = new HashMap<String, Double>(); 
		HashMap<String, Double> DCs = new HashMap<String, Double>(); 

		double maxDP = 0.0;
		double maxDC = 0.0;
		
		/* Loop over all the reviews in the dataset. */
		for (Span review : reviewData.getSpans("review")){
			
			/* Loop over all words in the review */
			Span scope = review.getTextualUnit();
			for(Word word : scope){
				
				/* Loop over all the words to find the types. */
				String pos = word.getAnnotation("pos");
				String lemma = word.getLemma();
				
				if (vrb && (pos.equals("VB")||pos.equals("VBD")||pos.equals("VBG")||pos.equals("VBN")||pos.equals("VBN")||pos.equals("VBP"))) {
					lemma = verbConvertion(lemma);
				}
				if (lemma == null) {
					continue;
				}
				if (lemma.length() < 2 ) {
					continue;
				}
				
				/* Calculate the domain pertinence. */
				double domainFreq = 0.0;
				if (wordFrequencyDocument.has(lemma)) {
					domainFreq = (double) (int) wordFrequencyDocument.get(lemma);
				}
				
				/* Find the maximum frequency of lemma in the contrasting corpus. */
				double contrastFreq = 0.0;
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
				
				/* Find the maximum frequency of lemma across the reviews. */
				double maxDomainFreq = 0.0;
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
				
				DCs.put(lemma, DC);
				
				/* Find the maximum domain consensus score. */
				if (DC > maxDC) {
					maxDC = DC;
				}
				
				/* If DC DP are not both 0.0 add it to nouns/adjectives. */
				if (!(DP == 0.0 && DC == 0.0)) {
					if (adj && (pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS"))) {
						adjectives.put(lemma, 0.0);
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
					} else if (nn && (pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") )){
						nouns.put(lemma, 0.0);
						int i = 0;
						String text = lemma;
						Word word2 = word;
						while (word2.hasPreviousWord() && i<5) {
							String pos1 = word2.getPreviousWord().getAnnotation("pos");
							if(pos1.equals("NN") || pos1.equals("NNS") || pos1.equals("NNP") || pos1.equals("VB")||pos1.equals("VBD")||pos1.equals("VBG")||pos1.equals("VBN")||pos1.equals("VBN")||pos1.equals("VBP")||pos1.equals("JJ")) {
							    String lemma2 = word2.getPreviousWord().getAnnotation("lemma", String.class);
								text = text +" "+ lemma2;
								word2 = word2.getPreviousWord();
								i++;
							}
							else {
								word2 = word2.getPreviousWord();
								i++;
							}

	
						}
						Word word3 = word;
						while (word3.hasNextWord() && i<10) {
							String pos2 = word3.getNextWord().getAnnotation("pos");
							if(pos2.equals("NN") || pos2.equals("NNS") || pos2.equals("NNP") || pos2.equals("VB")||pos2.equals("VBD")||pos2.equals("VBG")||pos2.equals("VBN")||pos2.equals("VBN")||pos2.equals("VBP")||pos2.equals("JJ")) {
								String lemma3 = word3.getNextWord().getAnnotation("lemma", String.class);
								text = text +" "+ lemma3;
								word3 = word3.getNextWord();
								i++;
							}
							else {
								word3 = word3.getNextWord();
								i++;
							}

						}
						
						if(nounContext.containsKey(lemma)) {
							nounContext.put(lemma, nounContext.get(lemma)+" "+text);
						}
						
						else {
							nounContext.put(lemma, text);
						}
						
						
					} else if (vrb && (pos.equals("VB")||pos.equals("VBD")||pos.equals("VBG")||pos.equals("VBN")||pos.equals("VBN")||pos.equals("VBP"))) {
						verbs.put(lemma, 0.0);
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
			double ind = fraction * verbs.size();
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
						numAccept++;
					} else if (input.equals("s")) { //sentimentVerbs
						sentimentVerbs.put(w, score);
						numAccept++;
					} else {
						numReject++;
					}
				}
			}
		}
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
			double ind = fraction * nouns.size();
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
						if (input.equals("a")) { //aspectverb
							aspectNouns.put(w, score);
							numAccept++;
						} else if (input.equals("s")) { //sentimentVerbs
							sentimentNouns.put(w, score);
							numAccept++;
						} else {
							numReject++;
						}
					}
				}
			}
		}
		//aspectNouns.putAll(aspectVerbs);
//		this.subsumptionAspect(aspectNouns);
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
			double ind = fraction * adjectives.size();
			int index = (int) ind;			
			double scoreThreshold = scores[adjectives.size() - 1 - index];	
			
			System.out.println("Enter 'a' to accept and 'r' to reject the adjective: ");
			Scanner in = new Scanner(System.in);
			
			for (String w : adjectives.keySet()) {
				double score = adjectives.get(w);
				if (score > scoreThreshold) {
					System.out.println("adjective: " + w);
					String input = in.next();
					if (input.equals("a")) { //aspectverb
						aspectAdjectives.put(w, score);
						numAccept++;
					} else if (input.equals("s")) { //sentimentVerbs
						sentimentAdjectives.put(w, score);
						numAccept++;
					} else {
						numReject++;
					}
				}
			}
			
		}
//		acceptedAdjectives.putAll(sentimentVerbs);
//		acceptedAdjectives.putAll(sentimentNouns);
//		this.subsumptionSentiment(acceptedAdjectives);
		aspectAdjectives.putAll(sentimentVerbs);
		aspectAdjectives.putAll(sentimentNouns);
		aspectAdjectives.putAll(sentimentAdjectives);
		aspectAdjectives.putAll(aspectNouns);
		aspectAdjectives.putAll(aspectVerbs);
		return aspectAdjectives;
	}
	
	/**
	 * A method that uses the subsumption method to determine the Aspect superclasses of the accepted terms
	 * and adds them to the ontology.
	 * @param words, the accepted terms to be added to the ontology.
	 */
	public HashMap<String, HashSet<String>> subsumptionAspect(HashMap<String, Double> words) {		//yes to noun, pos = "noun" or "verb" or "adjective"
		HashSet<String> set;
		HashMap<String, HashSet<String>> acceptedRelations = new HashMap<String, HashSet<String>>();
		int numAcceptOverall = numAccept;
		int numRejectOverall = numReject;
		//set = base.getSubclasses(base.URI_SentimentMention);
		//set.remove(base.URI_SentimentMention);
		//HashSet<String> temp = base.getSubclasses(base.URI_AspectMention);
		//temp.removeAll(base.getSubclasses(base.URI_SentimentMention));
		//set.removeAll(temp);

		//if (pos.equals("noun")) { //TODO: check of je gewoon alle apsectEntityMentions krijgt, en same voor action and property
			set = base.getSubclasses(base.URI_EntityMention); 
			set.remove(base.URI_EntityMention);
			HashSet<String> temp = base.getSubclasses(base.URI_ActionMention);
			temp.addAll(base.getSubclasses(base.URI_PropertyMention));
			temp.addAll(base.getSubclasses(base.URI_Sentiment));
			//temp.removeAll(base.getSubclasses(base.URI_EntityMention));
			set.removeAll(temp);
		//}
		//else if (pos.equals("verb")) {
		//	set = base.getSubclasses(base.URI_ActionMention); 
		//	set.remove(base.URI_ActionMention);
		//	HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
			//HashSet<String> temp = base.getSubclasses(base.URI_EntityMention);
			//temp.addAll(base.getSubclasses(base.URI_PropertyMention));
			//temp.removeAll(base.getSubclasses(base.URI_ActionMention));
		//	set.removeAll(temp);
		//}
		//else {
		//	set = base.getSubclasses(base.URI_PropertyMention); 
		//	set.remove(base.URI_PropertyMention);
		//	HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
		//	set.removeAll(temp);
		//}

		/* Loop over all the accepted words. */
		for (String word : words.keySet()) {
			HashSet<String> acceptedParents = new HashSet<String>();
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
						acceptedParents.add(finalParentURI);
						//System.out.println("Does this concept carry sentiment? Type 'p' if strictly positive and 'n' if stricly negative, 'r' if else");
						//String input_sent = in.next();
						//if (input_sent.equals("positive"))
						//{
						//if (nounsWithSynset.keySet().contains(word)) {
						//	for (String wInSynset : nounsWithSynset.get(word)) {
						//		String newConcept = base.addClass(pos, context, wInSynset.substring(0, 1).toUpperCase() + wInSynset.substring(1).toLowerCase(), true, wInSynset, new HashSet<String>(), finalParentURI);
								//this.suggestSynonyms(wInSynset, newConcept);
						//	}
						//}
						//else {
						//	String newConcept = base.addClass(pos, context, word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(), true, word, new HashSet<String>(), finalParentURI);
							//this.suggestSynonyms(word, newConcept);
						//}
						// for type three we disregard breaking, but add to more parent classes break;
					} else if (input.equals("i")) {
						numRejectOverall++;
						break;
					} else { numRejectOverall++;}
				}
			}
			acceptedRelations.put(word, acceptedParents);
		}
		return acceptedRelations;
		}
	


	
	
	/**
	 * A method that returns the number of accepted and rejected terms.
	 * @return an array with first the number of accepted and second number of rejected terms
	 */
	public int[] getStats() {
		int[] stats = new int[3];
		stats[0] = numAccept;
		stats[1] = numReject;
		return stats;
	}
	
	/**
	 * converts a verb to its most simple form.
	 * @param verb to be converted
	 * @return simple form of the verb
	 */
	public String verbConvertion(String verb) {
		 
		System.setProperty("wordnet.database.dir", "C:\\Users\\LZ\\Lisa\\SOFAS_software\\absa_software\\target\\classes\\externalData\\WordNet-3.0\\dict");
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