package edu.eur.absa.ontologybuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;

import edu.eur.absa.Framework;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import edu.eur.absa.seminarhelper.SeminarOntology;

public class GridSearchAlphaBeta {
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
		OntologyGridSearchSetCreation build = new OntologyGridSearchSetCreation(base, aspectCategories, domain, 0.1, 1.0, 0.25, true);
		OntologyGridSearchSet findalphabeta = new OntologyGridSearchSet(base, aspectCategories, domain, 0.1, 1.0, 0.1, true);

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
		HashMap<String, Double> acceptedWords = build.findTerms(true, true, true, 0.4, 0.1);
		System.out.println("WORDS ACCEPTED : "+ acceptedWords.size());

		/* Load the contrasting text. */
		findalphabeta.loadContrast();
		try {
			findalphabeta.loadReviews();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int i = 0;
		String[] results = new String[101];
		double alpha=0;
		double beta=1-alpha;
		while (alpha<=1) {
			findalphabeta.findTerms(true, true, true, alpha, beta, acceptedWords);
			int[] stats = build.getStats();
			double result = (double) stats[0] / ( (double) stats[0] + (double) stats[1] );
			results[i] = "Alpha : " + alpha + " &&& Beta: "+beta + " // Ratio accepted: " + result;
			System.out.println(results[i]);
			i++;

			alpha = alpha+0.1;
			beta = 1-alpha;
		}

		for (i=0; i<100; i++) {
			System.out.println(results[i]);
		}
	}
}
