package edu.eur.absa.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

import org.json.JSONObject;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.nlp.CoreNLPDependencyParser;
import edu.eur.absa.nlp.CoreNLPLemmatizer;
import edu.eur.absa.nlp.CoreNLPNamedEntityRecognizer;
import edu.eur.absa.nlp.CoreNLPParser;
import edu.eur.absa.nlp.CoreNLPPosTagger;
import edu.eur.absa.nlp.CoreNLPSentimentAnnotator;
import edu.eur.absa.nlp.CoreNLPTokenizer;
import edu.eur.absa.nlp.NLPTask;
import edu.eur.absa.nlp.OntologyLookup;
import edu.eur.absa.seminarhelper.SeminarOntology;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

/**
 * A program for preprocessing the Yelp restaurant review data. It extracts all the restaurant reviews
 * from the dataset and adds dependencies.
 * 
 * @author Karoliina Ranta
 * 
 */
public class YelpDataReviewReader implements IDataReader {
	
	public static void main(String args[]) throws Exception {
		
		Dataset reviewData =  (new YelpDataReviewReader()).read(new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_review.json"));
		(new DatasetJSONWriter()).write(reviewData, new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_review_restaurant_auto5001.json"));
		(new DatasetJSONWriter(true)).write(reviewData, new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_review_restaurant_auto5001.pretty.json"));

		Dataset check = (new DatasetJSONReader()).read(new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_review_restaurant_auto5001.json"));
		(new DatasetJSONWriter(true)).write(check, new File(Framework.EXTERNALDATA_PATH+"CheckYelpAuto5001.json"));
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		
		/* Create a hashset containing the businessIds of the restaurants. */
		HashSet<String> restaurantIDs = getRestaurantIds(new File(Framework.EXTERNALDATA_PATH+"yelp_academic_dataset_business.json"));
				
		/* Instantiate the dataset. Review is the textual unit, as sentences within a single review are semantically connected */
		Dataset dataset = new Dataset(file.getName(),"review");
		
		/* Read the json file. */
		Framework.debug("DatasetJSONReader: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		
		/* Keep track of the number of restaurant reviews. */
		int numRev = 0;
		boolean addRev = true;
		
		while ((line = in.readLine()) != null && addRev){
			boolean restaurant = true;
			
			Span reviewSpan = null; // = new Span("review", dataset);
			String reviewId = "";
						
			/* Scan the review. */
            Scanner input = new Scanner(line);
			input.useDelimiter("(\":|(,\"))");
            while (input.hasNext() && restaurant) {
                String line2 = input.next();
                if (line2.contains("review_id")) {
        			reviewId = input.next().replace("\"", "");
                } else if (line2.contains("business_id")) {
        			String businessId = input.next().replace("\"", "");
        			/* Check if the businessId belongs to the restaurant reviews. */
        			if (!restaurantIDs.contains(businessId)) {
        				restaurant = false;
        			} else {
        				reviewSpan = new Span("review", dataset);
            			reviewSpan.putAnnotation("id", reviewId);
                    	numRev++;
            			if (numRev > 5000) {
            				addRev = false;
            			}
        			}
                } else if (line2.contains("stars")) {
        			String stars = input.next().replace("\"", "");
    				if (reviewSpan != null) {
    					reviewSpan.putAnnotation("stars", stars);
    				}
                } else if (line2.contains("text")) {
        			String textualunit = input.next().replace("\"", "");
        			
        			String reviewText = "";
        			
        			/* Scan the sentences. */
                    Scanner reviewInput = new Scanner(textualunit);
                    reviewInput.useDelimiter("(?<=[.?!]+)"); //.?!
                    int i = 0;
                    while (reviewInput.hasNext()) {
        				String text = reviewInput.next();
                    	
                    	 /* Check that the sentence isn't just white space. */
                    	if (text.trim().length() > 0) {
	                    	i++;
	                    	
	                    	/* Create a sentence id. */
	        				String sentenceId = reviewId + ":" + i;
	        				if (reviewSpan != null) {
		        				Span sentenceSpan = new Span("sentence", reviewSpan);
		        				sentenceSpan.putAnnotation("id", sentenceId);
		        				sentenceSpan.putAnnotation("text", text);
	        				}
	        				reviewText += text;
                    	}
                    }
                    reviewInput.close();
                    
    				if (reviewSpan != null) {
    					reviewSpan.putAnnotation("text", reviewText);
    				}
                }
            }
            input.close();
		}
		in.close();
		
		
		dataset.getPerformedNLPTasks().add(NLPTask.SENTENCE_SPLITTING);
		dataset.process(new CoreNLPTokenizer(), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")	
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
			.process(new CoreNLPNamedEntityRecognizer(), "sentence")
			.process(new CoreNLPParser(), "sentence")
			.process(new CoreNLPDependencyParser(), "sentence")
			.process(new CoreNLPSentimentAnnotator(), "sentence")
			;		
		
		System.out.println("number restaurant reviews : " + numRev);
		
		return dataset;
	}
	
	/**
	 * A method for determining the ids of the restaurants.
	 * 
	 * @param file, the yelp file containing all the businesses
	 * @return a HashSet containing the restaurant business ids
	 */
	public HashSet<String> getRestaurantIds(File file) throws Exception {
		HashSet<String> ids = new HashSet<String>();
		
		/* Read the json file. */
		Framework.debug("DatasetJSONReader: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		while ((line = in.readLine()) != null){
			String businessId = "";

			/* Scan the business. */
            Scanner input = new Scanner(line);
			input.useDelimiter("(:|(,\"))");
            while (input.hasNext()) {
                String line2 = input.next();
                if (line2.contains("business_id")) {
        			businessId = input.next().replace("\"", "");
                } else if (line2.contains("categories")) {
                	
                	/* Get the business categories. */
                    Scanner catInput = new Scanner(line);
                    catInput.useDelimiter("(?<=(\"categories\":\\[|\\]))");
                    while (catInput.hasNext()) {
                    	String cats = catInput.next();
                    	if (cats.contains("categories")) {
                    		if (catInput.hasNext()) {
	                    		cats = catInput.next();
	                    		
	                        	/* Check if the business if a restaurant. */
	                        	if (cats.contains("Restaurants")) {
	                        		ids.add(businessId);
	                        	}
                    		}
                    	}
                    }
                    catInput.close();
                }
            }
            input.close();
		}
		in.close();
		return ids;
	}
}
