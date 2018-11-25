package edu.eur.absa.ontologybuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;

import edu.eur.absa.Framework;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.seminarhelper.SeminarOntology;
import java.util.*;

public class GridSearchMus {
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

		/* Initialise the semi-automatic ontology builder. */
		OntologyGridSearchSetCreation build = new OntologyGridSearchSetCreation(base, aspectCategories, domain, 0.1, 1.0, 0.25, true); //TODO: add optimal alpha and beta
		OntologyGridSearchSetMus findmus = new OntologyGridSearchSetMus(base, aspectCategories, domain, 0.1, 1.0, 0.1, true);

		/* Load the contrasting text. */
		build.loadContrast();

		/* Load the reviews toa
		 *  be used to build the ontology. */
		try {
			build.loadReviews();
		} catch (Exception e) {
			//
			e.printStackTrace();
		}

		/* Find important terms and add them to the ontology. */
		HashMap<String, Double> acceptedWords = build.findTerms(true, true, true, 0.3, 0.7); //TODO: find optimal alpha and beta
		System.out.println("WORDS ACCEPTED : "+ acceptedWords.size());

		/* Load the contrasting text. */
		findmus.loadContrast();
		try {
			findmus.loadReviews();
		} catch (Exception e) {
			// 
			e.printStackTrace();
		}
		double alpha = 0.3; //TODO: add optimal alpha and beta
		double beta = 0.7;
		double[] fraction  = new double[3];
		fraction[0] = 0.1; //fraction verbs
		fraction[1] = 0.1; //fraction nouns
		fraction[2] = 0.1;// fraction adjectives

		double fraction_verbs = 0.1;
		double fraction_nouns = 0.1;
		double fraction_adj = 0.1;
		
		while (fraction[0] <= 0.21) {
			findmus.findTerms(false, false, true, alpha, beta, fraction[0], fraction_nouns, fraction_adj, acceptedWords);
			int[] stats = findmus.getStats();
			double accep_num_verbs = (double) stats[2];
			double reject_num_verbs = (double) stats[3];
			double accep_ratio_verbs = (accep_num_verbs)/(accep_num_verbs+ reject_num_verbs);
			//System.out.println("Fraction: " + fraction[0] + " // Number accepted: " + accep_num_verbs + " // Number rejected " + reject_num_verbs + " // Ratio: " + accep_ratio_verbs);
			fraction[0] = fraction[0] + 0.01;
		}

		fraction[0] = 0.1;
		
		String[] results = new String[16];
		int i = 0;
		while (fraction[1]<=0.21) {			
			findmus.findTerms(true, false, true, alpha, beta, fraction_verbs, fraction[1], fraction_adj, acceptedWords);
			int[] stats = findmus.getStats();
			//double result = (double) stats[0] / ( (double) stats[0] + (double) stats[1] );
			//double result_verbs = (double) stats[2] / ( (double) stats[2] + (double) stats[3]);
			double accep_num_nouns = (double) stats[4];
			double reject_num_nouns = (double) stats[5];
			double result_nouns = accep_num_nouns / (accep_num_nouns + accep_num_nouns);
			//double result_adjectives = (double) stats[6] / ( (double) stats[6] + (double) stats[7]);
			results[i] = "fraction nouns + : " + fraction[1] + " // Ratio accepted nouns: " + result_nouns + " Number of accepted nouns: " + accep_num_nouns + " // Num Reject = " + reject_num_nouns;
			System.out.println(results[i]);
			i++;
			fraction[1] = fraction[1] + 0.01;
		}
		
		String[] results1 = new String[16];
		int j = 0;
		while (fraction[2]<=0.21) {			
			findmus.findTerms(true, true, true, alpha, beta, fraction_verbs, fraction_nouns, fraction[2], acceptedWords);
			int[] stats = findmus.getStats();
			//double result = (double) stats[0] / ( (double) stats[0] + (double) stats[1] );
			//double result_verbs = (double) stats[2] / ( (double) stats[2] + (double) stats[3]);
			//double result_nouns = (double) stats[4] / ( (double) stats[4] + (double) stats[5]);
			double accep_num_adj = (double) stats[6];
			double reject_num_adj = (double) stats[7];
			double result_adjectives = accep_num_adj/ (accep_num_adj + reject_num_adj);
			results1[j] = "fraction adj + : " + fraction[2] + " // Ratio accepted adj: " + result_adjectives + " Number of accepted adj: " + accep_num_adj + " Number of rejected adj" + reject_num_adj;
			System.out.println(results1[j]);
			j++;
			fraction[2] = fraction[2] + 0.01;
		}


	}
}
