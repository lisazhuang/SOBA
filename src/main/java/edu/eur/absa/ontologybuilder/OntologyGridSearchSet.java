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
//import edu.eur.absa.seminarhelper.Synonyms;
//import edu.eur.absa.seminarhelper.SeminarOntology;
//import edu.eur.absa.seminarhelper.WordSenseDisambiguation;
//import edu.eur.absa.seminarhelper.Wu_Palmer;
//import edu.eur.absa.seminarhelper.readJSON;
import edu.eur.absa.seminarhelper.*;
import edu.cmu.lti.ws4j.*;

/**
 * A method that builds an ontology semi-automatically.
 * 
 * @author Karoliina Ranta
 *
 */
public class OntologyGridSearchSet {
	
	/* The base ontology. */
	private SeminarOntology base;
	private HashMap<String, HashSet<String>> aspectCategories;
	private String domain;
	private Dataset reviewData;
	private JSONObject wordFrequencyReview;
	private JSONObject wordFrequencyDocument;
	private HashMap<String, HashMap<String, Integer>> contrastData;
	private int numReject;
	private int numVerbsReject;
	private int numNounsReject;
	private int numAdjectivesReject;
	private int numAccept;
	private int numVerbsAccept;
	private int numAdjectivesAccept;
	private int numNounsAccept;
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
	public OntologyGridSearchSet(SeminarOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, double thres, double frac, boolean r) {
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
	public OntologyGridSearchSet(SeminarOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, double thres, double invThres, double frac, boolean r) {
		
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
						String positivePropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveProperty", true, entity.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Positive);
						String negativePropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeProperty", true, entity.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI,  base.URI_Negative);
						String positiveActionClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveAction", true, entity.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Positive);
						String negativeActionClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeAction", true, entity.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Negative);
						String positiveEntityClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveEntity", true, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_Positive);
						String negativeEntityClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeEntity", true, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_Negative);

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
							String positivePropertyURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveProperty", true, aspectName.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Positive);
							String negativePropertyURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeProperty", true, aspectName.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Negative);
							String positiveActionURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveAction", true, aspectName.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Positive);
							String negativeActionURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeAction", true, aspectName.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Negative);
							String positiveEntityURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveEntity", true, aspectName.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_Positive);
							String negativeEntityURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeEntity", true, aspectName.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_Negative);					
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
				//this.suggestSynonyms("food", FoodMentionClassURI);
				String DrinksMentionClassURI = base.addClass("drinks#noun#1", "DrinksMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "#SustenanceMention");
				//this.suggestSynonyms("drinks", DrinksMentionClassURI);

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
	 * @throws IOException 
	 * @throws IllegalSpanException 
	 * @throws JSONException 
	 * @throws ClassNotFoundException 
	 */
	public double findTerms(boolean nn, boolean adj, boolean vrb, double alpha, double beta, HashMap<String, Double> acceptedWords) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException {

		HashMap<String, Double> nouns = new HashMap<String, Double>(); 
		HashMap<String, Double> adjectives = new HashMap<String, Double>(); 
		HashMap<String, Double> verbs = new HashMap<String, Double>();
		
		double numReject = 0;
		double numAccept = 0;
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
		
		int max_length = 0;
		
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
			int wordsToSkip = 0;
			/* Loop over all words in the review */
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
				//if (wordDatabase.getSynsets(candidateMultiLemma).length < 0) //the multi-word-lemma is found in WordNet!
				//	multiWord = candidateMultiLemma;
				Word nextWord = word;

				boolean proceed = true;
				//int counter = 0;
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
				if (!multiWord.equals(word.toString()) && !multiWord.equals(lemma)){ // if multiword of more than 1 word found, change lemma. Otherwise, lemma is just the original 1-word
					lemma = multiWord;
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


				if (lemma.contains(" "))
				{
					wordFrequencies = TermFrequencyYelp.getFrequency(lemma);
				}

				/* Calculate the domain pertinence. */
				double domainFreq = 0.0;
				if (lemma.contains(" ")) //if the lemma is a multi-word phrase, wordfrequency still has to be calculated
				{
					HashMap<String, HashMap<String,Integer>> wordFreqDocCast = wordFrequencies.get("wordFrequencyDocumentCast");
					HashMap<String,Integer> wordFreqDoc = wordFreqDocCast.get("wordFrequencyDocument"); 
					Integer ints = wordFreqDoc.get(lemma);
					domainFreq = (double) ints;
				}
				else
				{
					if (wordFrequencyDocument.has(lemma)) {
						domainFreq = (double) (int) wordFrequencyDocument.get(lemma);
					}
				}

				/* Find the maximum frequency of lemma in the contrasting corpus. */
				double contrastFreq = 0.0;
				if (lemma.contains(" ")) //if the lemma is a multi-word phrase, wordfrequency still has to be calculated
				{


					/* For each contrast text, calculate frequency and store max*/
					for (String name : contrastTexts.keySet()) // for each novel
					{
						Integer phraseFreq = 0; // number of times the lemma phrase appears
						String text = contrastTexts.get(name).toString(); // get the novel text as String
						int index = text.indexOf(lemma);
						while (index != -1) {
							phraseFreq = phraseFreq +1;  
							text = text.substring(index + 1);
							index = text.indexOf(lemma);
						}
						if (phraseFreq > contrastFreq)
						{
							contrastFreq = phraseFreq;
						}
					}

				}
				else {
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


				double maxDomainFreq = 0.0;
				if (lemma.contains(" ")) //if multiwordphrase
				{
					/* Find the maximum frequency of lemma across the reviews. */
					for (String revId : wordFrequencies.get("wordFrequencyReview").keySet())
					{
						if (wordFrequencyReview.getJSONObject(revId).has(lemma))
						{
							double revFreq = (double) (int) wordFrequencyReview.getJSONObject(revId).get(lemma);
							if (revFreq > maxDomainFreq) {
								maxDomainFreq = revFreq;
							}
						}
					}

					/* Loop over all the reviews. */
					for (String revId2 : wordFrequencies.get("wordFrequencyReview").keySet()) {
						if (wordFrequencyReview.getJSONObject(revId2).has(lemma)) {
							double frequency = (double) (int) wordFrequencyReview.getJSONObject(revId2).get(lemma);
							double normFreq = frequency / maxDomainFreq;
							DC += -1 * (normFreq) * Math.log(normFreq);
						}
					}
				}
				else
				{
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

						//store context for WSD
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
						Word word3 = endWord;
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
						Word word3 = endWord;
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
						//store context for WSD
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
						Word word3 = endWord;
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
			/* Calculate the scores. */
			Double[] scores = new Double[verbs.size()];
			int i = 0;
			for (String w : verbs.keySet()) { //TODO: what is alpha and beta?
				double score = alpha * (DPs.get(w) / maxDP) + beta * (DCs.get(w) / maxDC);
				verbs.put(w, score);
				scores[i] = score;
				i++;
			}
			/* Find the threshold value to get only the top n% of the terms. */
			Arrays.sort(scores);
			double ind = fraction * verbs.size(); //TODO: what is fraction?
			int index = (int) ind;			
			double scoreThreshold = scores[verbs.size() - 1 - index];	
			
			for (String w : verbs.keySet()) {
				double score = verbs.get(w);
				if (score > scoreThreshold) {
					if (acceptedWords.containsKey(w)) { //aspectverb TODO: what is accpetedWords?
						numVerbsAccept++;
						numAccept++;
					} else {
						numVerbsReject++;
						numReject++;
					}
				}
			}
		}
		if (nn) {
			/* Calculate the scores. */
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


			for (String w : nouns.keySet()) {
				double score = nouns.get(w);
				if (score > scoreThreshold) {
					if (acceptedWords.containsKey(w)) {
						HashSet<String> a = new HashSet<String>(); 	//added
						a.add(w);									//added
						numNounsAccept++;
						numAccept++;
						nouns.put(w, 0.0);
						//for (String w2 : nouns.keySet()) {
						//	double score2 = nouns.get(w2);
						//	if (score2 > scoreThreshold) {
						//		double r = Wu_Palmer.WupSimilarity(w,nounSenses.get(w),w2,nounSenses.get(w2),"n");
						//		if (r>0.88) {
						//			numAccept++;
						//			nouns.put(w2, 0.0);
						//		}
						//	}
						//}
					} else {
						numNounsReject++; //reject all other comparable words.
						numReject++;
						nouns.put(w, 0.0);
						//for (String w2 : nouns.keySet()) {
						//	double score2 = nouns.get(w2);
						//	if (score2 > scoreThreshold) {
						//		double r = Wu_Palmer.WupSimilarity(w,nounSenses.get(w),w2,nounSenses.get(w2),"n");
						//		if (r>0.88) {
						//			numReject++;
						//			nouns.put(w2, 0.0);
						//		}
						//	}
						//}
					}
				}
			}
		}
		if (adj) {
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
			
			for (String w : adjectives.keySet()) {
				double score = adjectives.get(w);
				if (score > scoreThreshold) {
					if (acceptedWords.containsKey(w)) {
						numAdjectivesAccept++;
						numAccept++;
					} else {
						numAdjectivesReject++;
						numReject++;
					}
				}
			}
			
		}

		System.out.println(alpha + " && " + beta + " // numaccept=" +numAccept+ " // numreject=" + numReject + " // ratio="+(numAccept/(numReject+numAccept)) );
		return (numAccept/(numReject+numAccept));
				
	}
	
	/**
	 * A method that returns the number of accepted and rejected terms.
	 * @return an array with first the number of accepted and second number of rejected terms
	 */
	public int[] getStats() {
		int[] stats = new int[9];
		stats[0] = numAccept;
		stats[1] = numReject;
		stats[2] = numVerbsAccept;
		stats[3] = numVerbsReject;
		stats[4] = numNounsAccept;
		stats[5] = numNounsReject;
		stats[6] = numAdjectivesAccept;
		stats[7] = numAdjectivesReject;
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