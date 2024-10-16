# Bug Hunter Preprocessing

Preprocessing code for the BugHunter dataset

## Usage

1. Download BugHunter dataset https://www.kaggle.com/datasets/vellyy/bug-hunter (methods and parents)
2. Download all repositories mentioned in the paper
3. Run the extractor script `python extract.py`
4. Build the main method extractor using `gradle build`

## Creating Entries for training

1. Run the jar with arguments `java -jar Method-Extractor-1.0-SNAPSHOT.jar -i method-p-source.csv -wd data/ -o output`.
   * -i: Name of the csv file with the entries
   * -wd: The folder where all the extracted files from the extract/py are
   * -o: The output directory

## Creating for unseen prediction

1. Run the jar with arguments `java -jar Method-Extractor-1.0-SNAPSHOT.jar -i BubbleSort.java -wd predict-data -o predict-output`
  * i: The java file to run the model on
  * -wd: The directory where the java file is
  * -o: The output directory
