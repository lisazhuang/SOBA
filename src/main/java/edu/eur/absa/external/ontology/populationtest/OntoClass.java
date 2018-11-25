package edu.eur.absa.external.ontology.populationtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public abstract class OntoClass implements Comparable<OntoClass> {

	public static boolean orderByFrequency = false;
	private static int ID = 0;
	protected int id;
	
	protected HashMap<String, OntoClass> classes = new HashMap<>();
	protected HashMap<Word, ArrayList<Word>> wordMapping = new HashMap<>();

	protected int frequency = 0;
	protected String lexRep = "";
	
	protected HashSet<OntoClass> hasSubclasses = new HashSet<>();
	protected HashSet<OntoClass> isSubclassOf = new HashSet<>();
	
	
	public OntoClass(Word word, HashMap<String, OntoClass> classes){
		id = ID;
		ID++;
		wordMapping.put(word, new ArrayList<Word>());
		wordMapping.get(word).add(word);
		
		frequency++;
		
		lexRep = word.getAnnotation("lemma");
		
		this.classes = classes;
		classes.put(getId(), this);
	}
	
	public OntoClass(ArrayList<Word> words, boolean lemmatizeLastWord, HashMap<String, OntoClass> classes){
		id = ID;
		ID++;
		String lastWord = "";
		for (Word word : words){
			lexRep += lastWord;
			wordMapping.put(word, new ArrayList<Word>());
			wordMapping.get(word).addAll(words);
			lastWord = word.getWord().toLowerCase() + " ";
		}
		if (lemmatizeLastWord){
			lexRep += words.get(words.size()-1).getAnnotation("lemma");
		} else {
			lexRep += lastWord;
		}
		lexRep =lexRep.trim();
		frequency++;
	
		this.classes = classes;
		classes.put(getId(), this);
	}
	
	public OntoClass(HashMap<String, OntoClass> classes, OntoClass... classesToMerge){
		id = ID;
		ID++;
		for (OntoClass oc : classesToMerge){
			if (oc != null)
				lexRep = oc.lexRep+" "+lexRep;
		}
		lexRep = lexRep.trim();
		
		frequency++;
		
		this.classes = classes;
		classes.put(getId(), this);
	}
	
	public void incrementFrequency(){
		frequency++;
	}
	public int getFrequency(){
		return frequency;
	}
	
	public String getLexicalRepresentation(){
		return lexRep;
	}
	
	public void addHasSubclass(OntoClass oc){
		hasSubclasses.add(oc);
	}
	public void addIsSubclassOf(OntoClass oc){
		isSubclassOf.add(oc);
	}
	
	public HashSet<OntoClass> getIsSubclassOf(){
		return isSubclassOf;
	}
	public HashSet<OntoClass> getSubclasses(){
		return hasSubclasses;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + ": "+lexRep;
	}
	
	//there is also a static getId() in the subclasses (Entity, Property, etc.)
	public String getId(){
		return toString();
	}
	
	
	public String toLongString(){
		String text = toString();
		text += "\tFrequency: "+frequency;
		text += "\thasSubclasses: "+hasSubclasses;
		text += "\tisSubClassOf: "+isSubclassOf;
//		text += "\thasParts: " + hasParts;
//		text += "\tisPartOf: " + isPartOf;
		return text;
	}	
	
	public Node toXMLNode(){
		Element node = new Element(this.getClass().getSimpleName().toLowerCase());
		node.addAttribute(new Attribute("id", ""+id));
		node.addAttribute(new Attribute("frequency",""+frequency));
		Element lexRep = new Element("lexicalRepresentation");
		lexRep.appendChild(getLexicalRepresentation());
		node.appendChild(lexRep);
		
		Element superclasses = new Element("isSubclassOfRelations");
		superclasses.addAttribute(new Attribute("count", ""+isSubclassOf.size()));
		node.appendChild(superclasses);
		for (OntoClass superclass : isSubclassOf){
			Element isSubclassOfElement = new Element("isSubClassOf");
			isSubclassOfElement.addAttribute(new Attribute("id",""+superclass.id));
			superclasses.appendChild(isSubclassOfElement);
		}
		
		Element subclasses = new Element("hasSubclassRelations");
		subclasses.addAttribute(new Attribute("count", ""+hasSubclasses.size()));
		node.appendChild(subclasses);
		for (OntoClass subclass : hasSubclasses){
			Element hasSubclass = new Element("hasSubclass");
			hasSubclass.addAttribute(new Attribute("id",""+subclass.id));
			subclasses.appendChild(hasSubclass);
		}
		return node;
	}
	
	@Override
	public int compareTo(OntoClass arg){
		if (orderByFrequency){
			if (this.frequency > arg.frequency){
				return -1;
			}
			if (this.frequency < arg.frequency){
				return 1;
			}
			return this.getId().compareToIgnoreCase(arg.getId());			
		} else {
			return this.id - arg.id;
		}
		
		
	}
}
