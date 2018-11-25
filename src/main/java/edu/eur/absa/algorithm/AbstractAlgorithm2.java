package edu.eur.absa.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import edu.eur.absa.evaluation.evaluators.Evaluator;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.model.Annotatable;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;

public abstract class AbstractAlgorithm2 {

	protected HashMap<Annotatable, HashSet<Prediction>> predictions = new HashMap<>();
	//the things that can be evaluated based on the output of the algorithm
	protected HashSet<Evaluator> evaluators = new HashSet<>();
	//features
	protected HashMap<Annotatable, HashSet<String>> features = new HashMap<>();
	
	protected ArrayList<HashSet<Span>> dataSubSets = new ArrayList<>();
	protected HashSet<Annotatable> testAnnotatables = new HashSet<>();
	protected String label;
	protected Properties properties = new Properties();
	
	/**
	 * The Span type which should be used to divide the dataset for this algorithm
	 */
	protected String unitOfAnalysisSpanType;
	
	public AbstractAlgorithm2(String label, String unitOfAnalysisSpanType) {
		this.label = label;
		this.unitOfAnalysisSpanType = unitOfAnalysisSpanType;
	}

	public void clean(){
		predictions.clear();
		cleanAlgorithm();
	}
	
	public void setDataSubSets(ArrayList<HashSet<Span>> dataSubSets){
		this.dataSubSets = dataSubSets;
		testAnnotatables.clear();
		for (Span r : getTestData()){
			for (Span s : r.getDataset().getSpans(r, "opinion")) {
				testAnnotatables.add(s);
			}
		}
	}
	
	protected abstract void cleanAlgorithm();
	
	public abstract void preprocess();
	
	public abstract void train();
	
	// when it is possible to serialize trained algorithms, it might be useful to add a predict(HashSet<Span> newTestData) function
	public abstract void predict();
	
	public EvaluationResults evaluate(Evaluator evaluator){
//		if (!evaluators.contains(evaluator)){
//			return null;
//		} else {
//			return evaluator.evaluate(getTestAnnotatables(), predictions);
//		}
		return evaluate(evaluator, getTestAnnotatables());
	}
	
	protected EvaluationResults evaluate(Evaluator evaluator, HashSet<Annotatable> testAnnotatables){
		if (!evaluators.contains(evaluator)){
			return null;
		} else {
			return evaluator.evaluate(testAnnotatables, predictions, features);
		}
	}
	
	/**
	 * Execute on a single
	 * @return
	 */
	public String executeAndShowResults(){
		return executeAndShowResults(true);
	}
	
	public String executeAndShowResults(boolean preprocess){
		clean();
		if (preprocess){
			preprocess();
		}
		train();
		predict();
		String output = "";
		for (Evaluator eval : evaluators){
			output += evaluate(eval).toString();
		}
		return output;
	}
	public HashMap<Class<? extends Evaluator>, EvaluationResults> executeAndReturnResults(){
		return executeAndReturnResults(true);
	}
	public HashMap<Class<? extends Evaluator>, EvaluationResults> executeAndReturnResults(boolean preprocess){
		clean();
		HashMap<Class<? extends Evaluator>, EvaluationResults> results = new HashMap<>();
		if (preprocess){
			preprocess();
		}
		train();
		predict();
		String output = "";
		for (Evaluator eval : evaluators){
			results.put(eval.getClass(), evaluate(eval));
		}
		return results;
	}

	protected HashSet<Annotatable> getTestAnnotatables(){
		return testAnnotatables;
	}
	
	protected HashSet<Span> getTestData(){
		return dataSubSets.get(dataSubSets.size()-1);
	}
	/**
	 * Return all spans in a single HashSet (useful for the preprocess method)
	 * The distinction between training/test/etc set is lost, so be careful to
	 * not use this in the train() method
	 * @return
	 */
	protected HashSet<Span> getCombinedData(){
		HashSet<Span> allData = new HashSet<>();
		for (HashSet<Span> subset : dataSubSets){
			allData.addAll(subset);
		}
		return allData;
	}
	/**
	 * Get all training data, which is all subsets (also for validation etc.) except the test data subset
	 * @return
	 */
	protected ArrayList<HashSet<Span>> getTrainingData(){
		ArrayList<HashSet<Span>> allTrainingSubSets = new ArrayList<>();
		allTrainingSubSets.addAll(dataSubSets);
		allTrainingSubSets.remove(getTestData());
		return allTrainingSubSets;
	}
	/**
	 * Get all training data from (maybe) different subsets into one set. If there is a validation set, it is
	 * combined with the training data
	 * @return
	 */
	protected HashSet<Span> getCombinedTrainingData(){
		HashSet<Span> trainingData = new HashSet<>();
		for (int i = 0; i < dataSubSets.size()-1; i++){
			trainingData.addAll(dataSubSets.get(i));
		}
		return trainingData;
	}
	
	/**
	 * Method to encode the predictions into the data model so the resp. DataWriter can write it back to the proper file format
	 */
	public void annotateTestData(){
		
	}

	public String getUnitOfAnalysisSpanType(){
		return this.unitOfAnalysisSpanType;
	}
	
	public HashSet<Evaluator> getEvaluators(){
		return this.evaluators;
	}
	
	public String getLabel(){
		return label + " " + properties;
	}
	
	//Properties related methods to make it easier to use them without having to specify "properties." all the time
	
	public AbstractAlgorithm2 setProperty(String key, String value){
		properties.setProperty(key, value);
		return this;
	}
	public AbstractAlgorithm2 setBinaryProperties(String... keys){
		for (String key : keys){
			properties.setProperty(key, "");
		}
		return this;
	}
	public boolean hasProperty(String key){
		return properties.containsKey(key);
	}
	public String getProperty(String key){
		return properties.getProperty(key);
	}
	public String getPropertyOrDefault(String key, String defaultValue){
		return properties.getProperty(key, defaultValue);
	}
	
	
}