package edu.eur.absa.algorithm.seminar;

import java.io.File;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm2;
import edu.eur.absa.algorithm.Experiment2;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.model.Dataset;

/**
 * The main for the sentiment classification model for the restaurant domain.
 * 
 * @author Karoliina Ranta
 * 
 */
public class MainRestaurant {
	
	public static void main(String[] args) throws Exception {
		
//		Dataset TRAININGdataset =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"ThesisRestaurants-Training2.json"));
//		Dataset TESTdataset =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"ThesisRestaurants-Test2.json"));
		
		Dataset TRAININGdatasetAuto =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarRestaurants-TrainingAuto5001.json"));
		Dataset TESTdatasetAuto =  (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarRestaurants-TestAuto5001.json"));
		
//		AbstractAlgorithm2 basecase = new SeminarAlgorithmRestaurant("sentence", "opinion", false, false, 3, "RestaurantOntologySeminar.owl").setBinaryProperties("applyReview", "applyCategory", "applyWords", "applyStanfordReviewSentiment", "applyStanfordSentiment", "applyNRCSentiment");		
//		AbstractAlgorithm2 manualOntologyModel = new SeminarAlgorithmRestaurant("review", "opinion", false, false, 3, "RestaurantOntologySeminar.owl").setBinaryProperties("applyReview", "applyCategory", "applyWords", "applyStanfordReviewSentiment", "applyStanfordSentiment", "applyNRCSentiment", "applyOntology", "applyNegation");		
		AbstractAlgorithm2 autoOntologyModel = new SeminarAlgorithmRestaurant("sentence", "opinion", false, false, 3, "RestaurantOntologySA.owl").setBinaryProperties("applyReview", "applyCategory", "applyWords", "applyStanfordReviewSentiment", "applyStanfordSentiment", "applyNRCSentiment", "applyOntology");//, "applyNegation");		
		
//		/*
//		 * Determines the in-sample and out-sample F1, using the training- and testdata.
//		 * Otherwise, to determine the average F1 and average standard deviation, 
//		 * uncomment .setDataset and .setCrossValidation and comment .setTrainingAndTestDataset
//		 */
//		Experiment2.createNewExperiment()
//		.addAlgorithms(manualOntologyModel)
////		.setDataset(TRAININGdataset)
//		.setTrainingAndTestSet(TRAININGdataset, TESTdataset, 1, 0)
////		.setCrossValidation(5, 10, 1.0)
//		.run();
		
		/*
		 * Determines the in-sample and out-sample F1, using the training- and testdata.
		 * Otherwise, to determine the average F1 and average standard deviation, 
		 * uncomment .setDataset and .setCrossValidation and comment .setTrainingAndTestDataset
		 */
		Experiment2.createNewExperiment()
//		.addAlgorithms(basecase)
//		.addAlgorithms(ManualOntologyModel)
		.addAlgorithms(autoOntologyModel)
		.setDataset(TRAININGdatasetAuto)
//		.setTrainingAndTestSet(TRAININGdatasetAuto, TESTdatasetAuto, 1, 0)
		.setCrossValidation(5, 10, 1.0)
		.run();	
	}
}