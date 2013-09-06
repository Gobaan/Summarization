import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;


class Documents {
	public ArrayList<Document> documents;
	public Documents() {
		documents = new ArrayList<Document>();
	}
	
	public void addJSONComments (JSONArray comments){
		for (int i = 0; i < comments.size(); i++) {
    		JSONObject comment = (JSONObject) comments.get(i);
    		String text = (String)comment.get("text");
    		documents.add(new Document(text, documents.size()));
		}
	}	
	
	public int numSentences () {
		int total = 0;
		for (Document d: documents) {
			total += d.sentences.size();
		}
		return total;
	}
	
	public void addComments (List<String> comments){
		for (int i = 0; i < comments.size(); i++) {
    		documents.add(new Document(comments.get(i), documents.size()));
		}
	}
	
	
	public Set<String> getVocab() {
		Counter vocabCount = new Counter();
		
		for (Document d:documents) {
			Map<String, Integer> termCounts = d.getTermCounts();
			for (Entry<String, Integer> e : termCounts.entrySet()) {
				if (StringUtils.isAlpha(e.getKey())) {
					vocabCount.add(e.getKey(), e.getValue());
				}
			}
		}
		
		Set<String> vocab = new HashSet<String> ();
		for (Entry<String, Integer> m : vocabCount.toHashMap().entrySet()) {
			if (m.getValue() < 4) {
				continue;
			}
			vocab.add(m.getKey());
		}
		
		return vocab;
	}
	
	public List<Sentence> getBestSnippet(ScoreFunction func) {
		return getBestSnippet(func, 1, 1);
	}
	
	public List<Sentence> getBestSnippet(ScoreFunction func, int min, int max) {
		return getBestSnippet(func, 1, 1, 0, 9999);
	}
	
	public List<Sentence> getBestSnippet(ScoreFunction func, int min, int max, int delta, int windowSize) {
		double highscore = Double.MIN_VALUE;
		List<Sentence> bestSentences = null;
		for (Document doc:this.documents) {
			Pair<List<Sentence>, Double> result = doc.getBestSnippet(func, min, max, delta, windowSize);
			if (result.second() > highscore) {
				highscore = result.second();
				bestSentences = result.first();
			}
		}
		return bestSentences;
	}
	
	
	public List<Sentence> getBestSentiment(ScoreFunction func, int sentiment, int min, int max, int delta, int windowSize) {
		double highscore = Double.MIN_VALUE;
		List<Sentence> bestSentences = null;
		for (Document doc:this.documents) {
			Pair<List<Sentence>, Double> result = doc.getBestSentiment(func, 0, min, max, delta, windowSize);
			if (result.second() > highscore) {
				highscore = result.second();
				bestSentences = result.first();
			}
		}
		return bestSentences;
	}
}