import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.util.StringUtils;


class Phrases {
	List<List<TermPos>> phrases;
	
	public int size() {
		return phrases.size();
	}
	
	public List<TermPos> get(int idx) {
		return phrases.get(idx);
	}
	
	public Phrases() {
		phrases = new ArrayList<>();
	}
	

	public void addPhrase(List<TermPos> phrase) {
		if (phrase != null && !phrase.isEmpty()) {
			phrases.add(phrase);
		}
	}
	
	public List<String> getNouns (int idx) 
	{
		List<String> nouns = new ArrayList<String>();
		List<TermPos> phrase = phrases.get(idx);
		int i = 0;
		for (; i < phrase.size() && !phrase.get(i).pos.startsWith("NN"); i++);
		for (; i < phrase.size(); i++) {
			nouns.add(phrase.get(i).term);
		}
		return nouns;
	}
	
	public List<String> getAdj (int idx) 
	{
		List<String> adjs = new ArrayList<String>();
		List<TermPos> phrase = phrases.get(idx);
		int i = 0;
		for (; i < phrase.size() && !phrase.get(i).pos.startsWith("NN"); i++);
		if (i == phrases.size()) {
			return adjs;
		}
		i -= 1;
		for (; i > 0; i--) {
			adjs.add(phrase.get(i).term);
		}
		return adjs;
	}
	
	
	public boolean hasAdjectives(int idx)
	{
		return getNouns(idx).size() > 0 && getAdj(idx).size() > 0;
	}
	
	public boolean isLinkingVerbBased(int idx) {
		if (idx + 1 > size()) return false;
		List<TermPos> phrase = phrases.get(idx + 1);
		if (!phrase.get(0).pos.startsWith("VB")) return false;
		if (phrase.size() > 1) {
			return phrase.get(1).pos.startsWith("RB");
		}
		
		if (idx + 2 > size()) return false;
		phrase = phrases.get(idx + 2);
		return phrase.get(0).pos.startsWith("JJ");		
	}
	
	public boolean isDefinite(int idx) {
		if (idx == 0 || size() < idx + 2) return false;
		if (phrases.get(idx - 1).get(0).term != "the") return false;
		return phrases.get(idx - 1).get(0).pos.startsWith("VB");		
	}
	
	public boolean isPrepositioned(int idx) {		
		return idx > 2 &&
				phrases.get(idx - 1).get(0).term == "of" &&
				phrases.get(idx - 2).get(0).type == "BNP";
	}
	
	public boolean isFeature(int idx) 
	{
		return isDefinite(idx) && isPrepositioned(idx) && isLinkingVerbBased(idx);	
	}
	
	public List<String> getImportantNouns () 
	{
		List<String> important = new ArrayList<>();
		for (int idx = 0; idx < size(); idx++)
		{
			List<TermPos> phrase = phrases.get(idx);
			if (phrase.get(0).type != "BNP") continue;
			if (!isFeature(idx)) continue;
			List<String> nouns = getNouns(idx);
			if (nouns.size() > 0) {
				important.add(StringUtils.join(nouns));
			}
		}
		return important;
	}
	
	
}