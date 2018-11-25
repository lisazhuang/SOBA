package edu.eur.absa.algorithm.seminar;

import java.io.File;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm2;
import edu.eur.absa.algorithm.Experiment2;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.model.Dataset;

/**
 * The main for the sentiment classification model for the laptop domain.
 * 
 * @author Karoliina Ranta
 *
 */
public class MainLaptop {
	
	public static void main(String[] args) throws Exception {

		Dataset TRAININGdatasetAuto =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarLaptops-TrainingAuto5001.json"));
		Dataset TESTdatasetAuto =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarLaptops-TestAuto5001.json"));
		
//    	AbstractAlgorithm2 basecase = new SeminarAlgorithmLaptop("sentence", "opinion", false, false, 3, "LaptopOntologySuus16-4.owl").setBinaryProperties("applySentence", "applyCategory", "applyWords", "applyStanfordReviewSentiment", "applyStanfordSentiment", "applyNRCSentiment");		
		AbstractAlgorithm2 autoOntologyModel = new SeminarAlgorithmLaptop("sentence", "opinion", false, false, 3, "LaptopManualOntology.owl").setBinaryProperties("applySentence", "applyCategory", "applyWords", "applyStanfordReviewSentiment", "applyStanfordSentiment", "applyNRCSentiment", "applyOntology", "applyNegation");
//		AbstractAlgorithm2 Ontologynoprop = new SeminarAlgorithmLaptop("sentence", "opinion", false, false, 3, "LaptopManualOntology.owl").setBinaryProperties("applyOntology", "applyNegation");
		
		/*
		 * Determines the in-sample and out-sample F1, using the training- and testdata.
		 * Otherwise, to determine the average F1 and average standard deviation, 
		 * uncomment .setDataset and .setCrossValidation and comment .setTrainingAndTestDataset
		 */
		Experiment2.createNewExperiment()
//    	.addAlgorithms(basecase)
		.addAlgorithms(autoOntologyModel)
//		.addAlgorithms(Ontologynoprop)
//		.setDataset(TRAININGdatasetAuto)
		.setTrainingAndTestSet(TRAININGdatasetAuto, TESTdatasetAuto, 1, 0)
//		.setCrossValidation(5, 10, 1.0)
		.run();
	}
}