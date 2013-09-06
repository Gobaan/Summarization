import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.nlp.util.StringUtils;

import java.io.File;


public class Extractor {
	JSONParser parser = new JSONParser();
	
	public List<String> extractFromDUC(Path path) {
		List<String> articles = new ArrayList<String>();
		boolean testing = path.toString().endsWith("FT921-74");
		File file = new File(path.toString());
		// Remove Stopwords
    	FileReader fr;
    	List<String> lines = new ArrayList<String>();
		try {
			fr = new FileReader(file);		
			BufferedReader br = new BufferedReader(fr);			
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);			
			}
			br.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String text = StringUtils.join(lines, " ");
		text = text.replaceAll("&Cx..;", "");
		text = text.replaceAll("&..;", "");
		text = text.replaceAll("&AMP;", "and");
		DocumentBuilder dBuilder;
		try {
			InputStream is = new ByteArrayInputStream(text.getBytes());
			dBuilder = DocumentBuilderFactory.newInstance()
			        .newDocumentBuilder();
			Node doc = dBuilder.parse(is);
			NodeList children = doc.getChildNodes().item(0).getChildNodes();
			List<String> data = new ArrayList<String> ();
			for (int i = 0; i < children.getLength(); i++) {
				Node current = children.item(i);		
				// (current.getNodeName() == "TEXT" && current.hasChildNodes())
				if (current.getNodeName() == "BODY" ) {
					children = current.getChildNodes();
					i = 0;
				} else if (current.getNodeName() == "TEXT" || current.getNodeName() == "P") {					
					data.add(current.getTextContent());
				}			
			}
			
			String article = StringUtils.join(data, " ");
			articles.add(article);
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			System.out.println("a");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			System.out.println("b");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("c");
		}
		return articles;
	}
	
	public List<JSONArray> extractFromCrawler(Path file, String[] related) {
		List<JSONArray> results = new ArrayList<JSONArray>();
		String name = "" + file;
		boolean cont;
		
			try {
				Object obj = parser.parse(new FileReader(name));
				JSONObject jsonObject = (JSONObject) obj;
				JSONArray docs = (JSONArray)jsonObject.get("docs");
				cont = false;
				for (int i = 0; i < docs.size(); i++) {
					JSONObject doc = (JSONObject) docs.get(i);
					JSONArray comments = (JSONArray)doc.get("comments");
					if (comments.size() == 0) continue;
					String article = ((String)doc.get("article")).toLowerCase();
					for (String term:related) {
						if (article.indexOf(term) == -1) {
							cont = true;
							break;
						}
					}
					if (cont) continue;
					results.add(comments);
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		return results;
	}
}
