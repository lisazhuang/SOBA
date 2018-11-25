package edu.eur.absa.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.nlp.NLPTask;

/**
 * This class can read a single file or directory of files where each file is the output
 *  of the frog natural language processor (for Dutch)
 * Frog is a binary that cannot easily be loaded in the framework, so that has to be run separately.
 * @author Kim Schouten
 *
 */
public class FrogReader implements IDataReader {

	private String textualUnit = "document";
	private String frogOutputFileExtension = ".out";
	
	public static void main(String[] args) throws Exception{
//		Dataset test = (new FrogReader()).read(new File("C:/workspace/alettesproject/data/items/"));
//		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"Aardbeving-Media.json"));
//		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"Aardbeving-Media.pretty.json"));
//		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"Aardbeving-Media.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"Check.json"));
//	
		
		Dataset test = (new FrogReader()).read(new File("C:/workspace/alettesproject/data/politicalItems/"));
		(new DatasetJSONWriter()).write(test, new File(Framework.DATA_PATH+"Aardbeving-Debatten.json"));
		(new DatasetJSONWriter(true)).write(test, new File(Framework.DATA_PATH+"Aardbeving-Debatten.pretty.json"));
		
//		Dataset check = (new DatasetJSONReader()).read(new File(Framework.DATA_PATH+"Aardbeving-Debatten.json"));
//		(new DatasetJSONWriter(true)).write(check, new File(Framework.DATA_PATH+"Check.json"));
//	
	}
	
	public FrogReader(){
	}
	
	public FrogReader(String frogOutputFileExtension){
		this.frogOutputFileExtension = frogOutputFileExtension;
	}
	
	@Override
	public Dataset read(File file) throws Exception {
		Framework.log(file.getName());
		Dataset dataset = new Dataset(file.getName(),textualUnit);
		if (file.isDirectory()){
			File[] files = file.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(frogOutputFileExtension);
				}
			});
			for (File f : files){
				Framework.log(f.getName());
				readDocument(f, dataset);
			}
		} else {
			Framework.log(file.getName());
			readDocument(file, dataset);
		}
		
		Framework.log(""+dataset.getAnnotationDataTypes());
		
		dataset.getAnnotationDataTypes().put("morphSegments", ArrayList.class);
		dataset.getAnnotationDataTypes().put("posDetails", ArrayList.class);
		
		
		dataset.getPerformedNLPTasks().add(NLPTask.TOKENIZATION);
		dataset.getPerformedNLPTasks().add(NLPTask.SENTENCE_SPLITTING);
		dataset.getPerformedNLPTasks().add(NLPTask.POS_TAGGING);
		dataset.getPerformedNLPTasks().add(NLPTask.LEMMATIZATION);
		dataset.getPerformedNLPTasks().add(NLPTask.NER);
		dataset.getPerformedNLPTasks().add(NLPTask.CHUNKING);
		
		return dataset;
	}

	public void readDocument(File file, Dataset dataset) throws IOException{
		Span doc = null;
		Span sentence = null;
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		int offset = 0;
		Word previousWord = null;
		String sentenceText = "";
		String documentText = "";
		while ((line = in.readLine())!=null){
			
			//Framework.log(line);
			if (line.trim().length() > 0){
				
				//if not a new line, this is another word for the current sentence
				
				//start doc if we haven't done so already
				if (doc == null){
//					Framework.log("Starting new document");
					doc = new Span(textualUnit, dataset);
				}
				
				String[] parts = line.split("\t");
				
				if (parts[0].equalsIgnoreCase("1")){
					//a new sentence starts
//					Framework.log("Starting new sentence");
					if (sentence!=null){
//						sentence.getAnnotations().put("text", sentenceText.trim());
					}
					sentence = new Span("sentence", doc);
					sentenceText = "";
					
				}
				//Framework.log("Creating Word object");
				String text = parts[1].replaceAll("[_]", " ");
				String lemma = parts[2].replaceAll("[_]", " ");
				String morphSegmentsText = parts[3];
				morphSegmentsText = morphSegmentsText.substring(1, morphSegmentsText.length()-1);
				String[] morphSegments = morphSegmentsText.split("[\\]][_]?[\\[]");
				String posText = parts[4];
				String pos = posText.substring(0, posText.indexOf("("));
				String posDetails[] = posText.substring(posText.indexOf("(")+1, posText.indexOf(")")).split(",");
				double posConfidence = Double.parseDouble(parts[5]);
				String ner = parts[6].split("[_]")[0];
				String chunk = parts[7].split("[_]")[0];
				Word word;
				if (previousWord == null){
					word = new Word(text, offset, doc, dataset);
				} else {
					word = new Word(text, offset, previousWord);
				}
				previousWord = word;
				offset += text.length()+1;
				sentenceText += text + " ";
				documentText += text + " ";
				
//				word.getAnnotations().put("lemma", lemma);
//				word.getAnnotations().put("pos", pos);
//				word.getAnnotations().put("posConfidence", posConfidence);
//				word.getAnnotations().put("ner", ner);
//				word.getAnnotations().put("chunk", chunk);
//				
//				ArrayList<String> morphs = new ArrayList<String>();
//				for (String morph : morphSegments){
//					morphs.add(morph);
//				}
//				word.getAnnotations().put("morphSegments", morphs);
//				
//				ArrayList<String> posDets = new ArrayList<String>();
//				for (String posDet : posDetails){
//					posDets.add(posDet);
//				}
//				word.getAnnotations().put("posDetails", posDets);
				sentence.add(word);
				
			}
			
			
		}
		if (doc!=null){
//			sentence.getAnnotations().put("text", sentenceText.trim());
//			doc.getAnnotations().put("text", documentText);
		}
		in.close();
	}
}
