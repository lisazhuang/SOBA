package edu.eur.absa.ontologybuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;

import edu.eur.absa.Framework;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.seminarhelper.SeminarOntology;

public class GridSearchT {
	public static void main(String[] args) throws ClassNotFoundException, JSONException, IllegalSpanException, IOException {

		/* RESTAURANT DOMAIN */

		/* Start with the skeletal ontology. */
		//		ThesisOntology base = new ThesisOntology(Framework.EXTERNALDATA_PATH + "RestaurantOntologyThesisBase.owl");
		SeminarOntology base = new SeminarOntology(Framework.EXTERNALDATA_PATH + "RestaurantOntologyThesisBase2018.owl");

		HashMap<String, HashSet<String>> aspectCategories = new HashMap<String, HashSet<String>>();

		HashSet<String> restaurant = new HashSet<String>();
		restaurant.add("general");
		restaurant.add("prices");
		restaurant.add("miscellaneous");
		aspectCategories.put("restaurant", restaurant);

		HashSet<String> ambience = new HashSet<String>();  
		ambience.add("general");
		aspectCategories.put("ambience", ambience);

		HashSet<String> service = new HashSet<String>();
		service.add("general");
		aspectCategories.put("service", service);

		HashSet<String> location = new HashSet<String>();
		location.add("general");
		aspectCategories.put("location", location);

		HashSet<String> sustenance = new HashSet<String>();
		sustenance.add("prices");
		sustenance.add("quality");
		sustenance.add("style&options");
		aspectCategories.put("sustenance", sustenance);

		/* Set the domain. */
		String domain = "restaurant";
		
		double[] fraction = new double[3];	
		fraction[0]=0.16;	//fraction nouns
		fraction[1]=0.20; 	// fraction adjectives  
		fraction[2]=0.16; //fraction verbs

		/* Initialise the semi-automatic ontology builder. */
		OntologyGridSearchSetCreation build = new OntologyGridSearchSetCreation(base, aspectCategories, domain, 0.1, 1.0, 0.20, true); 
		OntologyGridSearchSetT find_t = new OntologyGridSearchSetT(base, aspectCategories, domain, 0.1, 1.0, fraction, true); 
		/* Load the contrasting text. */
		build.loadContrast();

		/* Load the reviews toa
		 *  be used to build the ontology. */
		try {
			build.loadReviews();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Find important terms and add them to the ontology. */
		HashMap<String, Double> acceptedWords = build.findTerms(true, true, true, 0.3, 0.7);
		System.out.println("WORDS ACCEPTED : "+ acceptedWords.size());
		HashMap<String, HashSet<String>> acceptedRelations = build.subsumptionAspect(acceptedWords);
		System.out.println("Accepted Relations: " + acceptedRelations.size());

		/* Load the contrasting text. */
		find_t.loadContrast();
		try {
			find_t.loadReviews();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HashMap<String, Double> words = find_t.findTerms(true, true, true, 0.3, 0.7, acceptedWords); // accept words. Next: subsumption
		
		String[] results = new String[12];
		double t = 0.0;
		int i = 0;
		while (t <= 1.1)
		{
			find_t.subsumptionAspect(words, acceptedRelations, t);
			
			int[] stats = find_t.getStats();
			//print the alpha and beta line!!!!
			double result = (double) stats[2] / ( (double) stats[2] + (double) stats[3] );
			results[i] = "t: " + t + " Number overall accetped: " + stats[2] + " Ratio Overall Accepted: " + result;
			System.out.println(results[i]);
			i++;
			t = t + 0.1;
		}
		//for each t, do the subsumption method, and report the acceptance ratio
	}
}
