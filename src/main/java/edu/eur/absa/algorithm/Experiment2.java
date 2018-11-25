package edu.eur.absa.algorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.inference.TTest;

import edu.eur.absa.Framework;
import edu.eur.absa.data.IDataWriter;
import edu.eur.absa.evaluation.evaluators.Evaluator;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;

/**
 * Modified version of Experiment (@author Kim Schouten) to use AbstractAlgorithm2
 * 
 * @author Team5B
 */
public class Experiment2 {

	private HashSet<AbstractAlgorithm2> algs = new HashSet<>();
	private Dataset dataset=null;
	private Dataset testSet=null;
	private ArrayList<ArrayList<HashSet<Span>>> multipleDataSubSets = new ArrayList<>();
	private String unitOfAnalysisSpanType = null;
	private int nrFolds;
	private double[] subSetProportions;
	private int repetitions;
	
	private boolean run = false;
	private boolean dataSplit = false;
	
	
	private Experiment2(){}
	
	//factory methods - only available when experiment has not yet been performed
	public static Experiment2 createNewExperiment(){
		return new Experiment2();
	}
	public Experiment2 setDataset(Dataset dataset){
		if (!run && !dataSplit){
			this.dataset = dataset;
			multipleDataSubSets = new ArrayList<>();
		}
		return this;
	}
	
	public Experiment2 addAlgorithms(AbstractAlgorithm2... algs){
		if (!run){
			for (AbstractAlgorithm2 a : algs){
				if (unitOfAnalysisSpanType == null)
					unitOfAnalysisSpanType = a.getUnitOfAnalysisSpanType();
				if (unitOfAnalysisSpanType.equals(a.getUnitOfAnalysisSpanType())){
					this.algs.add(a);
				} else {
					Framework.error("You cannot directly compare algorithms that work on different units of analysis of the data set");
				}
			}
		}
		return this;
	}
	public Experiment2 setTrainingAndTestSet(Dataset trainingData, Dataset testData, double... trainingSubSetProportions){
		if (!run && !dataSplit && !algs.isEmpty()){
			this.dataset = trainingData;
			this.testSet = testData;
			multipleDataSubSets = new ArrayList<>();
			//single run always
			ArrayList<HashSet<Span>> singleRunAll = new ArrayList<>();
			singleRunAll.addAll(trainingData.createSubSets(unitOfAnalysisSpanType, trainingSubSetProportions));
			ArrayList<HashSet<Span>> singleRun = new ArrayList<>();
			singleRun.add(singleRunAll.get(0));
			HashSet<Span> testSet = new HashSet<>();
			testSet.addAll(testData.getSpans(unitOfAnalysisSpanType));
			singleRun.add(testSet);
			multipleDataSubSets.add(singleRun);
			repetitions = 1;
			nrFolds= 0;
			dataSplit = true;	
		}
		return this;
	}
	public Experiment2 setCrossValidation(int nrFolds, double... remainingSubSetProportions){
		return setCrossValidation(1, nrFolds, remainingSubSetProportions);
	}
	
	public Experiment2 setCrossValidation(int repetitions, int nrFolds, double... remainingSubSetProportions){
		if (!run && nrFolds > 1 && !dataSplit){
			this.nrFolds = nrFolds;
			this.subSetProportions = remainingSubSetProportions;
			this.repetitions = repetitions;
			double[] folds = new double[nrFolds];
			for (int i = 0; i < nrFolds; i++){
				folds[i] = (1.0/nrFolds);
			}
			for (int rep = 0; rep < repetitions; rep++){
				ArrayList<HashSet<Span>> testFolds = dataset.createSubSets(unitOfAnalysisSpanType, folds);
				TreeSet<Span> allData = dataset.getSpans(unitOfAnalysisSpanType);
				for (int i = 0; i < nrFolds; i++){
					TreeSet<Span> nonTestData = new TreeSet<>();
					nonTestData.addAll(allData);
					nonTestData.removeAll(testFolds.get(i));
					ArrayList<HashSet<Span>> foldSubSets = Dataset.createSubSets(nonTestData, remainingSubSetProportions);
					foldSubSets.add(testFolds.get(i));
					multipleDataSubSets.add(foldSubSets);
				}
			}
			dataSplit = true;	
		}
		
		return this;
	}
	public Experiment2 setSingleRun(double... subSetProportions){
		return setSingleRun(1, subSetProportions);
	}
	
