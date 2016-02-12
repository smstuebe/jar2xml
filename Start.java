/* 
 *  Copyright (c) 2011 Xamarin Inc.
 * 
 *  Permission is hereby granted, free of charge, to any person 
 *  obtaining a copy of this software and associated documentation 
 *  files (the "Software"), to deal in the Software without restriction, 
 *  including without limitation the rights to use, copy, modify, merge, 
 *  publish, distribute, sublicense, and/or sell copies of the Software, 
 *  and to permit persons to whom the Software is furnished to do so, 
 *  subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be 
 *  included in all copies or substantial portions of the Software.
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 *  SOFTWARE.
 */

package jar2xml;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Start {

	public static void main (String[] args)
	{
		String droiddocs = null;
		String javadocs = null;
		String java7docs = null;
		String java8docs = null;
		String annots = null;
		List<String> jar_paths = new ArrayList<String> ();
		String out_path = null;
		List<String> additional_jar_paths = new ArrayList<String> ();
		String usage = "Usage: jar2xml --jar=<jarfile> [--ref=<jarfile>] --out=<file> [--javadocpath=<javadoc>] [--java7docpath=<java7doc>] [--java8docpath=<java8doc>] [--droiddocpath=<droiddoc>] [--annotations=<xmlfile>]";

		for (String arg : args) {
			if (arg.startsWith ("--javadocpath=")) {
				javadocs = arg.substring (14);
				if (!javadocs.endsWith ("/"))
					javadocs += "/";
			} else if (arg.startsWith ("--java7docpath=")) {
				java7docs = arg.substring (15);
				if (!java7docs.endsWith ("/"))
					java7docs += "/";
			} else if (arg.startsWith ("--java8docpath=")) {
				java8docs = arg.substring (15);
				if (!java8docs.endsWith ("/"))
					java8docs += "/";
			} else if (arg.startsWith ("--droiddocpath=")) {
				droiddocs = arg.substring (15);
				if (!droiddocs.endsWith ("/"))
					droiddocs += "/";
			} else if (arg.startsWith ("--annotations=")) {
				annots = arg.substring (14);
			} else if (arg.startsWith ("--jar=")) {
				jar_paths.add (arg.substring (6));
			} else if (arg.startsWith ("--ref=")) {
				additional_jar_paths.add (arg.substring (6));
			} else if (arg.startsWith ("--out=")) {
				out_path = arg.substring (6);
			} else {
				System.err.println (usage);
				System.exit (1);
			}
		}

		if (jar_paths.size() == 0 || out_path == null) {
			System.err.println (usage);
			System.exit (1);
		}
		File dir = new File (out_path).getAbsoluteFile ().getParentFile ();
		if (!dir.exists ())
			dir.mkdirs ();

		JavaArchive jar = null;
		try {
			jar = new JavaArchive (jar_paths, additional_jar_paths);
		} catch (Exception e) {
			System.err.println ("error J2X0001: Couldn't open java archive : " + e);
			System.exit (1);
		}

		try {
			if (annots != null)
				AndroidDocScraper.loadXml (annots);
			if (droiddocs != null)
				JavaClass.addDocScraper (new DroidDocScraper (new File (droiddocs)));
			if (javadocs != null)
				JavaClass.addDocScraper (new JavaDocScraper (new File (javadocs)));
			if (java7docs != null)
				JavaClass.addDocScraper (new Java7DocScraper (new File (java7docs)));
			if (java8docs != null)
				JavaClass.addDocScraper (new Java8DocScraper (new File (java8docs)));
		} catch (Exception e) {
			e.printStackTrace ();
			System.err.println ("warning J2X8001: Couldn't access javadocs at specified docpath.  Continuing without it...");
		}

		Document doc = null;
		try {
			DocumentBuilderFactory builder_factory = DocumentBuilderFactory.newInstance ();
			DocumentBuilder builder = builder_factory.newDocumentBuilder ();
			doc = builder.newDocument ();
		} catch (Exception e) {
			System.err.println ("warning J2X8002: Couldn't create xml document - exception occurred:" + e.getMessage ());
		}

		try {
			Element root = doc.createElement ("api");
			doc.appendChild (root);
			for (JavaPackage pkg : jar.getPackages ())
				pkg.appendToDocument (doc, root);
		} catch (Exception e) {
			e.printStackTrace ();
			System.err.println ("error J2X0002: API analyzer failed with java exception. See verbose output for details.");
			System.exit (1);
		}

		try {
			TransformerFactory transformer_factory = TransformerFactory.newInstance ();
			Transformer transformer = transformer_factory.newTransformer ();
			transformer.setOutputProperty (OutputKeys.INDENT, "yes");
			FileOutputStream stream = new FileOutputStream(out_path);
			OutputStreamWriter writer = new OutputStreamWriter(stream,"UTF-8");
			StreamResult result = new StreamResult (writer);
			DOMSource source = new DOMSource (doc);
			transformer.transform (source, result);
			writer.close ();
		} catch (Exception e) {
			System.err.println ("error J2X0003: Couldn't format xml file - exception occurred:" + e.getMessage ());
			System.exit (1);
		}
	}
}

