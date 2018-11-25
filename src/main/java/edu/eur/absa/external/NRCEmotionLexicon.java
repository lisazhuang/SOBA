package edu.eur.absa.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.eur.absa.Framework;


public class NRCEmotionLexicon {

	private HashMap<String, HashSet<String>> dictionary;		//key = category, giving all words in that category
	private HashMap<String, HashSet<String>> reverseDictionary;	//key = word, giving all categories associated with that word
	
	public NRCEmotionLexicon(){
		
		dictionary = new HashMap<>();
		reverseDictionary = new HashMap<>();
        try
        {
            BufferedReader reader =  new BufferedReader(new FileReader(Framework.LIB_PATH + "SentimentLexicons/NRC-emotion-lexicon-wordlevel-alphabetized-v0.92.txt"));
            String line;
            
            while((line = reader.readLine()) != null){		
	            String[] cats = line.split("\t");
	            String word = cats[0];
	            String category = cats[1];
	            int assoc = Integer.parseInt(cats[2]);
	            if (assoc == 1){
	            	if (!dictionary.containsKey(category))
	            		dictionary.put(category, new HashSet<String>());
	            	dictionary.get(category).add(word);
	            	
	            	if (!reverseDictionary.containsKey(word))
	            		reverseDictionary.put(word, new HashSet<String>());
	            	reverseDictionary.get(word).add(category);
	            }
            }
            
            
            reader.close();
        }
        catch(IOException | NumberFormatException e)
        {
            Logger.getLogger(NRCEmotionLexicon.class.getName()).log(Level.SEVERE, null, e);
        }
		
	}
	
	public HashSet<String> getWords(String category){
		HashSet<String> words = new HashSet<String>();
		if (dictionary.containsKey(category)){
			words.addAll(dictionary.get(category));
		}
		return words;
	}
	
	public HashSet<String> getCategories(String word){
//		HashSet<String> cats = new HashSet<String>();
//		if (reverseDictionary.containsKey(word)){
//			cats.addAll(reverseDictionary.get(word));
//		}
//		return cats;
		if (reverseDictionary.containsKey(word)){
			return reverseDictionary.get(word);
		} else {
			return new HashSet<String>();
		}
	}
	
	public boolean wordIsInCategory(String word, String category){
		return dictionary.containsKey(category) && dictionary.get(category).contains(word);
	}
	
	public Set<String> getCategories(){
		return dictionary.keySet();
	}
	
	public static void main(String[] args){
		NRCEmotionLexicon gi = new NRCEmotionLexicon();
		System.out.println(gi.getWords("positive"));
		
	}
	
}
