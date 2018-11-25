package edu.eur.absa.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.eur.absa.Framework;

public class StockMarketLexicon {

	HashMap<String, Double> lexicon = new HashMap<String, Double>();
	HashMap<String, Double> negatedLexicon = new HashMap<String, Double>();
	
	private static StockMarketLexicon singleInstance = null;
	
	public static StockMarketLexicon getInstance(){
		if (singleInstance == null)
			singleInstance = new StockMarketLexicon();
		return singleInstance;
	}
	public static StockMarketLexicon getInstance(File file){
		if (singleInstance == null)
			singleInstance = new StockMarketLexicon();
		return singleInstance;
	}
	
	private StockMarketLexicon(){
    	this(new File(Framework.EXTERNALDATA_PATH+"/StockMarketLexiconDSS/stock_lex.csv"));
    }

	private StockMarketLexicon(File file) {

		Logger.getGlobal().info("Reading file...");
		try
        {
            BufferedReader reader =  new BufferedReader(new FileReader(file));
            String line = reader.readLine();	//skip first line with headers	

            while((line = reader.readLine()) != null)
            {
            	String[] lineParts = line.split(",");
            	String entry = lineParts[0].substring(1, lineParts[0].length()-1);
            	double affScore = Double.parseDouble(lineParts[2]);
            	double negScore = Double.parseDouble(lineParts[3]);
            	lexicon.put(entry, affScore);
            	negatedLexicon.put(entry, negScore);
            }
        }     
        catch(IOException | NumberFormatException e)
        {
            Logger.getLogger(StockMarketLexicon.class.getName()).log(Level.SEVERE, null, e);
        }
		Logger.getGlobal().info("Done!\nEntries loaded: " + lexicon.size());
	}
	
	public double getScore(String word){
		return lexicon.getOrDefault(word,0.0);
	}
	public double getNegatedScore(String word){
		return negatedLexicon.getOrDefault(word,0.0);
	}
	
	
}
