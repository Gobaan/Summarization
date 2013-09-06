import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.util.StringUtils;


public class Printer {
	public enum Type {
		TIMING,
		TEXT,
		BOTH,
	}
	Type type;
	File f;
	List<String> toPrint;
	public Printer(Type t, String filename)  {
		this.type = t;
		String suffix = ".all.tsv";
		if (t == Type.TIMING) {
			suffix = ".timing.tsv";
		} else if (t == Type.TEXT) {
			suffix = ".text.tsv";
		}
		
		f = new File(filename + suffix);
		//clear the file
		try {
			FileOutputStream out = new FileOutputStream(f);
			out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void startRow() {
		toPrint = new ArrayList<String>();
	}
	
	public void addColumn(String value, Type type) {
		if (this.type == Type.BOTH || this.type == type || type == Type.BOTH) {
			//Replace all single quotes with escaped single quote			
			value = value.replaceAll("\"", "\"\"");
			toPrint.add("\"" + value + "\"");
		}
	}
	
	public void addColumn(long value, Type type) {		
		addColumn("" + value, type);
	}
	
	public void endRow () {		
		try {
			FileOutputStream out = new FileOutputStream(f, true);
			PrintWriter p = new PrintWriter(out);
			p.println(StringUtils.join(toPrint, "\t"));
			p.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
