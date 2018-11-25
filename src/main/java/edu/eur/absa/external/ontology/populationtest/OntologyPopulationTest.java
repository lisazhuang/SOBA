package edu.eur.absa.external.ontology.populationtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import edu.eur.absa.Framework;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relations;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class OntologyPopulationTest {

	//for each Ontoclass oc, maps oc.id to oc (or oc.this)
	private HashMap<String, OntoClass> classes = new HashMap<>();
	private HashMap<Word, ArrayList<OntoClass>> wordToClasses = new HashMap<>();
	
	
	public void run(Dataset allData, String spanTypeOfSentence){
		
		for (Span textualUnit : allData.getSpans(spanTypeOfSentence)){
	
			nlp(textualUnit, wordToClasses);
			
		}
//		printAllClasses();
		saveToXML();
	}
	
	public void nlp(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		

		//single noun entities
		getSingleNounEntities(sentence, wordToClasses);

		getCompoundNounEntities(sentence, wordToClasses);
		
		getSingleAdjectiveProperties(sentence, wordToClasses);
		
		getAdverbAdjectiveProperties(sentence, wordToClasses);
		
		//get (property<-amod<-entity)=>entity pattern
		getEntitiesWithProperties(sentence, wordToClasses);
		
		//get relations
		getRelations(sentence, wordToClasses);
			
		//get compound relations
		getCompoundRelations(sentence, wordToClasses);
		
		//get statements of form entity<-nsubj<-relation->dobj->entity
		getTripleStatement(sentence, wordToClasses);
		
		//get statements of form entity<-nsubjpass<-relation
		getPassiveStatement(sentence, wordToClasses);
		
		//get statements of form entity<-nsubj<-property->cop->relation
		getCopulaStatement(sentence, wordToClasses);
		
			

		

	}
	
	private void printAllClasses(){
		OntoClass.orderByFrequency = true;
		TreeSet<OntoClass> orderedClasses = new TreeSet<>();
		orderedClasses.addAll(classes.values());
		for (OntoClass oc : orderedClasses){
			System.out.println(oc.toLongString());
		}
	}
	
	private void getSingleNounEntities(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
//		ArrayList<Word> nouns = sentence.getWords("(NN.*|PRP)");
		for (Word noun : sentence){
			if (!noun.getAnnotation("pos",String.class).matches("(NN.*|PRP)"))
				continue;
		
			//nouns are entities
			OntoClass entity;
			if (classes.containsKey(Entity.getId(noun.getAnnotation("lemma")))){
				entity = classes.get(Entity.getId(noun.getAnnotation("lemma")));
				entity.incrementFrequency();
			} else {
				entity = new Entity(noun, classes);
			}
			wordToClasses.put(noun, new ArrayList<OntoClass>());
			wordToClasses.get(noun).add(entity);
			
		}	
	}
	
	private void getCompoundNounEntities(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
//		ArrayList<Word> nouns = sentence.getWords("NN.*");
		for (Word noun : sentence){
			if (!noun.getAnnotation("pos",String.class).matches("NN.*"))
				continue;
			//compound nouns 
			// are entities
			// are subclasses of the entity created from the last word in the compound expression
			ArrayList<Word> compoundWords = new ArrayList<Word>();
			compoundWords.add(noun);
			
			
			String lexRep = noun.getAnnotation("lemma");
			int index = noun.getOrder();
			
			TreeSet<edu.eur.absa.model.Relation> compoundRelations = Relations.filterRelationsOnAnnotation(noun.getRelations().getRelationsToChildren("deps"),"relationShortName","compound");
			for (edu.eur.absa.model.Relation rel : compoundRelations.descendingSet()){
				
				Word dep = (Word)rel.getChild();
				
				int wordIndex = dep.getOrder();
				if (wordIndex + 1 == index){
					compoundWords.add(0,dep);
					lexRep = dep.getWord().toLowerCase() + " " + lexRep;
				}
			}
			lexRep = lexRep.trim();
			if (compoundWords.size() > 1){
				
//				System.out.println(sentence.getOriginalText() + "\n" + lexRep);
				OntoClass entity;
				if (classes.containsKey(Entity.getId(lexRep))){
					entity = classes.get(Entity.getId(lexRep));
					entity.incrementFrequency();
				} else {
					entity = new Entity(compoundWords, classes);
				}
	//			wordToClasses.put(noun, new ArrayList<OntoClass>());
				wordToClasses.get(noun).add(0,entity);
			}
		}
	}
	// maybe add adverbs too as properties or something related? we need to include NOT somewhere so nouns modified by ->neg->not can work
	private void getSingleAdjectiveProperties(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word adj : sentence){
			if (!adj.getAnnotation("pos",String.class).matches("JJ.*"))
				continue;			
			//adjectives describe properties (of entities)
			OntoClass property;
			if (classes.containsKey(Property.getId(adj.getAnnotation("lemma")))){
				property =classes.get(Property.getId(adj.getAnnotation("lemma"))); 
				property.incrementFrequency();
			} else {
				property = new Property(adj, classes);
			}
			wordToClasses.put(adj, new ArrayList<OntoClass>());
			wordToClasses.get(adj).add(property);
		}
	}
	
	private void getAdverbAdjectiveProperties(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word adj : sentence){
			if (!adj.getAnnotation("pos",String.class).matches("JJ.*"))
				continue;
			
			ArrayList<Word> compoundWords = new ArrayList<Word>();
			compoundWords.add(adj);
			String lexRep = adj.getAnnotation("lemma");
			int index = adj.getOrder();
//			TreeSet<edu.eur.absa.model.Relation> rels = adj.getRelations().getRelationsToChildren("advmod","neg");
			TreeSet<edu.eur.absa.model.Relation> rels = Relations.filterRelationsOnAnnotation(adj.getRelations().getRelationsToChildren("deps"),"relationShortName","advmod","neg");
			for (edu.eur.absa.model.Relation rel : rels.descendingSet()){
//			for (Word dep : adj.getDeps("advmod","neg").descendingSet()){
				Word dep = (Word)rel.getChild();
				int wordIndex = dep.getOrder();
				if (wordIndex + 1 == index){
					compoundWords.add(0,dep);
					lexRep = dep.getWord().toLowerCase() + " " + lexRep;
				}
			}
			lexRep = lexRep.trim();
			
			if (compoundWords.size() > 1){
				OntoClass property;
				if (classes.containsKey(Property.getId(lexRep))){
					property = classes.get(Property.getId(lexRep));
					property.incrementFrequency();
					
				} else {
					property = new Property(compoundWords, classes);
				}
	//			wordToClasses.put(noun, new ArrayList<OntoClass>());
				wordToClasses.get(adj).add(0,property);
			}
		}
	}
	
	private void getEntitiesWithProperties(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word noun : sentence){
			if (!noun.getAnnotation("pos",String.class).matches("NN.*"))
				continue;
			
//			Iterator<Word> depIt = noun.getDescendingDepIterator("amod");
			
			Entity entity = (Entity) wordToClasses.get(noun).get(0);
			ArrayList<Entity> subEntities = new ArrayList<Entity>();
			ArrayList<OntoClass> classesToMerge = new ArrayList<OntoClass>();
			String totalLexRep = entity.getLexicalRepresentation(); 
			classesToMerge.add(entity);
			
//			TreeSet<edu.eur.absa.model.Relation> rels = noun.getRelations().getRelationsToChildren("amod");
			TreeSet<edu.eur.absa.model.Relation> rels = Relations.filterRelationsOnAnnotation(noun.getRelations().getRelationsToChildren("deps"),"relationShortName","amod");
			for (edu.eur.absa.model.Relation rel : rels.descendingSet()){
//			while (depIt.hasNext()){
				Word dep = (Word)rel.getChild();
				
				if (!wordToClasses.containsKey(dep)){
//					System.out.println(sentence.serialize());
//					System.out.println(wordToClasses);
//					System.out.println("\nNo OntoClass for this word: " + dep);
//					System.out.println(sentence.getOriginalText());
//					printAllClasses();
				} else {
					if (wordToClasses.get(dep).get(0) instanceof Property){
						Property property = (Property) wordToClasses.get(dep).get(0);
						classesToMerge.add(property);
						String id = Entity.getId(property.getLexicalRepresentation() + " " + entity.getLexicalRepresentation());
						Entity sub;
						if (classes.containsKey(id)){
							sub = (Entity) classes.get(id); 
							sub.incrementFrequency();
							sub.addIsSubclassOf(entity);
							entity.addHasSubclass(sub);
						} else {
							sub = new Entity(classes, entity, property);
						}
						wordToClasses.get(noun).add(0,sub);
						subEntities.add(sub);
						totalLexRep = property.getLexicalRepresentation() + " " + totalLexRep; 
					}
				}
			}
			if (subEntities.size() > 1){
				
				if (classes.containsKey(Entity.getId(totalLexRep.trim()))){
					classes.get(Entity.getId(totalLexRep.trim())).incrementFrequency();
				} else {
					Entity total = new Entity(classes, classesToMerge.toArray(new OntoClass[]{}));
					wordToClasses.get(noun).add(0,total);
					for (Entity sub : subEntities){
						sub.addHasSubclass(total);
						total.addIsSubclassOf(sub);
					}
				}
			}
		}
	}
	/**
	 * Get single word relations (verbs)
	 * @param sentence
	 * @param wordToClasses
	 */
	private void getRelations(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word verb : sentence){
			if (!verb.getAnnotation("pos",String.class).matches("VB.*"))
				continue;					
			
			//relations describe relations between entities and entities/properties
			OntoClass relation;
			if (classes.containsKey(Relation.getId(verb.getAnnotation("lemma")))){
				relation =classes.get(Relation.getId(verb.getAnnotation("lemma"))); 
				relation.incrementFrequency();
			} else {
				relation = new Relation(verb, classes);
			}
			wordToClasses.put(verb, new ArrayList<OntoClass>());
			wordToClasses.get(verb).add(relation);
		}
	}
	
	private void getCompoundRelations(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word verb : sentence){
			if (!verb.getAnnotation("pos",String.class).matches("VB.*"))
				continue;	
			
			//compound relations 
			// are relations
			// are subclasses of the relation created from the last word in the compound expression
			ArrayList<Word> compoundWords = new ArrayList<Word>();
			compoundWords.add(verb);
			
			String lexRep = verb.getWord();
//			TreeSet<edu.eur.absa.model.Relation> rels = verb.getRelations().getRelationsToChildren("aux","neg","advmod", "auxpass");
			TreeSet<edu.eur.absa.model.Relation> rels = Relations.filterRelationsOnAnnotation(verb.getRelations().getRelationsToChildren("deps"),"relationShortName","aux","neg","advmod", "auxpass");
			for (edu.eur.absa.model.Relation rel : rels.descendingSet()){
//			for (Word dep : verb.getDeps("aux","neg","advmod", "auxpass").descendingSet()){
				
				Word dep = (Word)rel.getChild();
				compoundWords.add(0,dep);
				lexRep = dep.getWord().toLowerCase() + " " + lexRep;
			}
			if (compoundWords.size() > 1){
				
				OntoClass relation;
				lexRep = lexRep.trim();
				if (classes.containsKey(Relation.getId(lexRep))){
					relation = classes.get(Relation.getId(lexRep));
					relation.incrementFrequency();
				} else {
					relation = new Relation(compoundWords, classes);
				}
				wordToClasses.get(verb).add(0,relation);
			} else {
				//maybe this verb is a copula: then its aux verbs are attached to the main property word instead
				
				TreeSet<edu.eur.absa.model.Relation> cop = Relations.filterRelationsOnAnnotation(verb.getRelations().getRelationsToParents("deps"),"relationShortName","cop");
				if (cop.size()==1){
					Word gov = (Word)cop.iterator().next().getParent(); 
					//edu.eur.absa.model.Relation
//					TreeSet<edu.eur.absa.model.Relation> auxs = gov.getRelations().getRelationsToChildren("aux","neg");
					TreeSet<edu.eur.absa.model.Relation> auxs = Relations.filterRelationsOnAnnotation(verb.getRelations().getRelationsToChildren("deps"),"relationShortName","aux","neg");
					for (edu.eur.absa.model.Relation rel : auxs.descendingSet()){
						Word dep = (Word)rel.getChild();
						compoundWords.add(0,dep);
						lexRep = dep.getWord().toLowerCase() + " " + lexRep;
					}
					lexRep = lexRep.trim();
					if (compoundWords.size() > 1){
						
						OntoClass relation;
						if (classes.containsKey(Relation.getId(lexRep))){
							relation = classes.get(Relation.getId(lexRep));
							relation.incrementFrequency();
						} else {
							relation = new Relation(compoundWords, classes);
						}
						wordToClasses.get(verb).add(0,relation);
					}
				}
			}
		}
	}
	
	private void getTripleStatement(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word verb : sentence){
			if (!verb.getAnnotation("pos",String.class).matches("VB.*"))
				continue;	
			
			TreeSet<edu.eur.absa.model.Relation> subj = Relations.filterRelationsOnAnnotation(verb.getRelations().getRelationsToChildren("deps"),"relationShortName","nsubj");
			TreeSet<edu.eur.absa.model.Relation> obj = Relations.filterRelationsOnAnnotation(verb.getRelations().getRelationsToChildren("deps"),"relationShortName","dobj");
			
			if (subj.size()==1 && obj.size()==1){
//			if (verb.getDepCount("nsubj") == 1 && verb.getDepCount("dobj")==1){
				//we have a proper candidate
//				System.out.println(sentence.getOriginalText());
//				System.out.println(verb);
				Word subjectWord = (Word)subj.iterator().next().getChild(); 
//				Word subjectWord = verb.getDepIterator("nsubj").next();
//				System.out.println(subjectWord);
				Word objectWord = (Word)obj.iterator().next().getChild();
//				Word objectWord = verb.getDepIterator("dobj").next();
//				System.out.println(objectWord);
				
				if (wordToClasses.containsKey(verb) && wordToClasses.containsKey(subjectWord) && 
						wordToClasses.containsKey(objectWord) && wordToClasses.get(subjectWord).get(0) instanceof Entity){
					Relation relation = (Relation) wordToClasses.get(verb).get(0);
					Entity subject = (Entity) wordToClasses.get(subjectWord).get(0);
					OntoClass object = wordToClasses.get(objectWord).get(0);
					String key = Statement.getId(subject.getLexicalRepresentation() + " " + relation.getLexicalRepresentation() + " " + object.getLexicalRepresentation());
					OntoClass statement;
					if (classes.containsKey(key)){
						statement = classes.get(key);
						statement.incrementFrequency();
					} else {
						statement =  new Statement(classes, sentence, object, relation, subject);	
					}
//					wordToClasses.get(verb).add(0,statement);
				}
			}
		}
	}
	
	private void getPassiveStatement(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word verb : sentence){
			if (!verb.getAnnotation("pos",String.class).matches("VB.*"))
				continue;	
			
			TreeSet<edu.eur.absa.model.Relation> subjpass = Relations.filterRelationsOnAnnotation(verb.getRelations().getRelationsToChildren("deps"),"relationShortName","nsubjpass");
			if (subjpass.size()==1){
//			if (verb.getDepCount("nsubjpass") == 1){
				Word subjectWord = (Word)subjpass.iterator().next().getChild(); 
//				Word subjectWord = verb.getDepIterator("nsubjpass").next();
				if (wordToClasses.containsKey(verb) && wordToClasses.containsKey(subjectWord)){
					Relation relation = (Relation) wordToClasses.get(verb).get(0);
					Entity subject;
					try {
						subject = (Entity) wordToClasses.get(subjectWord).get(0);
					} catch (ClassCastException e){
						//? if the first is already a statement?
						try {
							subject = (Entity) wordToClasses.get(subjectWord).get(1);
						} catch (IndexOutOfBoundsException e2){
							continue;
						}
					}
					String key = Statement.getId(subject.getLexicalRepresentation() + " " + relation.getLexicalRepresentation());
					
					OntoClass statement;
					if (classes.containsKey(key)){
						statement = classes.get(key);
						statement.incrementFrequency();
					} else {
						statement =  new Statement(classes, sentence, null, relation, subject);	
					}
					wordToClasses.get(subjectWord).add(0,statement);
				}
			}
		}
	}
	
	private void getCopulaStatement(Span sentence, HashMap<Word, ArrayList<OntoClass>> wordToClasses){
		for (Word objectWord : sentence){
			if (!objectWord.getAnnotation("pos",String.class).matches("JJ.*"))
				continue;	
			
			TreeSet<edu.eur.absa.model.Relation> subj = Relations.filterRelationsOnAnnotation(objectWord.getRelations().getRelationsToChildren("deps"),"relationShortName","nsubj");
			TreeSet<edu.eur.absa.model.Relation> cop = Relations.filterRelationsOnAnnotation(objectWord.getRelations().getRelationsToChildren("deps"),"relationShortName","cop");
			
			if (subj.size()==1 && cop.size()==1){
//			if (objectWord.getDepCount("nsubj") == 1 && objectWord.getDepCount("cop")==1){
				//we have a proper candidate
//				System.out.println(sentence.getOriginalText());
//				System.out.println(verb);
				Word subjectWord = (Word)subj.iterator().next().getChild(); 
//				Word subjectWord = objectWord.getDepIterator("nsubj").next();
//				System.out.println(subjectWord);
				Word relationWord = (Word)cop.iterator().next().getChild(); 
//				Word relationWord = objectWord.getDepIterator("cop").next();
				
//				System.out.println(objectWord);
				
				if (wordToClasses.containsKey(objectWord) && wordToClasses.containsKey(subjectWord) && 
						wordToClasses.containsKey(relationWord) && wordToClasses.get(subjectWord).get(0) instanceof Entity && 
						wordToClasses.get(relationWord).get(0) instanceof Relation){
					Relation relation = (Relation) wordToClasses.get(relationWord).get(0);
					Entity subject = (Entity) wordToClasses.get(subjectWord).get(0);
					OntoClass object = wordToClasses.get(objectWord).get(0);
					String key = Statement.getId(subject.getLexicalRepresentation() + " " + relation.getLexicalRepresentation() + " " + object.getLexicalRepresentation());
					OntoClass statement;
					if (classes.containsKey(key)){
						statement = classes.get(key);
						statement.incrementFrequency();
					} else {
						statement =  new Statement(classes, sentence, object, relation, subject);	
					}
//					System.out.println(statement.toLongString());
//					wordToClasses.get(relationWord).add(0,statement);
				}
			}
		}
	}
	
	
	
	public void preprocess_old(Dataset allData) {
//		Iterator<Review> reviewIt = allData.getReviewIterator();
//		while (reviewIt.hasNext()){
//			Review review = reviewIt.next();
//			Iterator<Sentence> sentenceIt = review.getSentenceIterator();
//			while (sentenceIt.hasNext()){
//				Sentence sentence = sentenceIt.next();
////				System.out.println(sentence.getOriginalText());
//				ArrayList<Word> nouns = sentence.getWords("NN.*");
//				for (Word noun : nouns){					
//					//nouns are entities
//					if (entities.containsKey(noun.getAnnotation("lemma"))){
//						entities.get(noun.getAnnotation("lemma")).incrementFrequency();
//					} else {
//						
//						Entity n = new Entity(noun, sentence);
//						entities.put(n.getLexicalRepresentation(), n);
//					}
//					wordToEntity.put(noun, entities.get(noun.getAnnotation("lemma")));
//				}
//				for (Word noun : nouns){
//					//compound nouns 
//					// are entities
//					// are subclasses of the entity created from the last word in the compound expression
//					Iterator<Word> depIt = noun.getDepIterator("compound");
//					int startWord = sentence.getWordOrder(noun);
//					int endWord = startWord;
//					int deps = noun.getDepCount("compound");
//					while (depIt.hasNext()){
//						Word dep = depIt.next();
//						int wordIndex = sentence.getWordOrder(dep);
//						if (endWord - wordIndex <= deps){
//							startWord = Math.min(wordIndex, startWord);
//						}
//					}
//					if (startWord < endWord){
//						//we found a compound word
//						Span s = new Span(startWord, endWord, "NN", sentence);
//						String lexRep = getLexicalRepresentation(s);
//						if (entities.containsKey(lexRep)){
//							entities.get(lexRep).incrementFrequency();
//						} else {
//							Entity n = new Entity(s, entities);
//							entities.put(n.getLexicalRepresentation(), n);
//						}
//						ArrayList<Word> words = s.getSpan();
//						for (Word w : words){
//							wordToEntity.put(w, entities.get(lexRep));
//						}
//					}
//					
//					//JJ-amod-Entity
//					if (noun.getDepCount("amod") > 0){
//						Entity entity = wordToEntity.get(noun);
//						depIt = noun.getDepIterator("amod");
//						TreeMap<Integer, Word> orderedDeps = new TreeMap<>();
//						while (depIt.hasNext()){
//							Word dep = depIt.next();
//							orderedDeps.put(sentence.getWordOrder(dep), dep);
//						}
//						ArrayList<Word> amods = new ArrayList<>();
//						amods.addAll(orderedDeps.values());
//						Entity sub = new Entity(amods, entity, sentence);
//						entities.put(sub.getLexicalRepresentation(), sub);
//						for (Integer key : orderedDeps.keySet()){
//							wordToEntity.put(orderedDeps.get(key),sub);
//						}
//					}
//				}
//			}
//		}
//
//		for (Entity e : entities.values()){
//			System.out.println(e.toLongString());
//		}
	}
	
	private String getLexicalRepresentation(Span s){
//		ArrayList<Word> words = s.getSpan();
//		String lexicalRepresentation = "";
//		for (int i = 0; i < s.getNumberOfWords()-1; i++){
//			Word w = words.get(i);
//			lexicalRepresentation += w.getCorrectedText().toLowerCase() + " ";
//		}
//		lexicalRepresentation += s.getLastWord().getAnnotation("lemma");
//		return lexicalRepresentation;
		return s.getAnnotation("text");
	}
	
	public void saveToXML(){
		OntoClass.orderByFrequency = true;
		TreeSet<OntoClass> orderedClasses = new TreeSet<OntoClass>();
		orderedClasses.addAll(classes.values());
		
		Element root = new Element("ontology");
		Element entities = new Element("entities");
		root.appendChild(entities);
		Element properties = new Element("properties");
		root.appendChild(properties);
		Element relations = new Element("relations");
		root.appendChild(relations);
		Element statements = new Element("statements");
		root.appendChild(statements);
		
		for (OntoClass ontoclass : orderedClasses){
			String key = ontoclass.getId();
			if (key.startsWith("Entity")){
				entities.appendChild(ontoclass.toXMLNode());
			}
			if (key.startsWith("Property")){
				properties.appendChild(ontoclass.toXMLNode());
			}
			if (key.startsWith("Relation")){
				relations.appendChild(ontoclass.toXMLNode());
			}
			if (key.startsWith("Statement")){
				statements.appendChild(ontoclass.toXMLNode());
			}
		}
		
		entities.addAttribute(new Attribute("count",""+entities.getChildCount()));
		properties.addAttribute(new Attribute("count",""+properties.getChildCount()));
		relations.addAttribute(new Attribute("count",""+relations.getChildCount()));
		statements.addAttribute(new Attribute("count",""+statements.getChildCount()));
		
		Document doc = new Document(root);
		//System.out.println(doc.toXML());
		try {
			Serializer serializer = new Serializer(new FileOutputStream(new File(Framework.OUTPUT_PATH + "generatedConcepts-"+System.currentTimeMillis() + ".xml")));
			serializer.setIndent(4);
//		      serializer.setMaxLength(64);
			serializer.write(doc);  
	    }
	    catch (IOException ex) {
	    	System.err.println(ex); 
	    }  
		
		
	}
	
	public ArrayList<OntoClass> getOntoClasses(Word word){
		return wordToClasses.get(word);
	}

	public boolean hasOntoClass(Word word){
		return wordToClasses.containsKey(word);
	}
	

}

