
import static java.nio.file.FileVisitResult.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

public class SentimentSumVisitor extends SimpleFileVisitor<Path> { 
	String[] related;
	Documents docs;
    long dirStart;
    Extractor extractor = new Extractor();
    boolean started = false;
    static Properties props = new Properties();
    static StanfordCoreNLP pipeline;  
    static SentimentScoreFunction func;
    static Printer p;
    //final static String ROOTDIR = "/home/graveendran/Downloads/DUC";
    final static String ROOTDIR = "/home/graveendran/Results/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//func = ScoreFunctionFactory.getScoreFunction("KLSum+");
		Document.initialize(Document.ParseMode.POS);
		String fname = "Sentiment";
		String docset = ".Crawler";
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			docset = ".DUC";
		}
		
		p = new Printer(Printer.Type.BOTH, fname + docset);
		p.startRow();
		p.addColumn("Topic", Printer.Type.BOTH);
		p.addColumn("Num Comments", Printer.Type.BOTH);
		p.addColumn("Num Sentences", Printer.Type.BOTH);
		p.addColumn("Positive Summary Word Count", Printer.Type.BOTH);
		p.addColumn("Negative Summary Word Count", Printer.Type.BOTH);
		p.addColumn("Total Word Count", Printer.Type.BOTH);
		p.addColumn("Execution Time", Printer.Type.TIMING);
		p.addColumn("Positive Summary", Printer.Type.TEXT);
		p.addColumn("Negative Summary", Printer.Type.TEXT);		
		p.addColumn("Summary", Printer.Type.BOTH);
		p.endRow();
		
		func = new SentimentScoreFunction();
		//func.setBackgroundCorpus();
		props.put("annotators", "tokenize, ssplit, pos");
		pipeline = new StanfordCoreNLP(props);
		Annotation document = new Annotation("not a good car. it won't start. it never eats pie.,!;");
		pipeline.annotate(document);
		for(CoreMap sentence: document.get(SentencesAnnotation.class)) { 
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) 
			{
				String term = token.get(TextAnnotation.class);
				String pos = token.get(PartOfSpeechAnnotation.class);
				System.out.print(term + "/" + pos + " ");
			}
			System.out.println("");
		} 
		
		
		Path startingDir = FileSystems.getDefault().getPath(ROOTDIR);
		try {
			long start = System.currentTimeMillis();
			Files.walkFileTree(startingDir, new SentimentSumVisitor());
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
            String name = "" + dir;
            if (!name.toLowerCase().contains("sopa")) {
              return CONTINUE;
            }
            if (name.toString().equalsIgnoreCase(ROOTDIR)) return CONTINUE;
                       
            List<String> terms = new ArrayList<String>();
            for (SingleCounter s : extractKeyNouns(docs, 20)) {
            	terms.add(s.term);
            	System.out.println(s.term);
            }            		
            if (terms.size() == 0) {
            	//System.out.println("No terms found");
            	return CONTINUE;
            }					
           
           String[] comps = name.split("/");
           String tmp = comps[comps.length - 1].toLowerCase();
            func.setForegroundCorpus(docs);
            Sentence.resetSeen();
            if (related.length == 0) {
            	p.addColumn(tmp, Printer.Type.BOTH);
            } else {
            	p.addColumn(StringUtils.join(related, " "), Printer.Type.BOTH);
            }
            p.addColumn(docs.documents.size(), Printer.Type.BOTH);
            p.addColumn(docs.numSentences(), Printer.Type.BOTH);  
            
            List<String> positiveSummary = new ArrayList<String>();
            List<String> negativeSummary = new ArrayList<String>();
            List<String> totalSummary = new ArrayList<String>();
			Sentence.resetSeen();
			int totalWords = 0;
			int positiveWordCount = 0;
			int negativeWordCount = 0;
			while(totalWords < 250) {				
				func.update(terms);
				List<Sentence> bestSentences =  docs.getBestSentiment(func, 0, 1, 1, 0, 99999);
				if (bestSentences == null) continue;
				for (Sentence sentence:bestSentences) {
					positiveSummary.add(sentence.sentence);
					totalSummary.add(sentence.sentence);
					sentence.setSeen();
					positiveWordCount += sentence.terms.size();
					totalWords +=  sentence.terms.size();
				}				
											
			    bestSentences =  docs.getBestSentiment(func, 1, 1, 1, 0, 9999);
				if (bestSentences == null) continue;
				for (Sentence sentence:bestSentences) {
					negativeSummary.add(sentence.sentence);
					totalSummary.add(sentence.sentence);
	            	sentence.setSeen();
	            	negativeWordCount += sentence.terms.size();
	            	totalWords +=  sentence.terms.size();
	            }
			}
			
            long executionTime = System.currentTimeMillis() - dirStart;
            p.addColumn(positiveWordCount, Printer.Type.BOTH);  
            p.addColumn(negativeWordCount, Printer.Type.BOTH);  
            p.addColumn(totalWords, Printer.Type.BOTH);  
            p.addColumn(executionTime, Printer.Type.TIMING);
            p.addColumn(StringUtils.join(positiveSummary, " "), Printer.Type.TEXT);		           
            p.addColumn(StringUtils.join(negativeSummary, " "), Printer.Type.TEXT);
            p.addColumn(StringUtils.join(totalSummary, " "), Printer.Type.TEXT);
            p.endRow();
			return CONTINUE;
        }
	
	// Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file,
                                   BasicFileAttributes attr) {    	    	
        String name =  "" + file;    	
        if (!name.toLowerCase().contains("sopa")) {
          return CONTINUE;
        }
    	if (attr.isRegularFile()) {
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
    
    public List<SingleCounter> extractKeyNouns (Documents docs, int n) {
    	Counter c = new Counter();
    	
    	for (Document d : docs.documents) {
    		for (Sentence s : d.sentences) {
    			Phrases p = s.phrases;
    			for (int idx = 0; idx < p.size(); idx++) {
    				 for (String term : p.getNouns(idx)) {
    					 c.add(term);
    				 }
    			}
    		}
    	}
    	
    	return c.topN(n);
    }
}
