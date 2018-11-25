package edu.eur.absa.seminarhelper;

import java.util.List;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

/**
 * This program calculates the WuPalmer Similarity measure of two words given their WordNet senses
 * 
 * @author Abbaan Nassar
 *
 */
public class Wu_Palmer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(WupSimilarity("Turkey",1,"bird",0,"n"));
		

	}
	
	/**
	 * Method calculates and returns the WuPalmer similarity measure (a number between zero and one)
	 * 
	 * @param word1 The first word
	 * @param word2 The second word
	 * @param sense1 The Wordnet sense of the first word (integer starting from 0 for first sense)
	 * @param sense2 The Wordnet sense of the second word (integer starting from 0 for first sense)
	 * @param pos The type of the words, "n" for noun, "v" for verb etc.
	 *
	 */
	public static double WupSimilarity(String word1, int sense1, String word2, int sense2, String pos ) {
		
		ILexicalDatabase db = new NictWordNet();
		WS4JConfiguration.getInstance().setMFS(true);
		RelatednessCalculator rc = new WuPalmer(db);
	
		    List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(word1, pos);
		    List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(word2, pos);

		    if(synsets1.size()>sense1 && synsets2.size()>sense2) {
		    	Relatedness relatedness = rc.calcRelatednessOfSynset(synsets1.get(sense1), synsets2.get(sense2));
	            double score = relatedness.getScore();
	            return score;
		    }
		    else {
		    	return 0.0;
		    }
		    
	}

}