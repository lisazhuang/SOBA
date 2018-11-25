package edu.eur.absa.nlp;

import edu.eur.absa.model.Dataset;

public interface IPipeline {

	Dataset process(Dataset dataset);
	
}
