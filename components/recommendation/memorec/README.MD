Instruction to test

Requirements:
- JRE
- Folder with .ecores
- Terminal to run the commands


Procedure:

In the "training" you have to prepare the data for MemoRec
The class ExtractMany takes as input a folder with ecores and an output empty folder and fill this folder
with a version of the ecores that can be processed by memorec

To be noticed that the following command creates two folders: one for the class attribute recommendations
and  another for the package class recommendations

Concerning the Threshold parameter, it can be used both for the extraction of the test metamodel or to create the whole dataset.
It manages the minimum number of structural features that should be in the metamodel considered.
As an example the default value 2, means that a class is considered only if it contains at least 3 structural features, like 3 attributes.
 
command: 

    java -cp MemoRecLite-1.0.jar utils.ExtractMany ../data/modelSet/raw/ ../data/modelSet/output/ 2

    java -cp file.jar MainClass(utils.ExtractMany) Path/to/dataset ; Path/to/output/folder Threshold


Before testing also the input model must be compliant with the memorec format, so you have to prepare it

Type the following command:

    java -cp MemoRecLite-1.0.jar utils.ExtractOne ../data/modelSet/raw/8_graph_Graph.ecore Class 2

    java -cp file.jar MainClass (utils.ExtractOne) Path/to/input/model ; Specifiy whether you want recommendation for a Class or a Package; Threshold


MemoRec, given a Class provides you a list of structural features
given a package, provides you a list of classes

With this command you will get recommendation from MemoRec, should be specified:
- The file (path) of the model (previously adapted for MemoRec)
- The path to the folder of the models (previously adapted for MemoRec)
- The Class or the Package for which you want to receive recommendations
- The output file to store the results (if not defined a default file called results.json will be created)


In order to run MemoRec and obtain the recommendation do the following command:


	java -cp MemoRecLite-1.0.jar memoRecCore.Runner ../data/modelSet/raw/8_graph_Graph.txt ../data/modelSet/output/cls_attr/ graph recommendations.json

    java -cp file.jar MainClass (memoRecCore.Runner) Path/to/input/model ; Path/to/folder/with/extracted/models ; Class or Package you to receive recommendations output/file.json 

The recommendations will be saved inside the defined file 
