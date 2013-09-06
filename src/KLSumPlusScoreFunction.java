import static java.nio.file.FileVisitResult.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;


public class KLSumPlusScoreFunction implements ScoreFunction {
	int backgroundComments;
	static HashMap<String, Double> backgroundModel;
	HashMap<String, Double> termScore;

	public KLSumPlusScoreFunction () {
		
	}
	
	
	public double getScore(Collection<String> terms, int index) {
		double score = 0;
		for (String term: terms)
		{
			score += termScore.get(term);
		}
		return score;
	}
	
	public void update(List<String> terms)
	{
		for (String term: terms)
		{
			double score = 0; // termScore.get(term) / 2;
			termScore.put(term, score);
		}
	}
	
	/**
	 * @param args
	 */
	public void setForegroundCorpus(Documents docs) {
	    Counter scores = new Counter();
	    termScore = new HashMap<String, Double>();
	    double total = 0;
		for (Document doc : docs.documents) 
		{
			total += 1;
			for (String term:doc.getUniques()) {
				scores.add(term);
			}
		}
		for (Map.Entry<String, SingleCounter> e :  scores.counts.entrySet()) {
			double mt = e.getValue().value / total;			
			double bt = backgroundModel.get(e.getKey());
			termScore.put(e.getKey(), Math.max(0, mt * Math.log10(mt / bt)));
		}
	}

        public void printTopTerms(int n) {
          List<String> seen = new ArrayList<String>();
          for (int i=0; i<n; i++) {
            double highest = 0;
            String base = "";
            for (Map.Entry<String, Double> e :  termScore.entrySet()) {
               if (!seen.contains(e.getKey()) && e.getValue() > highest) {
                 highest = e.getValue();
                 base = e.getKey();
               }
            }
            seen.add(base);
          }
          for (String term: seen) {
            System.out.println(term);
          }
        }

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public void setBackgroundCorpus(String rootdir, int suffix) {
		CorpusLoaderVisitor visitor = new CorpusLoaderVisitor();
		// TODO Auto-generated method stub
		HashMap<String, Object> model = new HashMap<String, Object> ();
		backgroundModel = new HashMap<String, Double> ();
		File background = new File("background.corpus" + suffix);
		
		
		if(background.exists()) {
			try {
				FileInputStream in = new FileInputStream(background);
				BufferedInputStream inStream = new BufferedInputStream(in);
				ObjectInputStream objectInStream = new ObjectInputStream(inStream);
				model = (HashMap<String, Object>) objectInStream.readObject();
				objectInStream.close();
				inStream.close();
				in.close();
			} catch (IOException | ClassNotFoundException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//return model;
		} else {

			Path startingDir = FileSystems.getDefault().getPath(rootdir);
			try {
				long start = System.currentTimeMillis();
				Files.walkFileTree(startingDir, visitor);
				long end = System.currentTimeMillis();
				System.out.println(end - start);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			model.put("scores", visitor.counter.toHashMap());
			model.put("Total", new Integer(visitor.totalComments));
			try {
				FileOutputStream out = new FileOutputStream(background);
				BufferedOutputStream outStream = new BufferedOutputStream(out);
				ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
				objectOutStream.writeObject(model);
				objectOutStream.close();
				outStream.close();
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		HashMap<String, Integer> scores = (HashMap<String, Integer>)model.get("scores");
		this.backgroundComments = (Integer)model.get("Total");
		for (Map.Entry<String, Integer> e :  scores.entrySet()) {
			backgroundModel.put(e.getKey(), ((double) e.getValue()) / this.backgroundComments );
		}
	}
	
	public double getScore(List<String> terms, List<String> pos, int index) { return Double.MIN_VALUE; };
}


class CorpusLoaderVisitor extends SimpleFileVisitor<Path> { 
	JSONParser parser = new JSONParser();
	Extractor extractor = new Extractor();
	Documents docs;
    long dirStart;

    String[] related;
    public Counter counter = new Counter();
    public int totalComments;
    
    public CorpusLoaderVisitor () {
    	Document.initialize(Document.ParseMode.WORD_TOKENIZE);
    }
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException
        {
			String name = "" + dir;
			String[] comps = name.split("/");
			String tmp = comps[comps.length - 1].toLowerCase();
			related = tmp.split(Pattern.quote("+"));
		 	docs = new Documents();
            dirStart = System.currentTimeMillis();
            if (KLSumVisitor.ROOTDIR.equals("/home/graveendran/Downloads/DUC2006_Summarization_Documents/duc2006_docs")) {
				related = new String[0];
			}
			return CONTINUE;
        }
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException
        {
		
			for (Document doc : docs.documents) 
			{
				totalComments += 1;
				for (String term:doc.getUniques()) {
					counter.add(term);
				}
			}
			return CONTINUE;
        }
	
	// Print information about
    // each type of file.
    @Override
    public synchronized FileVisitResult visitFile(Path file,
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

