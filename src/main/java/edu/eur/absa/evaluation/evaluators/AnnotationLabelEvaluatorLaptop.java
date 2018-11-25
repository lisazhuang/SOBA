package edu.eur.absa.evaluation.evaluators;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import edu.eur.absa.algorithm.Prediction;
import edu.eur.absa.data.SeminarWriter;
import edu.eur.absa.Framework;
import edu.eur.absa.evaluation.results.ClassificationResults;
import edu.eur.absa.evaluation.results.EvaluationResults;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.Annotatable;
import edu.eur.absa.model.DataEntity;
import edu.eur.absa.model.Dataset;

public class AnnotationLabelEvaluatorLaptop implements Evaluator {

	private String spanType;
	private String annotationType;
	private boolean failureAnalysis;
	private boolean writeOutput;
	private SeminarWriter outputWriter;
	
	public AnnotationLabelEvaluatorLaptop(String spanType, String annotationType, boolean failAn){
		this(spanType, annotationType, failAn, null);
	}
	
	public AnnotationLabelEvaluatorLaptop(String spanType, String annotationType, boolean failAn, SeminarWriter writer){
		this.spanType = spanType;
		this.annotationType = annotationType;
		this.failureAnalysis = failAn;
		if (writer != null) {
			this.outputWriter = writer;
			writeOutput = true;
		}
		else {
			writeOutput = false;
		}
	}
	
	@Override
	public EvaluationResults evaluate(HashSet<? extends DataEntity> testSet,
			HashMap<? extends DataEntity, HashSet<Prediction>> predictions, HashMap<? extends DataEntity, HashSet<String>> features) {
		int truePos=0;
		int falsePos=0;
		int falseNeg=0;
		
		for (DataEntity parentSpan : testSet){
			
			
			boolean annotated = parentSpan.hasAnnotation(annotationType);
			if (predictions.containsKey(parentSpan)){
				
				HashSet<Prediction> preds = predictions.get(parentSpan);
				//only a single prediction is performed for this type of problem
				
				Prediction singlePrediction = preds.iterator().next();
				boolean predicted = singlePrediction.hasAnnotation(annotationType); 
//				Main.debug("Prediction found..."+singlePrediction.getAnnotations());
//				Main.debug("Gold values: "+parentSpan.getAnnotations());
				if (predicted && annotated){
					//check the values
					Object predObj = singlePrediction.getAnnotation(annotationType);
					Object annotObj = parentSpan.getAnnotation(annotationType);
					
					/*Variables for wrtieOutput and failureAnalysis*/
					Dataset data = parentSpan.getDataset();
					Span review = ((Span) parentSpan).getTextualUnit();
					String category = ((Span) parentSpan).getAnnotation("category");
					
					/*Write to output*/
					if (writeOutput) {
						outputWriter.add(review, category, predObj, annotObj);
					}
					
					if (predObj.equals(annotObj)){
						truePos++;
					} else {
						falsePos++;
						falseNeg++;
						
						if (failureAnalysis){
							String reviewText = ((Span) parentSpan).getTextualUnit().getAnnotation("text", String.class);
							String id = ((Span) parentSpan).getTextualUnit().getAnnotation("id", String.class);
							//Span review = data.getSpans("review", ((Span)parentSpan).first()).first();
							//String reviewText = "";
							//for (Word w : (Span) review) {
							//	reviewText += " " + w;
							//}
							Framework.log("\n\nID: "+id+"\t"+reviewText.trim() + "\ncategory: " +category+" predicted: " + predObj.toString() + ", correct: " + annotObj.toString());
						}		
					}
				} else if (predicted && !annotated){
					falsePos++;
				} else if (!predicted && annotated){
					falseNeg++;
//					Main.debug("False neg:\n"+parentSpan.getAnnotations()+"\n"+singlePrediction.getAnnotations());
					
				}
				
			} else {
				//no prediction, check if it's a false neg
				if (annotated){
					falseNeg++;
//					Main.debug("No prediction found for: "+parentSpan);
				}
				
			}
		}
		
		if (writeOutput) {
			try {
				outputWriter.write(null, new File(Framework.DATA_PATH+"ThesisLaptop.predictionsALLTest.json"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return new ClassificationResults(getLabel(), truePos, falsePos, falseNeg);
	}

	@Override
	public String getLabel() {
		return "Classification results of annotation label '"+annotationType+"':";
	}

}
