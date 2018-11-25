package edu.eur.absa.algorithm.seminar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.CVParameterSelection;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.SparseInstance;


import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm2;
import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.data.SeminarWriter;
import edu.eur.absa.evaluation.evaluators.AnnotationLabelEvaluatorRestaurant;
import edu.eur.absa.external.NRCReviewSentimentLexicon;
import edu.eur.absa.model.Annotatable;
import edu.eur.absa.model.Annotations;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Relations;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.seminarhelper.Synonyms;
import edu.eur.absa.seminarhelper.SeminarOntology;

import static java.lang.Math.pow;


/**
 * This is the sentiment classification algorithm for the restaurant domain. It loops over all the words in a review-level notion and determines features.
 * (uses the semi-automatically constructed ontology)
 * 
 * Features:
 * 	applySentence, sets scope to sentence (always apply when considering sentence level)
 * 	applyCategory, adds the aspect category as a feature
 *	applyWords, uses the presence of words as a feature
 *	applyStanfordReviewSentiment, adds the sentiment score of the review
 *	applyStanfordSentiment, uses the sentiment score of a word as determined by Stanford
 *	applyNRCSentiment, uses the NRC sentiment word scores
 *	applyOntology, adds the ontology concepts related to the currect aspect
 *	applyNegation, checks whether a word is being negated
 * 
 * @author Karoliina Ranta
 * Adapted by Suzanne Veltman and Abbaan Nassar
 */
public class SeminarAlgorithmRestaurant extends AbstractAlgorithm2 {
	/* Weka. */
	private Instances wekaData;
	private SMO model;
	private Evaluation eval;
	private HashMap<Span, Instance> wekaInstances = new HashMap<>();

	private ArrayList<String> classLabels = new ArrayList<String>();
	private SeminarOntology ont;

	private NRCReviewSentimentLexicon unigram;
	
	private int k; // The number of previous words to check for negation.
	private int timesnegatedtotal = 0;
	private String ontology;
	
	/**
	 * Constructor for seminarAlogrithm
	 * @param analysisSpan, the span type used to split the data (review)
	 * @param evalAnalysisSpan, the span type used to analyse the data (notion/'opinion')
	 * @param failureAnalysis, boolean to perform failureAnalysis
	 * @param printOutput, boolean for writing evaluation to a file
	 * @param num, the number of previous words to check for negation
	 */
	public SeminarAlgorithmRestaurant(String analysisSpan, String evalAnalysisSpan, boolean failureAnalysis, boolean printOutput, int num, String ont) {
		super("SeminarAlgorithm", analysisSpan);
		
		/*Write output*/
		if (printOutput) {
			evaluators.add(new AnnotationLabelEvaluatorRestaurant(evalAnalysisSpan, "polarity", failureAnalysis,  new SeminarWriter()));
		}
		else {
			evaluators.add(new AnnotationLabelEvaluatorRestaurant(evalAnalysisSpan, "polarity", failureAnalysis));
		}
				
		/*Load in NRC unigram*/
		unigram = new NRCReviewSentimentLexicon(NRCReviewSentimentLexicon.RESTAURANTS_UNIGRAM);
		
		k = num;
		
		ontology = ont;
	}

	@Override
	/**
	 * This method cleans the Algorithm for the next run
	 */
	protected void cleanAlgorithm() {
		Framework.log(getLabel() + " ++Cleaning...++");
		model = null;
		eval = null;
	}

