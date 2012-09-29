import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length < 2
				|| !(args[0].equals("--index") || args[0].equals("--replace"))) {
			printUsage();
		} else if (args[0].equals("--index")) {
			if (args.length != 4)
				printUsage();
			try {
				indexSite(new File(args[1]), new File(args[2]), new File(
						args[3]));
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else if (args[0].equals("--replace")) {
			if (args.length != 5)
				printUsage();
			try {
				replaceElement(new File(args[1]), args[2], new File(args[3]),
						args[4]);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

	}

	public static void printUsage() {
		System.out
				.println("Usage: java -jar SiteTools.jar \n"
						+ "       [ --index root_directory output_file search_root] | \n"
						+ "       [ --replace input_html_file css_id \n"
						+ "            replace_directory replace_file_css_id ]\n\n"
						+ "  --index root output search_root\n"
						+ "      index all html files in root directory, write\n"
						+ "      resulting javascript file to output.\n"
						+ "      file paths in the output file will be relative to search_root\n"
						+ "  --replace input input_id replace_dir replace_id\n"
						+ "      replace html element with id replace_id in all files in \n"
						+ "      replace_dir with the html element in input with the id input_id");
		System.exit(1);
	}

	/*
	 * Replaces the first element that matches the css identifier outId in all
	 * html files in the directory tree specified by directory with the first
	 * element that matches the css identifier id in the file input.
	 */
	public static void replaceElement(File input, String id, File directory,
			String outId) throws IOException {
		Element footer = Jsoup.parse(input, "UTF-8").select(id).get(0);
		for (File f : getAllFiles(directory, "html")) {
			Document d = Jsoup.parse(f, "UTF-8");
			try {
				d.select(outId).get(0).replaceWith(footer);
				PrintWriter p = new PrintWriter(f);
				p.println(d);
				p.flush();
				p.close();
			} catch (Exception e) {
				System.out.println(f);
			}
		}
		System.out.println(footer);
	}

	/*
	 * Finds all html files in the input directory. Builds a javascript multimap
	 * of all words from the text content of all .html files found in the input
	 * directory and all its sub directories. Appends a minified search function
	 * to the mutimap that filters pages down to only those containing all
	 * passed words.
	 */
	public static void indexSite(File input, File output, File relativeTo)
			throws FileNotFoundException {
		PrintWriter out = new PrintWriter(output);

		List<File> files = getAllFiles(input, "html");
		out.println("var a={};");
		int x = 0;
		for (File f : files) {
			out.println("a[" + x++ + "]=\"" + getRelativePath(relativeTo, f)
					+ "\"");
		}
		out.println("var b={};");
		LinkedHashMap<String, HashSet<Integer>> words = new LinkedHashMap<String, HashSet<Integer>>();
		for (File f : files) {
			Scanner s = new Scanner(extractText(f));
			s.useDelimiter("[\\W]");
			while (s.hasNext()) {
				String word = s.next().toLowerCase().trim();
				if (word.length() < 2)
					continue;
				if (!words.containsKey(word)) {
					words.put(word, new HashSet<Integer>());
				}
				words.get(word).add(files.indexOf(f));
			}
		}
		for (String word : words.keySet()) {
			String indexes = "[";
			for (int idx : words.get(word)) {
				indexes += idx + ",";
			}
			out.println("b['" + word + "']="
					+ indexes.substring(0, indexes.length() - 1) + "];");
		}

		/**
		 * Coffeescript search function: search = (phrase) -> sites
		 * =(b[w.toLowerCase()]for w in phrase.split(/[\W]/) when w.length>1)
		 * a[site] for site in sites[0]? when false not in (site in s? for s in
		 * sites[1..])
		 */

		out.println("var search=function(c){try{function d(a,b){res=[];for(var c=0;c<a.length;c++)if(b(a[c]))res.push(a[c]);return res}function e(a,b){res=[];for(var c=0;c<a.length;c++)res.push(b(a[c]));return res}function f(a,b){for(var c=0;c<a.length;c++)if(c in a&&b(a[c]))return true;return false}var g=unescape(c).split(/[\\W)]/);g=d(g,function(a){return a.length>1});g=e(g,function(a){return a.toLowerCase()});var h=[];for(i in g){h.push(b[g[i]])}rresult=[];for(i in h[0]){var k=true;for(j in h){if(!f(h[j],function(a){return a==h[0][i]})){k=false;break}}if(k){rresult.push(h[0][i])}}var l=[];for(i in rresult){l.push(a[rresult[i]])}return l}catch(e){return []}}");
		out.close();
	}

	/*
	 * returns the file path of f relative to rootDir
	 */
	private static List<String> getPathList(File f) {
		List<String> l = new ArrayList<String>();
		File r;
		try {
			r = f.getCanonicalFile();
			while (r != null) {
				l.add(r.getName());
				r = r.getParentFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
			l = null;
		}
		return l;
	}

	/**
	 * figure out a string representing the relative path of 'f' with respect to
	 * 'r'
	 * 
	 * @param r
	 *            home path
	 * @param f
	 *            path of file
	 */
	private static String matchPathLists(List<String> r, List<String> f) {
		int i;
		int j;
		String s;
		// start at the beginning of the lists
		// iterate while both lists are equal
		s = "";
		i = r.size() - 1;
		j = f.size() - 1;
		// first eliminate common root
		while ((i >= 0) && (j >= 0) && (r.get(i).equals(f.get(j)))) {
			i--;
			j--;
		}
		// for each remaining level in the home path, add a ..
		for (; i >= 0; i--) {
			s += ".." + File.separator;
		}
		// for each level in the file path, add the path
		for (; j >= 1; j--) {
			s += f.get(j) + File.separator;
		}
		// file name
		s += f.get(j);
		return s;
	}

	/**
	 * get relative path of File 'f' with respect to 'home' directory example :
	 * home = /a/b/c f = /a/d/e/x.txt s = getRelativePath(home,f) =
	 * ../../d/e/x.txt
	 * 
	 * @param home
	 *            base path, should be a directory, not a file, or it doesn't
	 *            make sense
	 * @param f
	 *            file to generate path for
	 * @return path from home to f as a string
	 */
	private static String getRelativePath(File home, File f) {
		List<String> homelist;
		List<String> filelist;
		String s;
		homelist = getPathList(home);
		filelist = getPathList(f);
		s = matchPathLists(homelist, filelist);
		return s;
	}

	/*
	 * Returns an array of all files ending in the passed extension found within
	 * the directory tree specified by path
	 */
	private static List<File> getAllFiles(File path, String extension) {
		ArrayList<File> files = new ArrayList<File>();
		getAllFiles(files, path, extension);
		return files;
	}

	/*
	 * Helper method for getAllFiles.
	 */
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

	/*
	 * Extracts all of the text content from the passed html file.
	 */
	private static String extractText(File path) {
		StringBean b = new StringBean();
		b.setLinks(false);
		b.setURL(path.getAbsolutePath());
		return b.getStrings();
	}

	/*
	 * Copies all elements from node to tree. Use for browsing xhtml in a JTree
	 */
	private static void populateXHTMLTree(DefaultMutableTreeNode tree, Node node) {
		if (node.getChildren() != null)
			for (int i = 0; i < node.getChildren().size(); i++) {
				DefaultMutableTreeNode cn = new DefaultMutableTreeNode(node
						.getChildren().elementAt(i).getPage());
				populateXHTMLTree(cn, node.getChildren().elementAt(i));
				tree.add(cn);
			}
	}

	/*
	 * Browse an XHTML file in a gui.
	 */
	public static void browseGuiTree(NodeIterator itr) throws ParserException {
		JFrame gui = new JFrame();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("HTML");
		itr.nextNode();
		populateXHTMLTree(root, itr.nextNode());
		final JTree tree = new JTree(root);
		gui.add(new JScrollPane(tree));
		gui.setSize(400, 400);
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setVisible(true);
	}
}
