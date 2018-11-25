package edu.eur.absa.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.eur.absa.Framework;

public class SentiWordNet {

/*
 * Original version from: http://sentiwordnet.isti.cnr.it/code/SWN3.java
 * Edited by: Marnix Moerland
 * Edited again by: Kim Schouten
 */

    
    private HashMap<String, ArrayList<Double>> dictionary;
    private HashMap<Integer, ArrayList<Double>> synsetDictionary;
    
    
    
    public SentiWordNet(){
    	this(Framework.LIB_PATH);
    }
    
    public SentiWordNet(String path)
    {
   	
        dictionary = new HashMap<>();
        synsetDictionary = new HashMap<>();
        try
        {
            BufferedReader reader =  new BufferedReader(new FileReader(path + "SentiWordNet/SentiWordNet_3.0.0_20130122.txt"));
            String line;		
            while((line = reader.readLine()) != null)
            {
                String[] data = line.split("\t");
                Double pos = Double.parseDouble(data[2]);
                Double neg = Double.parseDouble(data[3]);
                String[] words = data[4].split(" ");
                ArrayList<Double> score = new ArrayList<Double>();
                score.add(pos);
                score.add(neg);
                score.add(1-pos-neg);
                for(String word:words)
                {
                    word = (word.split("#"))[0];
                    if(!dictionary.containsKey(word)) dictionary.put(word, score);
                }
                Integer id = Integer.parseInt(data[1]);
                synsetDictionary.put(id, score);
            }
            reader.close();
        }
        catch(IOException | NumberFormatException e)
        {
            Logger.getLogger(SentiWordNet.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    // don't use this method, since it is not well defined which sense of the word is returned
    public int extractRoundedSentiment(String word)
    {
        ArrayList<Double> scores = dictionary.get(word);
        if (scores == null){
        	return 0;
        } else {
        	double score = scores.get(0) - scores.get(1);
        	if (score > 0.0) return 1;
        	else if(score < 0.0) return -1;
        	else return 0;
        }
    }
    // don't use this method, since it is not well defined which sense of the word is returned
    public double extractExactSentiment(String word)
    {
        ArrayList<Double> scores = dictionary.get(word);
        if (scores == null){
        	return 0;
        } else {
        	return scores.get(0) - scores.get(1);
        }
    }

    //make sure you give this method the offset or WordNet id of a WordNetConcept object, not its getId() method
    public int extractRoundedSentiment(Integer id)
    {
    	if (!hasID(id)){
    		try {
    			throw new Exception("This offset ("+id+") could not be found in SentiWordNet. Are you sure you used the getOffset() method, and not the getId() method on the WordNetConcept object?");
    		} catch (Exception e){
    			e.printStackTrace();
    		}
        	return 0;
        } else {
        	ArrayList<Double> scores = synsetDictionary.get(id);
        	double score = scores.get(0) - scores.get(1);
        	if (score > 0.0) return 1;
        	else if(score < 0.0) return -1;
        	else return 0;
        }
    }

    public double extractExactSentiment(Integer id)
    {
    	if (!hasID(id)){
    		try {
    			throw new Exception("This offset ("+id+") could not be found in SentiWordNet. Are you sure you used the getOffset() method, and not the getId() method on the WordNetConcept object?");
    		} catch (Exception e){
    			e.printStackTrace();
    		}
        	return 0;
        } else {
        	ArrayList<Double> scores = synsetDictionary.get(id);
        	return scores.get(0) - scores.get(1);
        }
    }

    public ArrayList<Double> getScores(Integer id){
    	return synsetDictionary.get(id);
    }
    
    public boolean hasWord(String word)
    {
        if (dictionary.containsKey(word))
            return true;
        else
            return false;
    }

    public boolean hasID(Integer id)
    {
        if(synsetDictionary.containsKey(id))
            return true;
        else
            return false;
    }
}