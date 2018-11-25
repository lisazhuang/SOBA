package edu.eur.absa.seminarhelper;

import java.io.File;
import java.util.HashSet;

import edu.eur.absa.Framework;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

/**
 * A program for getting the synonyms of a word.
 * 
 * @author Karoliina Ranta
 *
 */
public class Synonyms {
	private String word;
	private File f;
	
	/**
	 * A constructor for the GetSynonyms object.
	 * @param w, the word of which you want the synonyms
	 */
	public Synonyms(String w) {
		this.word = w;
		f = new File(Framework.EXTERNALDATA_PATH + "/WordNet-3.0/dict");
		System.setProperty("wordnet.database.dir", f.toString());
	}
	
	/**
	 * A method for getting all synonyms of the word.
	 * @return a HashSet containing the words synonyms.
	 */
	public HashSet<String> synonyms() {
		HashSet<String> synonyms = new HashSet<String>();
		WordNetDatabase wordDatabase = WordNetDatabase.getFileInstance();
		Synset[] synsets = wordDatabase.getSynsets(word);
		if (synsets.length > 0){
			for (int i = 0; i < synsets.length; i++) {
				String[] wordForms = synsets[i].getWordForms();
				for (int j = 0; j < wordForms.length; j++) {
					synonyms.add(wordForms[j]);
				}
			}
		}
		return synonyms;
 	}
}
