# SOBA
Semi-automated Ontology Builder for Aspect-based sentiment analysis (SOBA)

This project contains the code for SOBA and the adjusted Two-Stage Hybrid Model (TSHM).
The Two-Stage Hybrid Model is created by Schouten and Frasincar (2018) and explained in the paper Ontology-Driven Sentiment Analysis of Product and Service Aspects, by Kim Schouten and Flavius Frasincar, published in the 15th Extended Semantic Web Conference (ESWC 2018), Lecture Notes in Computer Science, Volume 10843, pages 608-623, Springer, 2018. 
The TSHM in this project is adapted so that it takes into account dictionary semantics.

Because of the large size of the data files, not all resources could be added to this GitHub project. Therefore, please go to 
https://drive.google.com/open?id=1moQG_Df0rczwkF1yNVTbkv0AKYY70pWt
and proceed as follows:
1) Download the files in data.zip to SOBA/src/main/resources/data.
2) Similarly, add the files in externalData.zip to SOBA/src/main/resources/externaldata. In this zip file, you find data from Yelp and Amazon, of which the reviews are used as training data for SOBA.

After this, to use the ontology builder SOBA, run the class MainOntoBuild.

To evaluate the SOBA ontologies in the TSHM, run the ESWC2018 class.




