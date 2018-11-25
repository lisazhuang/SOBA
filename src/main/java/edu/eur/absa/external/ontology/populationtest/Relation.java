package edu.eur.absa.external.ontology.populationtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import edu.eur.absa.model.Word;

public class Relation extends OntoClass {

	private HashSet<Statement> isRelationOf = new HashSet<>();
	
	public Relation(Word word, HashMap<String, OntoClass> classes) {
		super(word, classes);
	}

	public Relation(ArrayList<Word> words, HashMap<String, OntoClass> classes){
		super(words, false, classes);
		
		//no parts for compound relations since the modifiers do not have their own representation or are not really relevant
		// (e.g., has been ... where has and been are not so informative on their own)
//		for (int i = 0; i < words.size()-1; i++){
//			String lemma = words.get(i).getLemma();
//			if (classes.containsKey(Entity.getId(lemma))){
//				Entity part = (Entity) classes.get(Entity.getId(lemma));
//				part.isPartOf.add(this);
//				this.hasParts.add(part);
//			}
//		}
		
		//last word is the main word in a compound property (at least in English)
		// this is also the only one with already a Property assigned to it
		Word lastWord = words.get(words.size()-1);
		Relation superclass = (Relation) classes.get(Relation.getId(lastWord.getAnnotation("lemma")));
		if (superclass == null){
			System.out.println(words);
			System.out.println(lastWord);
			System.out.println(Relation.getId(lastWord.getAnnotation("lemma")));
			System.out.println(classes);
		}
		superclass.hasSubclasses.add(this);
		this.isSubclassOf.add(superclass);
		
	}
	
	public void addIsRelationOf(Statement s){
		isRelationOf.add(s);
	}
	
	public HashSet<Statement> getIsRelationOf(){
		return isRelationOf;
	}
	
	public static String getId(String lexRep){
		return Relation.class.getSimpleName() + ": "+lexRep;
	}
	
	public Node toXMLNode(){
		Element node = (Element) super.toXMLNode();
		
		Element isRelationOfGroup = new Element("isRelationOfRelations");
		isRelationOfGroup.addAttribute(new Attribute("count", ""+isRelationOf.size()));
		node.appendChild(isRelationOfGroup);
		for (OntoClass statement : isRelationOf){
			Element isRelationOf = new Element("isRelationOf");
			isRelationOf.addAttribute(new Attribute("id",""+statement.id));
			isRelationOfGroup.appendChild(isRelationOf);
		}
		
		return node;
	}
}
