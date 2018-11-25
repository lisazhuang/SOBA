package edu.eur.absa.algorithm.demo;

import java.io.File;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordNetDatabase.*;
import java.util.HashSet;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.algorithm.Experiment;
import edu.eur.absa.algorithm.ontology.OntologySentimentAlgorithm;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.external.ReasoningOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.nlp.OntologyLookup;
import edu.eur.absa.seminarhelper.SeminarOntology;

public class TestClass {
	
	public static void main(String args[]) throws Exception {
		
		//Dataset train2016 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Train.json"));
		//Dataset test2016 =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB1Restaurants-Test.json"));
		
		//AbstractAlgorithm cheating1 = new CheatingAspectCategoryClassificationAlgorithm()
		//		.setBinaryProperties("useless-property1","useless-property2")
		//		.setProperty("useless-property3", "some-value")
		//		;
		
		//Framework.log("Results of experiment 1\n====================");
		//Experiment.createNewExperiment()
		//	.addAlgorithms(cheating1)
		//	.setDataset(train2016)	
		//	.setCrossValidation(1, 10, 0.8, 0.2)
		//	.run();
		
		//Framework.log("Results of experiment 2\n====================");
		//Experiment.createNewExperiment()
		//.addAlgorithms(cheating1)
		//.setTrainingAndTestSet(train2016, test2016, 1.0)
		//.run();
		
		/*Adding synset properties to ontology*
		//HashMap<String, String> contextMap = new HashMap<String,String>();
		//contextMap.put("enjoy", "The industry enjoyed a boom");
		//SeminarOntology base = new SeminarOntology(Framework.EXTERNALDATA_PATH + "RestaurantOntologySeminarBase.owl");
		//base.addClass("verb", contextMap, "Enjoy", true, "enjoy", new HashSet<String>(), "http://www.semanticweb.org/karoliina/ontologies/2017/4/Restaurant#FoodMention");
		//base.save("TestOntology.owl");
	
		/*Multi Words*/
		File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", f.toString());
		WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();
		String candidateMultiLemma = "up with";
		Synset[] synsets = wordDatabase.getSynsets("up with");
		
		
		if (wordDatabase.getSynsets(candidateMultiLemma).length > 0)
		{System.out.println("yay");}
		else
			System.out.println("nope");
	}
	
	public void runExperiment1() {
		
	}
}