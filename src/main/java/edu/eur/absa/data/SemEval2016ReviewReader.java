package edu.eur.absa.data;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import edu.eur.absa.Framework;
import edu.eur.absa.external.IOntology;
import edu.eur.absa.external.ontology.ReasoningOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.CoreNLPDependencyParser;
import edu.eur.absa.nlp.CoreNLPLemmatizer;
import edu.eur.absa.nlp.CoreNLPNamedEntityRecognizer;
import edu.eur.absa.nlp.CoreNLPParser;
import edu.eur.absa.nlp.CoreNLPPosTagger;
import edu.eur.absa.nlp.CoreNLPSentimentAnnotator;
import edu.eur.absa.nlp.CoreNLPTokenizer;
import edu.eur.absa.nlp.DependencyDistanceAnnotator;
import edu.eur.absa.nlp.NLPTask;
import edu.eur.absa.nlp.OntologyLookup;

/**
 * A reader for subtask2 of SemEval2016 Task 5 (Review level ABSA).
 * Very similar to the SemEval2015 reader, but opinion elements are now added to the review element
 *   instead to sentence elements in the XML.
 * @author Kim Schouten
 *
 */
public class SemEval2016ReviewReader implements IDataReader {

	//test
	public static void main(String[] args) throws Exception{
		Dataset test = (new SemEval2016ReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB2_Restaurants_Train_Data.xml"));
		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"SemEval2016SB2Restaurants-Training.json"));
		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"SemEval2016SB2Restaurants-Training.pretty.json"));
		
		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SemEval2016SB2Restaurants-Training.json"));
		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"Check.json"));
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		//review is the textual unit, as sentences within a single review are semantically connected
		Dataset dataset = new Dataset(file.getName(),"review");
		
		HashSet<Span> createdOpinions = new HashSet<>();
		
		Document document = new Builder().build(file);
		//root is the <reviews> element
		Element root = document.getRootElement();
		Elements reviewElements = root.getChildElements();
		for (int i = 0; i < reviewElements.size(); i++){
			Element reviewElement = reviewElements.get(i);
			String reviewId = reviewElement.getAttributeValue("rid");
			Span reviewSpan = new Span("review", dataset);
			reviewSpan.putAnnotation("id", reviewId);
			String reviewText = "";
			Elements sentenceElements = reviewElement.getChildElements("sentences").get(0).getChildElements();
			for (int j = 0; j < sentenceElements.size(); j++){
				Element sentenceElement = sentenceElements.get(j);
				String sentenceId = sentenceElement.getAttributeValue("id");
				String text = sentenceElement.getChildElements("text").get(0).getValue();
				Span sentenceSpan = new Span("sentence", reviewSpan);
				sentenceSpan.putAnnotation("id", sentenceId);
				sentenceSpan.putAnnotation("text", text);
				reviewText += text;
				
			}
			boolean hasOpinions = reviewElement.getChildElements("Opinions").size()>0;
			if (hasOpinions){
				Elements opinionElements = reviewElement.getChildElements("Opinions").get(0).getChildElements();
				for (int k = 0; k < opinionElements.size(); k++){
					Element opinionElement = opinionElements.get(k);
					String category = opinionElement.getAttributeValue("category");
					String polarity = opinionElement.getAttributeValue("polarity");
					Span opinionSpan = new Span("opinion", reviewSpan.getTextualUnit());
					opinionSpan.putAnnotation("category", category);
					opinionSpan.putAnnotation("polarity", polarity);
					createdOpinions.add(opinionSpan);
				}
			}
			reviewSpan.putAnnotation("text", reviewText);
		}
		dataset.getPerformedNLPTasks().add(NLPTask.SENTENCE_SPLITTING);
		dataset.process(new CoreNLPTokenizer(), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
			.process(new OntologyLookup(null, (IOntology) new ReasoningOntology(Framework.EXTERNALDATA_PATH + "RestaurantSentiment.owl")), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")
			.process(new CoreNLPNamedEntityRecognizer(), "sentence")
			.process(new CoreNLPParser(), "sentence")
			.process(new CoreNLPDependencyParser(), "sentence")
			.process(new CoreNLPSentimentAnnotator(), "sentence")
			;		
		
		//fix opinion spans so they cover the whole review
		// we cannot do this earlier on, because we only get Word objects after tokenization,
		// so when creating the review span and opinion span, both are still empty
		for (Span opinion : createdOpinions){
			opinion.addAll(opinion.getTextualUnit());
		}
		
		return dataset;
	}

}
