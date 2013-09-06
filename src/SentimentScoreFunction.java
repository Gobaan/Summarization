import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SentimentScoreFunction implements ScoreFunction {	
	int topicNum;
	Set<String> topic;
	// K -> String Term:Sense   => positive and negtive sentiment
	static Map<String, SentimentScore> sentimentMapping;	
	static {
		
		sentimentMapping = new HashMap<>();
		Map<String, String> senseMapping = new HashMap<>();
		senseMapping.put("r", "RB");
		senseMapping.put("a", "JJ");
		senseMapping.put("n", "NN");
		senseMapping.put("v", "VB");
		File f = new File("SentiWordNet_3.0.0_20130122.txt");
		String line;
		try {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			Map<String, SentimentScore> tempMapping = new HashMap<>();
			
			
			while ((line = br.readLine()) != null) {
				String[] comps = line.split("\t");
				String sense = comps[0];
				sense = senseMapping.get(sense);
				String syns = comps[4];
				for (String term: syns.split(" ")) {				
					term = term.split("#")[0];		
					String key =  term + ":" + sense;
					if (tempMapping.get(key) == null) {
						tempMapping.put(key, new SentimentScore(term));
					}
					SentimentScore scores = tempMapping.get(key);
					scores.count += 1;
					scores.positive += Double.parseDouble(comps[2]);
					scores.negative += Double.parseDouble(comps[3]);
				}			
			}
			br.close();
			
			
			for (String key : tempMapping.keySet()) {
				SentimentScore scores = tempMapping.get(key);
				SentimentScore newScore = new SentimentScore(scores.term);
				newScore.positive = scores.positive / scores.count;
				newScore.negative = scores.negative / scores.count;
				newScore.count = scores.count;
				if (newScore.positive != 0.0 || newScore.negative != 0.0) {
					sentimentMapping.put(key, newScore);
				}				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public SentimentScoreFunction () {
		this.topic = new HashSet<String>();
		this.topicNum = 0;
	}
	
	public double getScore(List<String> terms, List<String> poss, int sentiment) {
		if (!terms.containsAll(topic)) return Double.MIN_VALUE;
		Set<String> negationTerms = new HashSet<String>();
		negationTerms.add("not");
		negationTerms.add("n't");
		negationTerms.add("never");
		double score = 0;
		for (int i = 0; i < terms.size(); i++) {		
			String pos = poss.get(i);
			pos = pos.substring(0, Math.min(2, pos.length()));			
			String term = terms.get(i).toLowerCase();
			String key = term + ":" + pos;
			if (!sentimentMapping.containsKey(key)) continue;
			
			int start = Math.max(0, i - 3);			
			int currentSentiment = sentiment;
			List<String> qualifier = terms.subList(start, i); 
			for (String negate : negationTerms) {
				if (qualifier.contains(negate)) {
					currentSentiment = 1 - currentSentiment;
				}
			}
			if (currentSentiment == 0) {
				score += sentimentMapping.get(key).positive;
			} else {
				score += sentimentMapping.get(key).negative;
			}			
		}
		
		return score != 0 ? score : Double.MIN_VALUE;
	}
	
	public void printScores(List<String> terms, List<String> poss, int sentiment) {
		Set<String> negationTerms = new HashSet<String>();
		negationTerms.add("not");
		negationTerms.add("n't");
		negationTerms.add("never");
		for (int i = 0; i < terms.size(); i++) {		
			String pos = poss.get(i);
			pos = pos.substring(0, Math.min(2, pos.length()));			
			String term = terms.get(i).toLowerCase();
			String key = term + ":" + pos;
			if (!sentimentMapping.containsKey(key)) continue;
			
			int start = Math.max(0, i - 3);			
			int currentSentiment = sentiment;
			List<String> qualifier = terms.subList(start, i); 
			for (String negate : negationTerms) {
				if (qualifier.contains(negate)) {
					currentSentiment = 1 - currentSentiment;
				}
			}
			if (currentSentiment == 0) {
				System.out.println(key + ":" + sentimentMapping.get(key).positive);
			} else {			
				System.out.println(key + ":" + sentimentMapping.get(key).negative);
			}
		}
		
	}
	
	public double getScore(Collection<String> terms, int index) {
		return Double.MIN_VALUE;
	}
	
	public void update(List<String> terms)
	{
		this.topic = new HashSet<String> ();
		this.topicNum = this.topicNum % terms.size();
		for (String s : terms.get(this.topicNum++).split(" ")) {
			this.topic.add(s);
		}
		System.out.println(this.topic);
	}
	
	/**
	 * @param args
	 */
	public void setForegroundCorpus(Documents docs) {
	    
	}
	

	/**
	 * @param args
	 */
	public void setBackgroundCorpus() {
		// There is no background model for sumbasic
	}
	
}

class SentimentScore {
	String term;
	public int count;
	public double positive, negative;
	
	public SentimentScore(String term) {		
		this.term  = term;
		this.count = 0;
		this.positive = 0.0;
		this.negative = 0.0;
	}
}
