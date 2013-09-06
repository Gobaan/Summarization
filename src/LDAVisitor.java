
import static java.nio.file.FileVisitResult.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;

public class LDAVisitor extends SimpleFileVisitor<Path> { 
	JSONParser parser = new JSONParser();
	Extractor extractor = new Extractor();
	String[] related;
	Documents docs;
    long dirStart;
    static Properties props = new Properties();
    static StanfordCoreNLP pipeline;  
    static LDAScoreFunction func;
    static Printer p;
    final static String ROOTDIR = "/home/graveendran/Downloads/DUC";
    //final static String ROOTDIR = "/home/graveendran/Results";
        
    Counter totalCounts;
	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		// TODO Auto-generated method stub
		//func = ScoreFunctionFactory.getScoreFunction("KLSum+");
		Document.initialize(Document.ParseMode.WORD_TOKENIZE);
		String fname = "LDA";
		String docset = ".Crawler";
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			docset = ".DUC";
		}
		
		p = new Printer(Printer.Type.BOTH, fname + docset);
		p.startRow();
		p.addColumn("Topic", Printer.Type.BOTH);
		p.addColumn("Num Comments", Printer.Type.BOTH);
		p.addColumn("Num Sentences", Printer.Type.BOTH);
		p.addColumn("Summary Word Count", Printer.Type.BOTH);
		p.addColumn("Execution Time", Printer.Type.TIMING);
		p.addColumn("Summary", Printer.Type.TEXT);
		p.endRow();
		
		Path startingDir = FileSystems.getDefault().getPath(ROOTDIR);
		try {
			long start = System.currentTimeMillis();
			Files.walkFileTree(startingDir, new LDAVisitor());
			long end = System.currentTimeMillis();
			System.out.println(end - start);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException
        {
		 	docs = new Documents();
            dirStart = System.currentTimeMillis();
            String name = "" + dir;
			String[] comps = name.split("/");
			String tmp = comps[comps.length - 1].toLowerCase();
			related = tmp.split(Pattern.quote("+"));
			if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
				related = new String[0];
			}
			p.startRow();
			return CONTINUE;
        }
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException
        {
           String dire = "" + dir;
           if (!dire.toLowerCase().contains("sopa")) {
           	return CONTINUE;
           }
           if (dir.toString().equalsIgnoreCase(ROOTDIR)) return CONTINUE;

            totalCounts = new Counter();
            Set<String> vocab = docs.getVocab();
            String[] vocabArray = new String[0];
            Map<String, Integer> vocabIndices = new HashMap<String, Integer>();
            double prior = 1.0 / vocab.size();
            
            String name = StringUtils.join(related, "_");
            
			
            if (name.equals("")) {
            	name = "" + dir;
    			String[] comps = name.split("/");
    			name = comps[comps.length - 1].toLowerCase();
            }
            
            try {
            	// Remove Stopwords
            	FileReader fr = new FileReader("stopwords.txt");
    			BufferedReader br = new BufferedReader(fr);
    			String line;
    			while ((line = br.readLine()) != null) {
    				vocab.remove(line.trim()); 
    			}
    			br.close();
    			
    			
    			// Write Vocab
            	File file = new File(name + ".vocab");
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				vocabArray = vocab.toArray(vocabArray);
				
				for (int i = 0; i < vocabArray.length; i++) {
					bw.write(vocabArray[i] + "\n");
					vocabIndices.put(vocabArray[i], i);
				}
				bw.close();
				
                // Write .dat
				file = new File(name + ".dat");
				fw = new FileWriter(file.getAbsoluteFile());
				bw = new BufferedWriter(fw);
				Iterator<Document> docIterator = docs.documents.iterator();
				int idx = 0;
				while ( docIterator.hasNext()) {
					Document doc = docIterator.next();
					doc.reduceTermCounts(vocab);
					Map<String, Integer> termCounts = doc.getTermCounts();
					if (termCounts.size() < 2) {
						docIterator.remove();
						continue;
					}
					bw.write(termCounts.size() + "");
					for (Entry<String, Integer> termCount: termCounts.entrySet()) {	
						int termNum = vocabIndices.get(termCount.getKey());						
						bw.write(" " + termNum + ":" + termCount.getValue());
					}
					bw.write("\n");
					doc.index = idx++;
				}
				bw.close();
				
				String[] cmd = { "/home/graveendran/workspace/StanfordNLPTest/willlda", "train",  "5", "/home/graveendran/workspace/StanfordNLPTest/" + name + ".dat",  "1000" };
		        Process p;				
				//p = new ProcessBuilder(cmd).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start();
		        p = new ProcessBuilder(cmd).start();
				p.waitFor();
				System.out.println("Done Execution");
				

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            
            
            func = new LDAScoreFunction(prior, 0.99);
            func.setBackgroundCorpus();    
            File phi = new File("output_phi");
            File theta = new File("output_theta");
            try {
            	func.setForegroundCorpus(theta, phi, vocabArray, 5);
            }	catch (java.lang.NumberFormatException e) {
            	final long total = System.currentTimeMillis() - dirStart;            	
                docs = new Documents();
     			return CONTINUE;
            	
            }
             

            Sentence.resetSeen();
		 	
            func.setForegroundCorpus(docs);
            Sentence.resetSeen();
            if (related.length == 0) {
            	p.addColumn(name, Printer.Type.BOTH);
            } else {
            	p.addColumn(StringUtils.join(related, " "), Printer.Type.BOTH);
            }
            p.addColumn(docs.documents.size(), Printer.Type.BOTH);
            p.addColumn(docs.numSentences(), Printer.Type.BOTH);                       
            
            long totalWords = 0;
            List<String> summary = new ArrayList<String>();
            while (totalWords < 100) {      
            	List<Sentence> sentences = docs.getBestSnippet(func, 1, 1, 0, 99999999);
            	//List<Sentence> sentences = docs.getBestSnippet(func, 3, 3, 40, 90);

            	if (sentences == null) break;            
            	for (Sentence sentence:sentences) {	            	
	            	summary.add(sentence.sentence.replaceAll("\t", " "));
	            	totalWords += sentence.terms.size();
	            	sentence.setSeen();
	            	func.update(null);
	            }
            }
            long executionTime = System.currentTimeMillis() - dirStart;
            
            p.addColumn(totalWords, Printer.Type.BOTH);            
            p.addColumn(executionTime, Printer.Type.TIMING);
            p.addColumn(StringUtils.join(summary, " "), Printer.Type.TEXT);		           
            p.endRow();
            
            
            docs = new Documents();
			return CONTINUE;
        }
	
	// Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file,
                                   BasicFileAttributes attr) {
    	if (attr.isRegularFile()) {
    		String name =  "" + file;    	
    		if (name.endsWith(".txt")) {
	    		for (JSONArray comments : extractor.extractFromCrawler(file, related)) {
	    			this.docs.addJSONComments(comments);
	    		}
    		} else if (Character.isDigit(name.charAt(name.length() - 1))) {
    			this.docs.addComments(extractor.extractFromDUC(file));
    		}
    	}
    	
        return CONTINUE;
    }
}
