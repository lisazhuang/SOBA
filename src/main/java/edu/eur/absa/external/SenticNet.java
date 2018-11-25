package edu.eur.absa.external;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.eur.absa.Framework;

/**
 * This class should be rewritten to properly use an XML loader. Also upgrade to SenticNet 4 while we're at it.
 * Using XMl reader will also quickly allow us to extract other useful properties from this repository.
 * @author Kim Schouten
 *
 */
@Deprecated
public class SenticNet {

	HashMap<String, Double> senticNet = new HashMap<String, Double>();
	
	public SenticNet(){
    	this(Framework.LIB_PATH);
    }
	public SenticNet(String libPath){
		Logger.getGlobal().info("Reading SenticNet file...");
		try
        {
            BufferedReader reader =  new BufferedReader(new FileReader(libPath + "SenticNet/senticnet3.rdf.xml"));
            String line;	
//            boolean insideConceptDesc = false;
            String conceptName = "";
            while((line = reader.readLine()) != null)
            {
            	line = line.trim();
            	if (line.contains("<rdf:Description")){
            		//start of concept
//            		insideConceptDesc = true;
            		String concept = line.substring(line.indexOf("/concept/")+9,line.lastIndexOf("\">"));
            		conceptName = concept.replaceAll("_", "");
            	}
            	
            	if (line.contains("<polarity")){
            		String pol = line.substring(line.indexOf("\">")+2,line.indexOf("</polarity>"));
            		senticNet.put(conceptName, Double.parseDouble(pol));
//            		insideConceptDesc = false;
            		
            	}
            }
            reader.close();
            
        }     
        catch(IOException | NumberFormatException e)
        {
            Logger.getLogger(SenticNet.class.getName()).log(Level.SEVERE, null, e);
        }
		Logger.getGlobal().info("Done!\nConcepts loaded: " + senticNet.size());
		
//		for (String text : senticNet.keySet()){
//			System.out.println(senticNet.get(text) + "\t" + text);
//		}
		
	}
	
	public boolean hasEntry(String text){
		return senticNet.containsKey(text);
	}
	public double extractExactSentiment(String text){
		return senticNet.getOrDefault(text,0.0);
	}
	
	
}