	@Override
	/**
	 * Preprocessing of the data: create weka readable data
	 */
	public void preprocess() {		
		Framework.log(getLabel() + " ++Preprocessing...++ ");
		
		/*Load in the ontology*/
		if (hasProperty("applyOntology") || hasProperty("applyOntologySentiment")) {
			ont = new SeminarOntology(Framework.EXTERNALDATA_PATH + ontology);
		}

		/* Create the Attribute list and the map of the Instances */
		HashMap<String, Attribute> allAttributes = new HashMap<>();
		HashMap<Span, HashMap<Attribute, Double>> allInstances = new HashMap<>();
		HashMap<Span, String> goldValues = new HashMap<>();
		
		/*Use Stanford Sentiment tool for review sentiment scores */
		if (hasProperty("applyStanfordReviewSentiment")) {
			allAttributes.put("stanfordReviewSentiment", new Attribute("stanfordReviewSentiment"));
		}
		
		/* Loop over all the spans in the data set */
		for (Span sentence : getCombinedData()) {
			/* Get the opinion spans in the current review */
			for (Span span : sentence.getDataset().getSpans(sentence, "opinion")) {
				/* Create variable to keep track of the values of the attributes in this span */
				HashMap<Attribute, Double> spanInstance = new HashMap<>();
				allInstances.put(span, spanInstance);
	
				/* Gather the gold value of the polarity of the span */
				if (span.hasAnnotation("polarity")) {
					goldValues.put(span, span.getAnnotation("polarity", String.class));
				}
	
				/* Set the scope of the span */
				Span scope;
				if (hasProperty("applySentence")) {
					scope = span.getTextualUnit();
				} else {
					scope = span;
				}
				
				/*Get the category*/
				String category = span.getAnnotation("category", String.class);
				
				/* Use category as attribute, set value equal to 1.0 */
				if (hasProperty("applyCategory")) {
					allAttributes.putIfAbsent(category, new Attribute(category));
					spanInstance.put(allAttributes.get(category), 1.0);
				}
				
				/* Loop over the words within the scope/span */
				for (Word word : scope) {
					Annotations ann = word.getAnnotation();
	
					if (hasProperty("applyWords")) {
						String attribute = ann.getEntryText("lemma");
	
						if (!allAttributes.containsKey(attribute)) {
							allAttributes.putIfAbsent(attribute, new Attribute(attribute));					
						}	
							
						double instanceValue = 0.0;
						double dicts = 0.0;
						if (hasProperty("applyStanfordSentiment")){
							double sentiment = getSentimentScore(word.getAnnotation("phraseSentiment", ArrayList.class));
							instanceValue += sentiment;
							dicts++;
						}
						if (hasProperty("applyNRCSentiment")){
							double sentimentWeight = unigram.getScore(word.getWord()); //use word or lemma? word.getAnnotations().get("lemma", String.class)
							instanceValue += sentimentWeight;
							dicts++;
						}
							
						/* The instance value is set equal to 1.0 if sentiment scores are not used. */
						if (!hasProperty("applyNRCSentiment") && !hasProperty("applyStanfordSentiment") && !hasProperty("applyOntologySentiment")){
							instanceValue = 1.0;
						}
						/* Calculate the average sentiment score. */
						if (dicts > 0) {
							instanceValue /= dicts;
						}
						
						/* Set the instance value. */
						spanInstance.put(allAttributes.get(ann.getEntryText("lemma")), instanceValue);
				
					}
					/* Use the ontology to create new attributes. */
					if (hasProperty("applyOntology") && word.hasAnnotation("URI") && !word.getAnnotation("URI").equals("null")) {
						
						/* Check whether the word is negated. */
						boolean negated = false;
						if (hasProperty("applyNegation")) {
							/* Check if there is a negation relation related to this word. */
							for (Relation rel : word.getRelations().getAllRelationsToChildren()) {
								Annotations test3 = rel.getAnnotations();
								String test5 = test3.get("relationShortName");
								if (test5 != null && test5.equals("neg")) {
									negated = true;
								}
							}
							boolean punctuation = false;
							int negatedtimes=0;
							Word word2= word;
							while (word2.hasPreviousWord() && !punctuation){
								Word prev = word2.getPreviousWord();
								String prevLemma = prev.getAnnotation("lemma", String.class);
								if (prevLemma.equals("and") || prevLemma.indexOf(".")!=-1 ||prevLemma.indexOf(",")!=-1 || prevLemma.indexOf("/")!=-1 || prevLemma.indexOf("!")!=-1 || prevLemma.indexOf("?")!=-1 || prevLemma.indexOf(";")!=-1){
									punctuation=true;
								}
								else if(prevLemma.equals("too") || prevLemma.equals("none") || prevLemma.equals("nobody")|| prevLemma.equals("never") ||  prevLemma.equals("nothing") || prevLemma.equals("no") || prevLemma.indexOf("n't")!=-1) {
									negatedtimes++;
								}
								word2 = prev;
							}
							if (negatedtimes==1) {
								negated = true;
								timesnegatedtotal++;
								//System.out.println("timesnegatedtotal = "+ timesnegatedtotal + "::"+word.getLemma()+" :: " + word.getTextualUnit().getAnnotations().get("id"));
							}
						}
						
						/*Add all the superclasses of the word to the set*/
						String URI = word.getAnnotation("URI");
						HashSet<String> superclasses = new HashSet<String>(); 
						superclasses.addAll(ont.getSuperclasses(URI));
						//System.out.println(word.getLemma() + " :: " + word.getAnnotations().get("URI") + " :: " + superclasses);
						HashSet<String> foundURIs = new HashSet<String>();
						
						/*Check if the word is a subclass of 'sentimentMention'. */
						if (superclasses.contains(ont.URI_SentimentMention)) {
							/* Check if the word is a subclass of 'aspectMention' --> aspect dependent sentiment. */
							if (superclasses.contains(ont.URI_AspectMention)) {
								/* Check whether the word relates to the current aspect category. */
								HashSet<String> superclasses2 = ont.getSuperclasses(URI);
								HashSet<String> categoryURIs = ont.getLexicalizedConcepts(null, ont.getNS()+"#aspect", category); 
								superclasses2.retainAll(categoryURIs);
								if (!superclasses2.isEmpty()){
									/* The aspect dependent sentiment is related to the current aspect. */
									/* If the word is negated change the positive (negative) superclasses into negative (positive) superclasses. */
									if (negated) {
										if (superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive") && !superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative")) {
											superclasses.remove("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive");
											superclasses.add("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative");
										} else if (superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative")  && !superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive")) {
											superclasses.remove("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative");
											superclasses.add("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive");
										}
										
										
									}
									foundURIs.addAll(superclasses);
									
								}
							}
							/* Check if the word is context dependent */
							else if (superclasses.contains(ont.URI_ContextDependent)) {
								if (hasProperty("applyRelatedWords")) {
									/* Find all words related to the current word. */
									HashSet<Word> relatedWords = new HashSet<>();
									for (Relation rel : word.getRelations().getRelationsToChildren("deps")){
										if (rel.getChild() instanceof Word)
											relatedWords.add((Word)rel.getChild());
									}
									for (Relation rel : word.getRelations().getRelationsToParents("deps")){
										if (rel.getParent() instanceof Word)
											relatedWords.add((Word)rel.getParent());
									}
									/* Loop over the related words. */
									for (Word relWord : relatedWords){
										/* Check if the related word occurs in the ontology. */
										if (relWord.hasAnnotation("URI")){
											HashSet<String> classes = ont.getSuperclasses(relWord.getAnnotation("URI"));
											/* The related word should be a subclass of aspect mention. */
											if (classes.contains(ont.URI_AspectMention)){
												/* Create the new class. */
												String word1 = word.getWord().substring(0, 1).toUpperCase()+word.getWord().substring(1);
												String word2 = relWord.getAnnotation("lemma");
												word2 = word2.substring(0,1).toUpperCase()+word2.substring(1);
												String newClassURI = ont.addClass(word1 + " " + word2, true, word1 + " " + word2, URI, relWord.getAnnotation("URI"));
												/* Add all the superclasses of the new class to foundURIs. */
												HashSet<String> newClassSuperclasses = ont.getSuperclasses(newClassURI);
												if (negated) {
													if (newClassSuperclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive") && !newClassSuperclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative")) {
														newClassSuperclasses.remove("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive");
														newClassSuperclasses.add("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative");
													} else if (newClassSuperclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative")  && !newClassSuperclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive")) {
														newClassSuperclasses.remove("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative");
														newClassSuperclasses.add("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive");
													}
													
													
												}
												foundURIs.addAll(newClassSuperclasses);
												
											}
												
											
										}
									}
								}
							} else {							   
								/* If the word is negated change the positive (negative) superclasses into negative (positive) superclasses. */
								if (negated) {
									if (superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive") && !superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative")) {
										superclasses.remove("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive");
										superclasses.add("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative");
									} else if (superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative")  && !superclasses.contains("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive")) {
										superclasses.remove("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Negative");
										superclasses.add("http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#Positive");
									}
									
									
								}
								foundURIs.addAll(superclasses);
								
								
							}
						}	
						
