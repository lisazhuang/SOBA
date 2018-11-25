package edu.eur.absa.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.eur.absa.Framework;
import edu.eur.absa.algorithm.AbstractAlgorithm;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;

public class SeminarWriter implements IDataWriter {

	private boolean writeSentences = false;
	private JSONArray datasetJSON;
	
	public SeminarWriter(){
		this(false);
	}
	
	public SeminarWriter(boolean writeSentences){
		this.writeSentences = writeSentences;
		datasetJSON = new JSONArray();

	}
	
	public void add(Span span, String cat, Object prediction, Object goldVal) {
		JSONObject instance = new JSONObject();
		instance.put("review-id", span.getTextualUnit().getAnnotation("id", String.class));
		if (!span.getAnnotation("id", String.class).equals(span.getTextualUnit().getAnnotation("id", String.class))) {
			instance.put("sentence-id", span.getAnnotation("id", String.class));
		}
		instance.put("category", cat);
		instance.put("gold-value", goldVal);
		instance.put("prediction", prediction);
		this.datasetJSON.put(instance);
		
	}
	
	@Override
	public void write(Dataset dataset, File file) throws IOException {
		Framework.debug("ThesisWriter: Start writing " + file + "...");
//		JSONArray datasetJSON = new JSONArray();
//		
//		for (Span span : dataset.getSpans("opinion")){//dataset.getTextualUnitSpanType())){
//			JSONObject review = new JSONObject();
//			review.put("textualUnitId", span.getTextualUnit().getAnnotations().get("id", String.class));
//			review.put("category", span.getAnnotations().get("category", String.class));
//			review.put("goldPolarity", span.getAnnotations().get("polarity", String.class));
//			review.put("predicted-polarity", span.getAnnotations().get("predicted-polarity", String.class));
//			if (writeSentences)
//				review.put("text", span.getTextualUnit().getAnnotations().get("text", String.class));
//			datasetJSON.put(review);
//		}
		
		BufferedWriter out;
		out = new BufferedWriter(new FileWriter(file));
//		if (prettyJSON){
			out.write(datasetJSON.toString(2));
//		} else {
//			out.write(datasetJSON.toString());
//		}
		out.close();
	}

	@Override
	public void write(Dataset dataset, AbstractAlgorithm alg, File file) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean supportsWritingPredictions() {
		// TODO Auto-generated method stub
		return false;
	}

}
