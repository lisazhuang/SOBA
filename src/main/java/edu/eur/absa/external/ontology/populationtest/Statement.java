package edu.eur.absa.external.ontology.populationtest;

import java.util.HashMap;
import java.util.HashSet;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class Statement extends OntoClass {

	private HashSet<Span> originalSentences = new HashSet<>();
	private Entity subject;
	private Relation relation;
	private OntoClass object;
	
	public Statement(HashMap<String, OntoClass> classes, Span sentence, OntoClass... statementParts){
		super(classes, statementParts);
		originalSentences.add(sentence);
		subject = (Entity) statementParts[2];
		subject.addIsSubjectOf(this);
		relation = (Relation) statementParts[1];
		relation.addIsRelationOf(this);
		object = statementParts[0];
		if (object instanceof Entity)
			((Entity)object).addIsObjectOf(this);
		if (object instanceof Property)
			((Property)object).addIsObjectOf(this);
		
	}
	
	public String toLongString(){
		String text = super.toLongString();
		text += "\tSubject: " + subject;
		text += "\tRelation: " + relation;
		text += "\tObject: " + object;
		for (Span s : originalSentences){
			text += "\n\t" + s.getAnnotation("text");
		}
		return text;
	}
	
	public static String getId(String lexRep){
		return Statement.class.getSimpleName() + ": "+lexRep;
	}
	
	public Entity getSubject(){
		return subject;
	}
	public Relation getRelation(){
		return relation;
	}
	public OntoClass getObject(){
		return object;
	}
	
	public Node toXMLNode(){
		Element node = (Element) super.toXMLNode();
		
		Element subjectNode = new Element("subject");
		subjectNode.addAttribute(new Attribute("id",""+subject.id));
		node.appendChild(subjectNode);
		
		Element relationNode = new Element("relation");
		relationNode.addAttribute(new Attribute("id",""+relation.id));
		node.appendChild(relationNode);
		
		if (object != null){
			Element objectNode = new Element("object");
			objectNode.addAttribute(new Attribute("id",""+object.id));
			node.appendChild(objectNode);
		}
		
		return node;
	}
}