						/* Determine the value of the Positive and Negative feature. */
						allAttributes.putIfAbsent("Positive", new Attribute("Positive"));
						allAttributes.putIfAbsent("Negative", new Attribute("Negative"));
						if (foundURIs.contains(ont.URI_Positive)) {
							spanInstance.put(allAttributes.get("Positive"), 1.0);
						}
						if (foundURIs.contains(ont.URI_Negative)) {
							spanInstance.put(allAttributes.get("Negative"), 1.0);
						}
//						System.out.println(word.getLemma() + "::"+ word.getTextualUnit().getAnnotations().get("id")+" :: "+foundURIs);
					} //End of ontology loop
					
					
				} //End of the word loop
				
				/* Set the review sentiment score. */
				if (hasProperty("applyStanfordReviewSentiment")){
					/* Determine the average sentiment score over all the sentences in the review. */
					double stanfordSentiment = 0.0;
					
					
						double sentenceScore = getSentimentScore(sentence.getAnnotation("phraseSentiment", ArrayList.class));
						
						
					
					
					spanInstance.put(allAttributes.get("stanfordReviewSentiment"), stanfordSentiment);
				}
				//System.out.println(span.getAnnotations().get("category") + "::"+ span.getTextualUnit().getAnnotations().get("id")+" :: "+spanInstance);
			}
		}

		/* Convert attribute map to list of attributes and add classes */
		ArrayList<Attribute> featVector = new ArrayList<>();
		featVector.addAll(allAttributes.values());
		classLabels.add("positive");
		classLabels.add("negative");
		classLabels.add("neutral");
		classLabels.add("conflict");
		Attribute classAttribute = new Attribute("polarity", classLabels);
		featVector.add(classAttribute);

		/* Convert to Weka data */
		wekaData = new Instances(label, featVector, 0);
		wekaData.setClass(classAttribute);

		/* Iterate over all the spans and set the instance values */
		for (Span span2 : allInstances.keySet()) {
			HashMap<Attribute, Double> currentInstance = allInstances.get(span2);
			//System.out.println(currentInstance);
			Instance i = new SparseInstance(featVector.size());
			i.setDataset(wekaData);

			features.put(span2, new HashSet<String>());

			/* For each attribute of the current instance, set the value to the attribute value or else 0.0 */
			for (Attribute attr : featVector) {
				i.setValue(attr, currentInstance.getOrDefault(attr, 0.0));
				if (currentInstance.containsKey(attr)) {
					features.get(span2).add(attr.name() + "\t" + currentInstance.get(attr) + "\n");
				}
			}

			/* Set correct class for current instance */
			if (goldValues.containsKey(span2)) {
				i.setClassValue(goldValues.get(span2));
			}

			/* Add the span to Weka */
			wekaInstances.put(span2, i);
			wekaData.add(i);
		}
		//System.out.println(wekaData);
	}

	@Override
	/**
	 * Train the Weka model with the Weka data
	 */
	public void train() {
		Framework.log(getLabel() + " ++Training...++ ");

		/* Create Weka instances, where split the data into a training and test part*/
		Instances trainingData = new Instances(this.wekaData, 0);

		ArrayList<Span> trainingSpans = new ArrayList<Span>();
		HashSet<Annotatable> trainingSet = new HashSet<Annotatable>();

		/* Create the training data for Weka */
		for (Span review3 : getCombinedTrainingData()) {
			for (Span span3 : review3.getDataset().getSpans(review3, "opinion")) {
				trainingData.add(this.wekaInstances.get(span3));
				trainingSpans.add(span3);
			}
		}
		trainingSet.addAll(trainingSpans);

		try {
			model = new SMO();
			model.setFilterType(new SelectedTag(SMO.FILTER_NORMALIZE, SMO.TAGS_FILTER));
			model.buildClassifier(trainingData);
			System.out.println("numClassAttr: "+model.numClassAttributeValues());
				
			/* Get trainingdata predictions (in-sample) */
			eval = new Evaluation(trainingData);
			double[] inSamplePredictions = eval.evaluateModel(model, trainingData);

			/* Save the Weka predictions as Prediction objects in the predictions hashMap */
			for (int i = 0; i < trainingSpans.size(); i++) {
				Span s = trainingSpans.get(i);
				Prediction p = new Prediction(s);
				p.putAnnotation("polarity", classLabels.get((int) inSamplePredictions[i]));
				this.predictions.put(s, p.getSingletonSet());
			}
			
			/* Log and clear the in sample predictions */
			Framework.log(getLabel() + " ++The results of the training data:++ ");
			Framework.log(this.evaluate(this.getEvaluators().iterator().next(), trainingSet).getEvaluationResults());
			predictions.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	/**
	 * Predicts the polarities of the testData
	 */
	public void predict() {
		Framework.log(getLabel() + " ++Predicting...++ ");

		/* Create the Weka instance for testData */
		Instances testData = new Instances(this.wekaData, 0);
		ArrayList<Span> testSpans = new ArrayList<Span>();

		/* Create the test data for Weka */
		for (Span r : getTestData()) {
			for (Span s : r.getDataset().getSpans(r, "opinion")) {
				testData.add(this.wekaInstances.get(s));
				testSpans.add(s);
			}
		}

		try {
			/* Get testdata predictions (out-sample) */
			eval = new Evaluation(testData);
			double[] outSamplePredictions = eval.evaluateModel(model, testData);

			System.out.println(eval.toSummaryString("\nResults\n======\n", false));
			System.out.println(eval.toClassDetailsString());

			/*
			 * Save the Weka predictions as Prediction objects in the predictions hashMap
			 */
			for (int i = 0; i < testSpans.size(); i++) {
				Span s = testSpans.get(i);
				Prediction p = new Prediction(s);
				p.putAnnotation("polarity", classLabels.get((int) outSamplePredictions[i]));
				this.predictions.put(s, p.getSingletonSet());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the sentiment score as determined by the Stanford Sentiment tool.
	 * @param sentimentScores, the scores to be made into a single score
	 * @return the sentiment score
	 */
	public double getSentimentScore(ArrayList<Double> sentimentScores){
		if (sentimentScores == null || sentimentScores.isEmpty())
			return 0.0;
		
		double score = 0;
		if (sentimentScores.size()> 0){
			for (int i = 0; i < 5; i++){
				score += sentimentScores.get(i)*(i-2);
			}
		}
		return score;
	}
}