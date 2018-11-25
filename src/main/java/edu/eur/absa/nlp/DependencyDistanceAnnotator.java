package edu.eur.absa.nlp;

import edu.eur.absa.Framework;
import edu.eur.absa.external.ontology.FinanceOntology;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;

public class DependencyDistanceAnnotator extends AbstractNLPComponent {

	private String sourceAnnotationType;
	private String spanAnnotationType;
	
	public DependencyDistanceAnnotator(String sourceAnnotationType, String spanAnnotationType){
		this.thisTask = NLPTask.DEP_DISTANCE_ANNOTATING;
		this.prerequisites.add(NLPTask.DEP_PARSING);
		this.sourceAnnotationType = sourceAnnotationType;
		this.spanAnnotationType = spanAnnotationType;
	}
	
	
	@Override
	public void validatedProcess(Dataset dataset, String spanType) {
		FinanceOntology ont =new FinanceOntology(Framework.EXTERNALDATA_PATH + "finance.owl");
		for (Span span : dataset.getSpans(spanType)){
			//temp line to avoid having to process everything from the beginning
//			span.getAnnotations().put("URI", ont.getLexicalizedEntity(span.getAnnotation("company")));
			
			String sourceAnnotation = span.getAnnotation(spanAnnotationType);
			Word source = null;
			for(Word w : span){
				if (w.hasAnnotation(sourceAnnotationType) &&
						w.getAnnotation(sourceAnnotationType).equals(sourceAnnotation)){
					source = w;
					break;
				}
			}
			
			if (source==null){
				Framework.error("There is no Word with a "+sourceAnnotationType+" annotation that matches the value for the " + spanAnnotationType + " annotation on the Span");
			//	Framework.error(span.getAnnotation().toString());
				
				continue;
			}
			
			traverseDependencies(source, 0);
			
			
		}
		
	}
	
	private void traverseDependencies(Word w, int distance){ //ESCW CHANGE
		double oldDistanceValue = (double)w.getAnnotation("depDistance");//.getOrDefault(w.getAnnotation("depDistance"), Double.MAX_VALUE);
		w.putAnnotation("depDistance", Math.min(distance, oldDistanceValue ));
		if (oldDistanceValue > distance){
			for (Relation rel : w.getRelations().getAllRelationsToChildren()){
				if (rel.getChild() instanceof Word){
					traverseDependencies((Word)rel.getChild(), distance+1);
				}
			}
			for (Relation rel : w.getRelations().getAllRelationsToParents()){
				if (rel.getParent() instanceof Word){
					traverseDependencies((Word)rel.getParent(), distance+1);
				}
			}
		}
	}

}
