package edu.eur.absa.seminarhelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.eur.absa.Framework;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
/**
 * An implementation of the (simplified) Lesk algorithm for differentiating between different senses 
 * of a word (noun) given its context (the words around it), using WordNet.
 * 
 * @author Abbaan Nassar
 *
 */
public class WordSenseDisambiguation {


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//String[] test4 = wordsToArray("greece turkey is a beautiful country couscous officially the Republic of turkey");
		//System.out.println(Sense(test4, "turkey"));
		//Synset synset = findSynset(test4, "turkey", "noun");
		
		File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", f.toString());
		WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();
		Synset[] synset = wordDatabase.getSynsets("asdfa");

		if (synset.length < 0)
		{
			System.out.println("how");
		}
	}


	/**
	 * Method returns an integer (starting from 0) representing the WordNet sense of the word
	 * 
	 * @param w Word for which you want to know the sense
	 * @param s String of words which are the context in which the word appears (has to include the word itself)
	 *
	 */
	public static Synset findSynset(String[] s, String w, String pos) {
		//boolean found =false;
		int index = 0;
		int n = s.length;
		File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", f.toString());
		WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();
		//for (int i = 0; i<n;i++) { //check if the word itself is in the context
		//	if (s[i].equals(w)) {
		//		index = i;
		//		found = true;
		//		break;
		//	}
		//}
		//if (found == false) {
		//	return wordDatabase.getSynsets(w)[0];
		//}
		Synset sense = null;
		if (wordDatabase.getSynsets(w).length>0) {
		sense = wordDatabase.getSynsets(w)[0]; //give most frequent option by default
		int maxScore = -1;
		Synset[] synsets1 = wordDatabase.getSynsets(w);
		for (int j = 0; j < synsets1.length; j++) {
			String type = synsets1[j].getType().toString();
			if((type.equals("1") && pos.equals("noun"))||(type.equals("2") && pos.equals("verb"))||(type.equals("3") && pos.equals("adjective"))) {
				int score = 0;
				String def1 = synsets1[j].getDefinition();
				String[] examples = synsets1[j].getUsageExamples();
				for (int e = 0; e<examples.length;e++) {
					def1 = def1 + " " + examples[e];
				}
				String[] A1 = wordsToArray(def1);

				for (int i = 0; i<n;i++) {
					if (i != index) {
						//simplified Lesk algorithm
						for(int c = 0; c< A1.length;c++) {
							if (s[i].equals(A1[c])) {
								score++;
							}
						}
						//original Lesk algorithm
						//				Synset[] synsets = wordDatabase.getSynsets(s[i]);
						//				if (synsets.length > 0){
						//					for (int k = 0; k < synsets.length; k++) {
						//						String def = synsets[k].getDefinition();
						//						String[] A = wordsToArray(def);
						//						score = score + Overlap(A,A1);
						//						
						//					}
						//			    }

					}
				}

				if (score > maxScore) {
					maxScore = score;
					sense = synsets1[j];
				}

			}
		}
		}
		return sense;

	}

	/**
	 * Method returns an integer (starting from 0) representing the WordNet sense of the word
	 * 
	 * @param w Word for which you want to know the sense
	 * @param s String of words which are the context in which the word appears (has to include the word itself)
	 *
	 */
	public static int Sense(String[] s, String w, String pos) {
		//boolean found =false;
		int index = 0;
		int n = s.length;
		File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", f.toString());
		WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();

		int sense = 0;  //most frequent one by default
		int maxScore = -1;
		Synset[] synsets1 = wordDatabase.getSynsets(w);
		for (int j = 0; j < synsets1.length; j++) {
			String type = synsets1[j].getType().toString();
			int score = 0;
			if((type.equals("1") && pos.equals("noun"))||(type.equals("2") && pos.equals("verb"))||(type.equals("3") && pos.equals("adjective"))) {
				String def1 = synsets1[j].getDefinition();
				String[] examples = synsets1[j].getUsageExamples();
				for (int e = 0; e<examples.length;e++) {
					def1 = def1 + " " + examples[e];
				}
				String[] A1 = wordsToArray(def1);
				for (int i = 0; i<n;i++) {
					if (i != index) {
						//simplified Lesk algorithm
						for(int c = 0; c< A1.length;c++) {
							if (s[i].equals(A1[c])) {
								score++;
							}
						}
					}
				}
				if (score > maxScore) {
					maxScore = score;
					sense = j;
				}

			}
		}
		return sense;
	}


	public static String[] wordsToArray(String s) {
		File f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", f.toString());
		WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();

		String[] words = s.split("\\s+");
		for (int i = 0; i < words.length; i++) {
			Synset[] synsets = wordDatabase.getSynsets(words[i]);
			if (synsets.length < 1) {
				words[i]="";
			}
			if (words[i].equals("are") ||words[i].equals("where") ||words[i].equals("have") ||words[i].equals("not") ||words[i].equals("be") ||words[i].equals("the") || words[i].equals("in") || words[i].equals("as") || words[i].equals("by") || words[i].equals("with") || words[i].equals("a") || words[i].equals("and") || words[i].equals("on") || words[i].equals("of") || words[i].equals("that") || words[i].equals("an") || words[i].equals("or") || words[i].equals("is")|| words[i].equals("which") || words[i].equals("for")) {
				words[i] = "";
			}
		}

		Set<String> h = new HashSet<String>();
		for(int i = 0; i < words.length;i++) {
			if (!words[i].equals("")){
				h.add(words[i]);
			}
		}
		String[] words2 = new String[h.size()];
		int i = 0;
		for(String e : h) {
			words2[i] = e;
			i++;	
		}

		return words2;

	}



	public static int Overlap(String[] a, String[] b) {
		int counter = 0;
		HashSet<String> map = new HashSet<String>();
		for (String i : a) {
			map.add(i); 
		}
		for (String i : b) {
			if (map.contains(i)) {
				counter++;
			}        
		}
		return counter;
	}

}