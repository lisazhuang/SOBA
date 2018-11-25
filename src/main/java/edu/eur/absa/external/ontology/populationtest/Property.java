package edu.eur.absa.external.ontology.populationtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import edu.eur.absa.model.Word;

public class Property extends OntoClass {

	private HashSet<Entity> isPropertyOf = new HashSet<>();
	private HashSet<Statement> isObjectOf = new HashSet<>();
	
	public Property(Word word, HashMap<String, OntoClass> classes) {
		super(word, classes);
	}

	
	
	public Property(ArrayList<Word> words, HashMap<String, OntoClass> classes){
		super(words, true, classes);
		
		//no parts for compound properties since the property-modifiers do not have their own representation 
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
		Property superclass = (Property) classes.get(Property.getId(lastWord.getAnnotation("lemma")));
		if (superclass == null){
			System.out.println(words);
			System.out.println(lastWord);
			System.out.println(Property.getId(lastWord.getAnnotation("lemma")));
			System.out.println(classes);
		}
		superclass.hasSubclasses.add(this);
		this.isSubclassOf.add(superclass);
		
	}
	
	public static String getId(String lexRep){
		return Property.class.getSimpleName() + ": "+lexRep;
	}
	
	public void addIsPropertyOf(Entity e){
		isPropertyOf.add(e);
	}
	
	public void addIsObjectOf(Statement s){
		isObjectOf.add(s);
	}
	
	public HashSet<Entity> getIsPropertyOf(){
		return isPropertyOf;
	}
	public HashSet<Statement> getIsObjectOf(){
		return isObjectOf;
	}
	
	public String toLongString(){
		String text = super.toLongString();
		text += "\tisPropertyOf: "+isPropertyOf;
		return text;
	}
	
	public Node toXMLNode(){
		Element node = (Element) super.toXMLNode();
		
		Element isPropertyOfGroup = new Element("isPropertyOfRelations");
		isPropertyOfGroup.addAttribute(new Attribute("count", ""+isPropertyOf.size()));
		node.appendChild(isPropertyOfGroup);
		for (OntoClass entity : isPropertyOf){
			Element isPropertyOf = new Element("isPropertyOf");
			isPropertyOf.addAttribute(new Attribute("id",""+entity.id));
			isPropertyOfGroup.appendChild(isPropertyOf);
		}
		Element isObjectOfGroup = new Element("isObjectOfRelations");
		isObjectOfGroup.addAttribute(new Attribute("count", ""+isObjectOf.size()));
		node.appendChild(isObjectOfGroup);
		for (OntoClass statement : isObjectOf){
			Element isObjectOf = new Element("isObjectOf");
			isObjectOf.addAttribute(new Attribute("id",""+statement.id));
			isObjectOfGroup.appendChild(isObjectOf);
		}
		return node;
	}
}