	public Experiment2 setSingleRun(int repetitions, double... subSetProportions){
		if (!run && !dataSplit){
			nrFolds = 0;
			this.repetitions = repetitions;
			this.subSetProportions = subSetProportions;
			for (int rep = 0; rep < repetitions; rep++){
				ArrayList<HashSet<Span>> singleRun = new ArrayList<>();
				singleRun.addAll(dataset.createSubSets(unitOfAnalysisSpanType, subSetProportions));
				multipleDataSubSets.add(singleRun);
			}
			dataSplit = true;
		}
		return this;
	}
	
	public Experiment2 run() throws InstantiationException, IllegalAccessException{
		if (nrFolds+repetitions == 1){
			run(true, false);
		} else {
			run(false, true);
		}
		return this;
	}
	
	public Experiment2 run(boolean showAllResults, boolean showSummary) throws InstantiationException, IllegalAccessException{
		
		
		//results per evaluator, then per algorithm, then a list of results
		HashMap<Class<? extends Evaluator>, HashMap<AbstractAlgorithm2, ArrayList<EvaluationResults>>> allResults = new HashMap<>();
		
		for (AbstractAlgorithm2 alg : algs){
			boolean preprocessed = false;
			for (ArrayList<HashSet<Span>> subsets : multipleDataSubSets){
				alg.setDataSubSets(subsets);
				if (!preprocessed){
					alg.preprocess();
					preprocessed = true;
				}
					
				HashMap<Class<? extends Evaluator>, EvaluationResults> results = alg.executeAndReturnResults(false);
				
				for (Class<? extends Evaluator> evaluator : results.keySet()){
					if (showAllResults){
						Framework.log(alg.getLabel() + "\n" + results.get(evaluator).getEvaluationResults());
					}
					
					
					if (!allResults.containsKey(evaluator))
						allResults.put(evaluator, new HashMap<>());
					
					if (!allResults.get(evaluator).containsKey(alg))
						allResults.get(evaluator).put(alg, new ArrayList<>());
					
					allResults.get(evaluator).get(alg).add(results.get(evaluator));
					
				}
			}
		}
		
		//now create a combined report
		if (showSummary){
			for (Class<? extends Evaluator> e : allResults.keySet()){
				
				
				HashMap<AbstractAlgorithm2, ArrayList<EvaluationResults>> resultsForEvaluator = allResults.get(e);
				HashMap<AbstractAlgorithm2, double[]> resultsPerAlg = new HashMap<>();
				
				Framework.log("\n===============================\n"+
						resultsForEvaluator.values().iterator().next().get(0).getLabel()+
						"\n===============================\n");
				for (AbstractAlgorithm2 alg : resultsForEvaluator.keySet()){
					double[] numbers = new double[resultsForEvaluator.get(alg).size()];
					int rep = 0;
					for (EvaluationResults eval : resultsForEvaluator.get(alg)){
						if (showAllResults){
							Framework.log(eval.getEvaluationResults());
						}
						numbers[rep] = eval.getMainNumber();
						rep++;
					}
					resultsPerAlg.put(alg, numbers);
				}
				
				//compare the raw numbers
				
				StandardDeviation stdev = new StandardDeviation();
				TTest t = new TTest();
				for (AbstractAlgorithm2 alg1 : resultsForEvaluator.keySet()){
					Framework.log("\n"+alg1.getLabel());
					Framework.log("Average: "+average(resultsPerAlg.get(alg1)));
					Framework.log("St.dev.: "+stdev.evaluate(resultsPerAlg.get(alg1)));
					
					for (AbstractAlgorithm2 alg2 : resultsForEvaluator.keySet()){
						Framework.log("t-test with "+alg2.getLabel()+": "+t.pairedTTest(resultsPerAlg.get(alg1), resultsPerAlg.get(alg2)));
					}
				}
				
			}
		}
		run=true;
		return this;
	}
	public void savePredictions(IDataWriter dataWriter, File file){
		if (testSet != null && run){
			try {
				dataWriter.write(testSet, file);
			} catch (IOException e){
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	public void saveExperiment(File file){
		
	}
	public void loadExperiment(File file){
		
	}
	
	public static double average(double[] scores){
		double avg = 0;
		for (int i = 0; i < scores.length; i++){
			avg += scores[i];
		}
		return avg/scores.length;
	}
	
}
