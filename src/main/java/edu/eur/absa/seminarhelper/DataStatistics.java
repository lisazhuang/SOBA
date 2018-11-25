package edu.eur.absa.seminarhelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.eur.absa.Framework;

/**
 * Produces the data statistics for the restaurant and laptop SB2 datasets.
 * 
 * @author Karoliina Ranta
 *
 */
public class DataStatistics {

	public static void main(String[] args) throws ClassNotFoundException, JSONException, IOException {
		getStatsRestaurant();
		getStatsLaptop();
	}
	
	/**
	 * Reads a JSON file and converts it to a JSONArray
	 * @param file, the file to read
	 * @return the JSONArray
	 */
	public static JSONArray readJSONArrayFile(File file)  throws IOException, ClassNotFoundException, JSONException {
		Framework.debug("DataStatistics: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		String json = "";
		while ((line = in.readLine()) != null){
			json += line + "\n";
		}
		in.close();
		
		/*Create a JSON array*/
		JSONArray datasetJSON = new JSONArray(json.trim());
		return datasetJSON;
	}
	
	/**
	 * Gets the statistics about the restaurant review data (SB2)
	 */
	public static void getStatsRestaurant() throws ClassNotFoundException, JSONException, IOException {
		File fileReviews = new File(Framework.DATA_PATH+"ThesisRestaurant.predictionsALLTest.json");
		JSONArray reviewsJSON = readJSONArrayFile(fileReviews);
		
		HashSet<String> reviews = new HashSet<String>();
		HashMap<String, Integer> categoriesNum = new HashMap<String, Integer>();
		int pos = 0;
		int neg = 0;
		int neu = 0;
		int con = 0;
		
		
		/*Loops over all the reviews and categories*/
		for (int i = 0; i<reviewsJSON.length(); i++) {
			JSONObject reviewJSON = reviewsJSON.getJSONObject(i);
			String revID = reviewJSON.getString("review-id");
			String cat = reviewJSON.getString("category");
			String goldValue = reviewJSON.getString("gold-value");
			
			switch (goldValue) {
			case "positive": pos++; break;
			case "negative": neg++; break;
			case "neutral": neu++; break;
			case "conflict": con++; break;
			}
			
			reviews.add(revID);
			
			int j;
			if (!categoriesNum.containsKey(cat)) {
				j = 0;
			}
			else {
				j = categoriesNum.get(cat);
			}
			categoriesNum.put(cat, ++j);
		}
		
		System.out.println("==Restaurant SB2 statistics==");
		System.out.println("Number of reviews: "+reviews.size());
		System.out.println("Positive: "+pos);
		System.out.println("Negative: "+neg);
		System.out.println("Neutral: "+neu);
		System.out.println("Conflict: "+con);
		System.out.println("Number of instances: "+reviewsJSON.length());
		System.out.println(categoriesNum);
		System.out.println("Number aspect categories: " + categoriesNum.size());
		for (String key : categoriesNum.keySet()) {
			System.out.println(key + " " + categoriesNum.get(key));
		}
	}
	
	/**
	 * Gets the statistics about the laptop review data (SB2)
	 */
	public static void getStatsLaptop() throws ClassNotFoundException, JSONException, IOException {
		File fileReviews = new File(Framework.DATA_PATH+"ThesisLaptop.predictionsALLTest.json");
		JSONArray reviewsJSON = readJSONArrayFile(fileReviews);
		
		HashSet<String> reviews = new HashSet<String>();
		HashMap<String, Integer> categoriesNum = new HashMap<String, Integer>();
		int pos = 0;
		int neg = 0;
		int neu = 0;
		int con = 0;
		
		
		/*Loops over all the reviews and categories*/
		for (int i = 0; i<reviewsJSON.length(); i++) {
			JSONObject reviewJSON = reviewsJSON.getJSONObject(i);
			String revID = reviewJSON.getString("review-id");
			String cat = reviewJSON.getString("category");
			String goldValue = reviewJSON.getString("gold-value");
			
			switch (goldValue) {
			case "positive": pos++; break;
			case "negative": neg++; break;
			case "neutral": neu++; break;
			case "conflict": con++; break;
			}
			
			reviews.add(revID);
			
			int j;
			if (!categoriesNum.containsKey(cat)) {
				j = 0;
			}
			else {
				j = categoriesNum.get(cat);
			}
			categoriesNum.put(cat, ++j);
		}
		
		System.out.println("==Laptop SB2 statistics==");
		System.out.println("Number of reviews: "+reviews.size());
		System.out.println("Positive: "+pos);
		System.out.println("Negative: "+neg);
		System.out.println("Neutral: "+neu);
		System.out.println("Conflict: "+con);
		System.out.println("Number of instances: "+reviewsJSON.length());
		System.out.println(categoriesNum);
		System.out.println("Number aspect categories: " + categoriesNum.size());
		for (String key : categoriesNum.keySet()) {
			System.out.println(key + " " + categoriesNum.get(key));
		}
	}
}
