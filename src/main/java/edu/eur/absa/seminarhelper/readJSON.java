package edu.eur.absa.seminarhelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.eur.absa.Framework;

public class readJSON {
	public readJSON() {
	
	}
	
	/**
	 * Reads a JSON file and converts it to a JSONArray
	 * @param file, the file to read
	 * @return the JSONArray
	 */
	public static JSONArray readJSONArrayFile(String path)  throws IOException, ClassNotFoundException, JSONException {
		File file = new File(path);
		Framework.debug("ReadJSONArrayFile: Start reading " + file + "...");
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
	
	public static JSONObject readJSONObjectFile(String path) throws IOException, ClassNotFoundException, JSONException {
		File file = new File(path);
		Framework.debug("readJSONObjectFile: Start reading " + file + "...");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		String json = "";
		while ((line = in.readLine()) != null){
			json += line + "\n";
		}
		in.close();
		
		/*Create a JSON object*/
		JSONObject datasetJSON = new JSONObject(json.trim());
		return datasetJSON;
	}
	
}
