import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.util.StringUtils;

public class SumBasicScoreFunction implements ScoreFunction {	
	HashMap<String, Double> termScore;

	public SumBasicScoreFunction () {
		
	}
	
	public double getScore(Collection<String> terms, int index) {
		double score = 0;
		for (String term: terms)
		{
			if (termScore.containsKey(term)) {
			  score += termScore.get(term);
			}
		}
		return score;
	}
	
	public void update(List<String> terms)
	{
		for (String term: terms)
		{
			if (!termScore.containsKey(term)) continue;
			double score = termScore.get(term) * termScore.get(term);
			termScore.put(term, score);
		}
	}
	
	/**
	 * @param args
	 */
	public void setForegroundCorpus(Documents docs) {
	    Counter scores = new Counter();
	    termScore = new HashMap<String, Double>();
            Set<String> stopwords = new HashSet<String>();
            FileReader fr;
			try {
				fr = new FileReader("stopwords.txt");
			
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
            	stopwords.add(line.trim()); 
            }
            br.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	    double total = 0;
		for (Document doc : docs.documents) 
		{
			for (String term:doc.getTerms()) {
                          if (stopwords.contains(term)  || !StringUtils.isAlpha(term)) continue;
				total += 1;
				scores.add(term);
			}
		}
		
		for (Map.Entry<String, SingleCounter> e :  scores.counts.entrySet()) {
			double mt = e.getValue().value / total;			
			termScore.put(e.getKey(), mt);
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
	public void setBackgroundCorpus() {
		// There is no background model for sumbasic
	}
	
	public double getScore(List<String> terms, List<String> pos, int index) { return Double.MIN_VALUE; };
}
