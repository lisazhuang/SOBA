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
import edu.eur.absa.seminarhelper.SeminarOntology;

/**
 * A reader for subtask2 of SemEval2016 Task 5 (Review level ABSA) bachelor Seminar classification model.
 * Very similar to the SemEval2015 reader. Opinion elements are added to the sentence element
 *    in the XML.
 *   
 * @author Kim Schouten
 * 
 * Adapted by Abbaan Nassar
 *
 */
public class SeminarReviewReader implements IDataReader {

	public static void main(String[] args) throws Exception{
		
		/* Manual restaurant ontology */
		
		//Training data restaurant
//		Dataset test = (new SeminarReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Restaurants_Train_Data.xml"));
//		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"SeminarRestaurants-Training2.json"));
//		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"SeminarRestaurants-Training2.pretty.json"));
//		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarRestaurants-Training2.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"CheckSeminarTrain2.json"));
		
		//Test data restaurant
//		Dataset test = (new SeminarReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Restaurants_Test_Gold.xml"));
//		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"SeminarRestaurants-Test2.json"));
//		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"SeminarRestaurants-Test2.pretty.json"));
//		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarRestaurants-Test2.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"CheckSeminarTest2.json"));
		
		/* Automatic restaurant ontology */
		
		//Training data restaurant
//		Dataset test = (new SeminarReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Restaurants_Train_Data.xml"));
//		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"SeminarRestaurants-TrainingAuto5001.json"));
//		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"SeminarRestaurants-TrainingAuto5001.pretty.json"));
		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarRestaurants-TrainingAuto5001.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"CheckSeminarTrainAuto5001.json"));
		
		//Test data restaurant
//		Dataset test2 = (new SeminarReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Restaurants_Test_Gold.xml"));
//		(new DatasetJSONWriter()).write(test2, new File(Framework.DATA_PATH+"SeminarRestaurants-TestAuto5001.json"));
//		(new DatasetJSONWriter(true)).write(test2, new File(Framework.DATA_PATH+"SeminarRestaurants-TestAuto5001.pretty.json"));
		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarRestaurants-TestAuto5001.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"CheckSeminarTestAuto5001.json"));
		
		/* Automatic laptop ontology */
		
		//Training data laptop
		Dataset test3 = (new SeminarReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Laptops_Train_Data.xml"));
		(new DatasetJSONWriter()).write(test3, new File(Framework.DATA_PATH+"SeminarLaptops-TrainingAuto5001.json"));
		(new DatasetJSONWriter(true)).write(test3, new File(Framework.DATA_PATH+"SeminarLaptops-TrainingAuto5001.pretty.json"));
		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarLaptops-TrainingAuto5001.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"CheckSeminarLaptopTrainAuto5001.json"));
		
		//Test data laptop
		Dataset test4 = (new SeminarReviewReader()).read(new File(Framework.RAWDATA_PATH + "ABSA-16_SB1_Laptops_Test_Gold.xml"));
		(new DatasetJSONWriter()).write(test4, new File(Framework.DATA_PATH+"SeminarLaptops-TestAuto5001.json"));
		(new DatasetJSONWriter(true)).write(test4, new File(Framework.DATA_PATH+"SeminarLaptops-TestAuto5001.pretty.json"));
		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"SeminarLaptops-TestAuto5001.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"CheckSeminarLaptopTestAuto5001.json"));
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		//review is the textual unit, as sentences within a single review are semantically connected
		Dataset dataset = new Dataset(file.getName(),"sentence");
		
		HashSet<Span> createdOpinions = new HashSet<>();
		
		Document document = new Builder().build(file);
		//root is the <reviews> element
		Element root = document.getRootElement();
		Elements reviewElements = root.getChildElements();
		for (int i = 0; i < reviewElements.size(); i++){
			Element reviewElement = reviewElements.get(i);
			
			Elements sentenceElements = reviewElement.getChildElements("sentences").get(0).getChildElements();
			for (int j = 0; j < sentenceElements.size(); j++){
				Element sentenceElement = sentenceElements.get(j);
				String sentenceId = sentenceElement.getAttributeValue("id");
				String text = sentenceElement.getChildElements("text").get(0).getValue();
				Span sentenceSpan = new Span("sentence", dataset);
				sentenceSpan.putAnnotation("id", sentenceId);
				sentenceSpan.putAnnotation("text", text);
				
			
			boolean hasOpinions = sentenceElement.getChildElements("Opinions").size()>0;
			if (hasOpinions){
				Elements opinionElements = sentenceElement.getChildElements("Opinions").get(0).getChildElements();
				for (int k = 0; k < opinionElements.size(); k++){
					Element opinionElement = opinionElements.get(k);
					String category = opinionElement.getAttributeValue("category");
					String polarity = opinionElement.getAttributeValue("polarity");
					Span opinionSpan = new Span("opinion", sentenceSpan);
					opinionSpan.putAnnotation("category", category);
					opinionSpan.putAnnotation("polarity", polarity);
					createdOpinions.add(opinionSpan);
				}
			}
			}
		}
		dataset.getPerformedNLPTasks().add(NLPTask.SENTENCE_SPLITTING);
		dataset.process(new CoreNLPTokenizer(), "sentence")
			.process(new CoreNLPPosTagger(), "sentence")
			.process(new CoreNLPLemmatizer(), "sentence")	
//			.process(new OntologyLookup(null, new SeminarOntology(Framework.EXTERNALDATA_PATH + "LaptopOntologySeminarAuto5001.owl")), "sentence") //semi-automatic laptop
			.process(new OntologyLookup(null, (IOntology) new SeminarOntology(Framework.EXTERNALDATA_PATH + "RestaurantOntologySA.owl")), "sentence") //semi-automatic restaurant
//			.process(new OntologyLookup(null, new SeminarOntology(Framework.EXTERNALDATA_PATH + "RestaurantOntologySeminar.owl")), "sentence") //manual restuarant
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