package vn.newai.fileout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FileWriter {
	/**
	 * Write HTML String to file
	 * 
	 * @param html
	 *            HTML String to write
	 * @param filePath
	 *            path to save, must include "<code>.txt</code>" extension.
	 */
	public static void writeTextFile(String text, String filePath) {
		try {
			File fileOut = new File(filePath);
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), "UTF8"));
			out.append(text);
			out.flush();
			out.close();
			System.out.println("Write to text file completed " + filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
