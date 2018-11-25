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
 * A program for preprocessing the Amazon laptop review data. It extracts all the laptop reviews
 * from the dataset and adds dependencies.
 * 
 * @author Karoliina Ranta
 * 
 */
public class AmazonDataReviewReader implements IDataReader {
	
	public static void main(String args[]) throws Exception {
	
		Dataset reviewData =  (new AmazonDataReviewReader()).read(new File(Framework.EXTERNALDATA_PATH+"reviews_Electronics.json"));
		(new DatasetJSONWriter()).write(reviewData, new File(Framework.EXTERNALDATA_PATH+"amazon_review_laptop5001.json"));
		(new DatasetJSONWriter(true)).write(reviewData, new File(Framework.EXTERNALDATA_PATH+"amazon_review_laptop5001.pretty.json"));

		Dataset check = (new DatasetJSONReader()).read(new File(Framework.EXTERNALDATA_PATH+"amazon_review_laptop5001.json"));
		(new DatasetJSONWriter(true)).write(check, new File(Framework.EXTERNALDATA_PATH+"CheckAmazon5001.json"));
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		/* Create a hashset containing the businessIds of the restaurants. */
		HashSet<String> laptopIDs = getLaptopIds(new File(Framework.EXTERNALDATA_PATH+"meta_Electronics.json"));
						
		/* Instantiate the dataset. Review is the textual unit, as sentences within a single review are semantically connected */
		Dataset dataset = new Dataset(file.getName(),"review");
		
		/* Read the json file. */
		Framework.debug("DatasetJSONReader: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		
		/* Keep track of the number of laptop reviews. */
		int numRev = 0;
		boolean addRev = true;
		
		while ((line = in.readLine()) != null && addRev){
			boolean laptop = true;
			
			Span reviewSpan = null;
			String reviewId = "";
						
			/* Scan the review. */
            Scanner input = new Scanner(line);
			input.useDelimiter("(\":\\s|(,\\s\"))");
            while (input.hasNext() && laptop) {
                String line2 = input.next();
                if (line2.contains("reviewerID")) {
        			reviewId = input.next().replace("\"", "");
                } else if (line2.contains("asin")) {
        			String businessId = input.next().replace("\"", "");
        			/* Check if the businessId belongs to the laptop reviews. */
        			if (!laptopIDs.contains(businessId)) {
        				laptop = false;
        			}
                } else if (line2.contains("reviewText")) {
                	
        			String textualunit = input.next().replace("\"", "");
        			
        			String reviewText = "";
        			
        			/* Scan the sentences. */
                    Scanner reviewInput = new Scanner(textualunit);
                    reviewInput.useDelimiter("(?<=[.?!]+)"); //.?!
                    int i = 0;
                    if (reviewInput.hasNext()) {
        				reviewSpan = new Span("review", dataset);
            			reviewSpan.putAnnotation("id", reviewId);
            			
            			numRev++;
            			if (numRev > 5000) {
            				addRev = false;
            			}
                    }
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
        			reviewSpan.putAnnotation("text", reviewText);
                } else if (line2.contains("overall")) {
        			String stars = input.next().replace("\"", "");
    				if (reviewSpan != null) {
    					reviewSpan.putAnnotation("stars", stars);
    				}
                }
            }
            input.close();            
            if (reviewSpan != null) {
            	if (!(reviewSpan.hasAnnotation("id") && reviewSpan.hasAnnotation("text") && reviewSpan.hasAnnotation("stars"))) {
            		reviewSpan = null;
            		numRev--;
            	} else if (reviewSpan.getAnnotation("id") == null || reviewSpan.getAnnotation("text") == null || reviewSpan.getAnnotation("stars") == null) {
            		reviewSpan = null;
            		numRev--;
            	}
            }
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
		
		System.out.println("number laptop reviews : " + numRev);
		
		return dataset;
	}
	
	/**
	 * A method for determining the ids of the laptop reviews.
	 * 
	 * @param file, the amazon file containing all the businesses
	 * @return a HashSet containing the laptop business ids
	 */
	public HashSet<String> getLaptopIds(File file) throws Exception {
		HashSet<String> ids = new HashSet<String>();
		
		/* Read the json file. */
		Framework.debug("DatasetJSONReader: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		while ((line = in.readLine()) != null){
			String businessId = "";

			/* Scan the business. */
            Scanner input = new Scanner(line);
			input.useDelimiter("(\':\\s|(,\\s\'))");
            while (input.hasNext()) {
                String line2 = input.next();
                if (line2.contains("asin")) {
                	if (input.hasNext()) {
                		businessId = input.next().replace("\'", "");
                	} else {
                		continue;
                	}
                } else if (line2.contains("categories")) {
                	
                	/* Get the business categories. */
                    Scanner catInput = new Scanner(line);
                    catInput.useDelimiter("(?<=(\'categories\':\\s\\[\\[|\\]\\]))");
                    while (catInput.hasNext()) {
                    	String cats = catInput.next();
                    	if (cats.contains("categories")) {
                    		if (catInput.hasNext()) {
	                    		cats = catInput.next();
	                    		
	                        	/* Check if the product is a laptop. */
	                        	if (cats.contains("Laptops")) {
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
