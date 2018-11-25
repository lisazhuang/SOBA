package edu.eur.absa.external.ontology.populationtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import edu.eur.absa.model.Word;

public class Entity extends OntoClass {

	private HashSet<OntoClass> hasParts = new HashSet<>();
	private HashSet<OntoClass> isPartOf = new HashSet<>();
	private HashSet<Property> hasProperties = new HashSet<>();
	
	private HashSet<Statement> isSubjectOf = new HashSet<>();
	private HashSet<Statement> isObjectOf = new HashSet<>();
	
	/**
	 * Constructor for single noun entities
	 * @param word
	 * @param classes
	 */
	public Entity(Word word, HashMap<String, OntoClass> classes) {
		super(word, classes);
	}
	/**
	 * Constructor for compound noun entities
	 * @param words
	 * @param classes
	 */
	public Entity(ArrayList<Word> words, HashMap<String, OntoClass> classes){
		super(words, true, classes);
		
//		System.out.println(classes);
//		System.out.println(words);
		
		for (int i = 0; i < words.size()-1; i++){
			String lemma = words.get(i).getAnnotation("lemma");
			if (classes.containsKey(Entity.getId(lemma))){
				Entity part = (Entity) classes.get(Entity.getId(lemma));
				part.isPartOf.add(this);
				this.hasParts.add(part);
			}
		}
		//last word is the main word in a compound noun (at least in English)
		Word lastWord = words.get(words.size()-1);
		Entity superclass = (Entity) classes.get(Entity.getId(lastWord.getAnnotation("lemma")));
		superclass.hasSubclasses.add(this);
		this.isSubclassOf.add(superclass);
		
	}
	/**
	 * Constructor to create a new Entity from a previous Entity combined with one or more Property objects
	 * @param classes
	 * @param entityFollowedByProperties
	 */
	public Entity(HashMap<String, OntoClass> classes, OntoClass... entityFollowedByProperties){
		super(classes, entityFollowedByProperties);
		for (int i = 1; i < entityFollowedByProperties.length; i++){
			Property property = (Property) entityFollowedByProperties[i];
			hasProperties.add(property);
			property.addIsPropertyOf(this);
		}
		Entity superclass = (Entity) entityFollowedByProperties[0];
		superclass.hasSubclasses.add(this);
		this.isSubclassOf.add(superclass);
	}
	
	public void addIsSubjectOf(Statement s){
		isSubjectOf.add(s);
	}
	public void addIsObjectOf(Statement s){
		isObjectOf.add(s);
	}
	
	public HashSet<Statement> getIsSubjectOf(){
		return isSubjectOf;
	}
	public HashSet<Statement> getIsObjectOf(){
		return isObjectOf;
	}
	public HashSet<Property> getProperties(){
		return hasProperties;
	}
	public HashSet<OntoClass> getIsPartOf(){
		return isPartOf;
	}
	public HashSet<OntoClass> getParts(){
		return hasParts;
	}
		
	
	public String toLongString(){
		String text = super.toLongString();
		text += "\thasParts: " + hasParts;
		text += "\tisPartOf: " + isPartOf;
		text += "\thasProperties: " + hasProperties;
		return text;
	}	
	
	public static String getId(String lexRep){
		return Entity.class.getSimpleName() + ": "+lexRep;
	}
	
	public Node toXMLNode(){
		Element node = (Element) super.toXMLNode();
		
		Element hasPartsGroup = new Element("hasPartsRelations");
		hasPartsGroup.addAttribute(new Attribute("count", ""+hasParts.size()));
		node.appendChild(hasPartsGroup);
		for (OntoClass part : hasParts){
			Element hasPart = new Element("hasPart");
			hasPart.addAttribute(new Attribute("id",""+part.id));
			hasPartsGroup.appendChild(hasPart);
		}
		
		Element isPartOfGroup = new Element("isPartOfRelations");
		isPartOfGroup.addAttribute(new Attribute("count", ""+isPartOf.size()));
		node.appendChild(isPartOfGroup);
		for (OntoClass whole : isPartOf){
			Element isPartOfElement = new Element("isPartOf");
			isPartOfElement.addAttribute(new Attribute("id",""+whole.id));
			isPartOfGroup.appendChild(isPartOfElement);
		}
		
		Element hasPropertyGroup = new Element("hasPropertyRelations");
		hasPropertyGroup.addAttribute(new Attribute("count", ""+hasProperties.size()));
		node.appendChild(hasPropertyGroup);
		for (OntoClass property : hasProperties){
			Element hasProperty = new Element("hasProperty");
			hasProperty.addAttribute(new Attribute("id",""+property.id));
			hasPropertyGroup.appendChild(hasProperty);
		}
		
		Element isSubjectOfGroup = new Element("isSubjectOfRelations");
		isSubjectOfGroup.addAttribute(new Attribute("count", ""+isSubjectOf.size()));
		node.appendChild(isSubjectOfGroup);
		for (OntoClass statement : isSubjectOf){
			Element isSubjectOfElement = new Element("isSubjectOf");
			isSubjectOfElement.addAttribute(new Attribute("id",""+statement.id));
//			isSubjectOfElement.addAttribute(new Attribute("frequency",""+isSubjectOf.get(statement)));
			isSubjectOfGroup.appendChild(isSubjectOfElement);
		}
		
		Element isObjectOfGroup = new Element("isObjectOfRelations");
		isObjectOfGroup.addAttribute(new Attribute("count", ""+isObjectOf.size()));
		node.appendChild(isObjectOfGroup);
		for (OntoClass statement : isObjectOf){
			Element isObjectOfElement = new Element("isObjectOf");
			isObjectOfElement.addAttribute(new Attribute("id",""+statement.id));
//			isObjectOfElement.addAttribute(new Attribute("frequency",""+isObjectOf.get(statement)));
			isObjectOfGroup.appendChild(isObjectOfElement);
		}
		return node;
	}
}
