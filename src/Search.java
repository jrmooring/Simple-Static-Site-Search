import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.htmlparser.Node;
import org.htmlparser.beans.StringBean;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.ParserException;

public class Search {
	public static void main(String[] args) throws Exception {
		PrintWriter out = new PrintWriter("/home/john/Desktop/indexedWords.js");

		List<File> files = getAllFiles(
				new File(
						"/home/john/Desktop/apache-tomcat-7.0.29/webapps/ROOT/public/"),
				"html");
		out.println("var a={};");
		int x = 0;
		for (File f : files) {
			out.println("a[" + x++ + "]=\"" + getRelPath(f, "public")
					+ "\"");
		}
		out.println("var b={};");
		LinkedHashMap<String, HashSet<Integer>> words = new LinkedHashMap<String, HashSet<Integer>>();
		for (File f : files) {
			Scanner s = new Scanner(extractText(f.getAbsolutePath()));
			s.useDelimiter("[\\W]");
			while (s.hasNext()) {
				String word = s.next().toLowerCase().trim();
				if(word.length()<2)
					continue;
				if (!words.containsKey(word)) {
					words.put(word, new HashSet<Integer>());
				}
				words.get(word).add(files.indexOf(f));
			}
		}
		for (String word : words.keySet()) {
			String indexes = "[";
			for(int idx : words.get(word)){
				indexes += idx+",";
			}
			out.println("b['" + word + "']=" + indexes.substring(0, indexes.length()-1) + "];");
		}
		out.println("var search = function(phrase){var words = phrase.split(\"+\").filter(function(x){return x.length > 1}).map(function(x){return x.toLowerCase()});var filtered = [];for(i in words){filtered.push(b[words[i]]);}rresult = [];for(i in filtered[0]){var found = true;for(j in filtered){if(!filtered[j].some(function (x){return x == filtered[0][i]})){found = false;break;}}if(found){rresult.push(filtered[0][i]);}}var result = [];for(i in rresult){result.push(a[rresult[i]]);}return result;}");
		out.flush();
		out.close();
	}

	public static String getRelPath(File f, String rootDir) {
		return f.getAbsolutePath().substring(
				f.getAbsolutePath().indexOf(rootDir) + (rootDir.length() + 1));
	}

	public static List<File> getAllFiles(File path, String extension) {
		ArrayList<File> files = new ArrayList<File>();
		getAllFiles(files, path, extension);
		return files;
	}

	private static void getAllFiles(List<File> files, File path,
			String extension) {
		for (File f : path.listFiles()) {
			if (f.isDirectory())
				getAllFiles(files, f, extension);
			else {
				if (f.getAbsolutePath().endsWith(extension))
					files.add(f);
			}
		}
	}

	private static String extractText(String path) {
		StringBean b = new StringBean();
		b.setLinks(false);
		b.setURL(path);
		return b.getStrings();
	}

	private static void populateXMLTree(DefaultMutableTreeNode tree, Node node) {
		if (node.getChildren() != null)
			for (int i = 0; i < node.getChildren().size(); i++) {
				DefaultMutableTreeNode cn = new DefaultMutableTreeNode(node
						.getChildren().elementAt(i).getPage());
				populateXMLTree(cn, node.getChildren().elementAt(i));
				tree.add(cn);
			}
	}

	public static void browseGuiTree(NodeIterator itr) throws ParserException {
		JFrame gui = new JFrame();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("HTML");
		itr.nextNode();
		populateXMLTree(root, itr.nextNode());
		final JTree tree = new JTree(root);
		gui.add(new JScrollPane(tree));
		gui.setSize(400, 400);
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setVisible(true);
	}
}
