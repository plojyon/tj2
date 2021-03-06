import java.security.Permission;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.*;

public class Tj2 {
	private static boolean DEBUG = false;

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 3 || args[0].equals("help")) {
			System.out.println("uporaba: Tj2 <program> [time-limit] [input-dir]");
			System.out.println("primer: Tj2 DN04");
			System.out.println("primer: Tj2 DN04 5");
			System.out.println("primer: Tj2 DN04 1.5 ./testi");
			System.out.println("Pozor: pred zagonom Tj2 je potrebno program (in teste) prevesti (javac *.java)");
			System.out.println("");
			System.out.println("Legenda rezultatov:");
			System.out.println(colorize(Result.OK) + " ... ok");
			System.out.println(colorize(Result.WA) + " ... wrong answer");
			System.out.println(colorize(Result.TLE) + " ... time limit exceeded");
			System.out.println(colorize(Result.RTE) + " ... runtime error");
			System.out.println("");
			System.out.println("Z uporabo programa soglasate, da vase osebne podatke pustim pri miru");
			System.out.println("Hvala da izbirate prosto programsko opremo.");
			System.exit(0);
		}

		String program = args[0];
		Path inputDir;
		Path outputDir;
		int timeLimit; // in milliseconds

		if (args.length > 1)
			timeLimit = (int) (1000 * Float.parseFloat(args[1]));
		else
			timeLimit = 1000;

		/*
		if (args.length > 2)
			outputDir = Paths.get(args[2]);
		else
			outputDir = Paths.get(".");
		*/
		outputDir = Paths.get("."); // temporarily disabled

		if (args.length > 2)
			inputDir = Paths.get(args[1]);
		else
			inputDir = Paths.get(".");

		// read all files
		HashMap<String, IOFile> vhodi      = getFiles(inputDir, "vhod*.txt");
		HashMap<String, IOFile> javaTests  = getFiles(inputDir, "Test*.java");
		HashMap<String, IOFile> izhodi     = getFiles(inputDir, "izhod*.txt");
		
		if ((vhodi.size() + javaTests.size()) != izhodi.size()) {
			System.out.println("Pozor: neenako stevilo vhodov("+vhodi.size()+" + "+javaTests.size()+") in izhodov("+izhodi.size()+")!");
		}
		if (DEBUG) System.out.println("Prebrano "+vhodi.size()+" vhodov, "+javaTests.size()+" java testov in "+izhodi.size()+" izhodov.");

		if (DEBUG) System.out.println("Printing izhodi:");
		for (String izhod: izhodi.keySet()) {
			if (DEBUG) System.out.println(izhodi.get(izhod));
		}

		// create Test objects
		ArrayList<Test> tests = new ArrayList<Test>();
		Method main = getMain(program);
		for (String fname: izhodi.keySet()) {
			IOFile file;

			// associate inputs with outputs
			if (vhodi.containsKey(fname))
				file = izhodi.get(fname).addInput(vhodi.get(fname));
			else if (javaTests.containsKey(fname))
				file = izhodi.get(fname).addInput(javaTests.get(fname));
			else {
				System.out.println("Pozor: izhod " + fname + " nima pripadajocega vhoda! Ignoriram ...");
				continue;
			}

			Method method = file.isJava() ? getMain("Test" + file.getName()) : main;
			tests.add(new Test(file, method, timeLimit));
		}
		if (DEBUG) System.out.println("Imam teste: " + tests);


		// run all tests
		int okCount = 0;
		int notOkCount = 0;
		for (Test test: tests) {
			if (DEBUG) System.out.println("Testiram "+test.getName());
			
			test.run();
			System.out.println(test.getName()+" ... "+colorize(test.getResult()));

			if (test.getResult() == Result.OK) okCount++;
			else notOkCount++;
		}
		
		System.out.println("Tocke: "+okCount+"/"+(okCount+notOkCount)+" Lp");

		// save output to file (result.html)
		String htmlReport = generateOutputFile(tests);
		try {
			FileWriter myWriter = new FileWriter(outputDir + "/report.html");
			myWriter.write(htmlReport);
			myWriter.close();
			System.out.println("Podrobno porocilo shranjeno v "+outputDir+"/report.html");
		} catch (IOException e) {
			System.out.println("Prislo je do napake pri zapisovanju v datoteko.");
			System.out.println(e);
			e.printStackTrace();
		}
		
		System.out.println("\nHvala ker uporabljate Tj2");
	}

	private static String generateOutputFile(ArrayList<Test> results) {
		StringBuilder sb = new StringBuilder();
		sb.append("<span class='button' onclick='openall()'>Odpri vse</span>");
		sb.append("<span class='button' onclick='openwa()'>Odpri napacne</span>");
		sb.append("<span class='button' onclick='closeall()'>Zapri vse</span>");
		for (Test test: results) {
			sb.append("<details>");
				sb.append("<summary class='"+test.getResult()+"'>");
					sb.append("<span class='name'>" + test.getName() + "</span>");
					sb.append("<span class='result'>" + test.getResult() + "</span>");
				sb.append("</summary>");
				sb.append("<div class='details'>");
					sb.append("<fieldset class='output code'><legend>Vas izhod</legend>" + test.getOut().replace("\n", "<br>") + "</fieldset>");
					sb.append("<fieldset class='expected code'><legend>Pricakovan izhod</legend>" + test.getExpectedOut().replace("\n", "<br>") + "</fieldset>");
					if (!test.getErr().equals("null"))
						sb.append("<fieldset class='stderr code'><legend>Izhod stderr</legend>" + test.getErr().replace("\n", "<br>") + "</fieldset>");
				sb.append("</div>");
			sb.append("</details>");
		}
		sb.append("<style>");
			sb.append("body{background-color: #07080B; color: #999999; font-family:sans-serif; font-size: 20px}");
			sb.append(".OK{color:#96AB78}");
			sb.append(".WA{color:#A15A5E}");
			sb.append(".RTE{color:#8F71A2}");
			sb.append(".TLE{color:#B69963}");
			sb.append(".name{margin: 0.5em 1em}");
			sb.append("summary {font-size: 1.1em; font-weight: bold; cursor: pointer}");
			sb.append("fieldset{border: 1px solid #999999}");
			sb.append(".code{font-family:monospace; border: 1px solid #999999}");
			sb.append(".details{display:grid; grid-template-rows:2; grid-template-columns:1; margin-bottom: 1em}");
			sb.append(".output{grid-column:1 / span 1; grid-row: 1 / span 1;}");
			sb.append(".expected{grid-column:2 / span 1; grid-row: 1 / span 1;}");
			sb.append(".stderr{grid-column:1 / span 2; grid-row: 1 / span 1;}");

			sb.append(".button {cursor: pointer; border: 1px solid #999999; border-radius: 5px; padding: 0.5em 1em; margin: 1em; display:inline-block; background-color: rgba(0,0,0,0.5)}");
		sb.append("</style>");
		sb.append("<script>");
			sb.append("function openall() {Array.from(document.querySelectorAll('summary')).forEach(function(obj, idx) {obj.parentElement.open = true;});}");
			sb.append("function closeall() {Array.from(document.querySelectorAll('summary')).forEach(function(obj, idx) {obj.parentElement.open = false;});}");
			sb.append("function openwa() {Array.from(document.querySelectorAll('summary:not(.ok)')).forEach(function(obj, idx) {obj.parentElement.open = true;});}");
		sb.append("</script>");
		return sb.toString();
	}

	private static enum Result {
		OK, // okay
		WA, // wrong answer
		RTE, // runtime error (exception thrown or stderr is non-empty)
		TLE // time limit exceeded
	}

	private static String colorize(Result result){
		switch(result){
			case OK:
				return "\033[0;32m[+] OK\033[0m";
			case WA:
				return "\033[0;31m[-] WA\033[0m";
			case TLE:
				return "\033[1;33m[*] TLE\033[0m";
			case RTE:
				return "\033[1;35m[?] RTE\033[0m";
			default:
				return "Shit";
		}
	}

	

	// restore the environment: set the original stdin, stdout and stderr and re-enable System.exit
	private static void restoreEnv(InputStream origIn, PrintStream origOut, PrintStream origErr) {
		System.setIn(origIn);
		System.setOut(new PrintStream(origOut));
		System.setErr(new PrintStream(origErr));
		enableSystemExitCall();
	}

	// read all files matching a given pattern (like "vhod*.txt") in directory dir
	private static HashMap<String, IOFile> getFiles(Path dir, String pattern) {
		HashMap<String, IOFile> files = new HashMap<String, IOFile>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, pattern)) {
			for (Path file : stream) {
				if (DEBUG) System.out.println("Berem "+file);
				// read a List<String> of lines in the file
				List<String> read = Files.readAllLines(file);
				
				// flatten List<String> to a single \n-separated string
				StringBuilder buffer = new StringBuilder();
				for (int line = 0; line < read.size(); line++) {
					buffer.append(read.get(line));
					buffer.append("\n");
				}
				String filename = file.getFileName().toString();
				IOFile f = new IOFile(filename, buffer.toString());
				files.put(f.getName(), f);
			}
		}
		catch (IOException e) {
			System.out.println("Napaka pri branju iz "+dir.toString());
			System.out.println("Sporocilo napake: "+e.toString());
			System.exit(0);
		}
		return files;
	}

	// searches for a class named "program" and returns its main method
	private static Method getMain(String program) {
		try {
			return Class.forName(program).getMethod("main", String[].class);
		} catch (ClassNotFoundException e) {
			System.err.println("Razred " + program + " ne obstaja! Si pognal `javac *.java`?");
			System.exit(0);
		} catch (NoSuchMethodException e) {
			System.err.println("Razred " + program + " nima metode main!");
			System.exit(0);
		}
		return null; // it shouldn't come to this unless System.exit() somehow failed
	}

	/*
		For simplicity's sake, please consider not adding new .java files to the project.
		I would like to avoid the need to package it into a .jar, because compiling
		and running a .java is a lot more native to students; the target audience.
	*/
	// A security manager implementation that will catch calls to System.exit
	private static class ExitTrappedException extends SecurityException {/* burger time */}
	private static void forbidSystemExitCall() {
		// https://stackoverflow.com/a/5401402
		final SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission permission) {
				if (permission.getName().startsWith("exitVM")) {
					throw new ExitTrappedException();
				}
			}
		};
		System.setSecurityManager(securityManager);
	}
	private static void enableSystemExitCall() {
		System.setSecurityManager(null);
	}
	private static class Test implements Runnable {
		private IOFile file;
		private Method main; // main method (either the java test, or our program)
		private int timeLimit;

		private String stdout;
		private String stderr;
		private Result result;

		public Test(IOFile file, Method main, int timeLimit) {
			this.file = file;
			this.main = main;
			this.timeLimit = timeLimit;

			if (DEBUG) System.out.println(file);
		}

		public String getName() {
			return this.file.getName();
		}
		public String getInput() {
			return this.file.getInput();
		}
		public String getExpectedOut() {
			return this.file.getOutput();
		}
		public String getOut() {
			return this.stdout;
		}
		public String getErr() {
			return this.stderr;
		}
		public Result getResult() {
			return this.result;
		}
		public boolean isJava() {
			return this.file.isJava;
		}

		// run the main method of program with given input, expected output and time limit
		// returns the test result
		// also writes the outputs to a global HashMap<String,String> outputs
		// DO NOT ATTEMPT TO MAKE THIS ASYNC! The restoreEnv() hack will break in an async environemnt!!!
		@SuppressWarnings("deprecation") // ok boomer
		public void run() {
			// change stdin and stdout to a custom PrintStream, to capture program output
			ByteArrayInputStream   inStream = new ByteArrayInputStream(this.file.getInput().getBytes());
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			ByteArrayOutputStream errStream = new ByteArrayOutputStream();
			InputStream origIn  = System.in;
			PrintStream origOut = System.out; // make a backup of the original stdout
			PrintStream origErr = System.err;
			System.setIn(inStream);
			System.setOut(new PrintStream(outStream));
			System.setErr(new PrintStream(errStream));
			
			forbidSystemExitCall();
			boolean tle = true;
			String err = "";
			Thread parent = Thread.currentThread();
			Object[] args = new Object[] { this.main, err, parent }; // this feels like opening a shoe box with a screwdriver

			Thread program = new Thread(new Runnable() {
				Object[] arguments = args; // must be an object[] so that this thread modifies the originals

				public void run() {
					try {
						((Method)this.arguments[0]).invoke(null, new Object[] { new String[] {} });
						((Thread)this.arguments[2]).interrupt();
					} catch (ExitTrappedException e) {
						// this is thrown by the securityManager when the program calls System.exit()
						// do nothing here, the output might still be OK
					} catch (InvocationTargetException e) {
						// this is for every other exception by the program.
						// this counts as a RTE

						// print stack trace to stderr
						StringWriter errata = new StringWriter();
						e.printStackTrace(new PrintWriter(errata));
						this.arguments[1] += errata.toString();
					} catch (IllegalAccessException e) {
						// wtf
						this.arguments[1] += "Tj2: IllegalAccessException when invoking main";
					} catch (IllegalArgumentException e) {
						// program expected arguments??
						this.arguments[1] += "Tj2: IllegalArgumentException when invoking main";
					}
				};
			});
			try {
				program.start();
				TimeUnit.MILLISECONDS.sleep(this.timeLimit);
				program.stop(); // This is deprecated since Java 1.2, but it does exactly what I need
			}
			catch (InterruptedException e) {
				// program finished before the timer was over
				tle = false;
			}
			finally {
				restoreEnv(origIn, origOut, origErr);
			}

			// if program broke lines with \r\n, repace with \n only
			this.stdout  = outStream.toString().replaceAll("\\r", "");
			this.stderr += errStream.toString().replaceAll("\\r", "");

			if (this.stderr.length() > 0) {
				this.result = Result.RTE;
			}

			if (this.stdout.equals(this.file.getOutput()))
				this.result = Result.OK;
			else
				this.result = Result.WA;

			if (tle)
				this.result = Result.TLE;

			if (DEBUG) System.out.println(this.getName()+" output length: "+this.stdout.length());
			if (DEBUG) System.out.println(this.getName()+" expected length: "+this.file.getOutput().length());
		}
	}
	private static class IOFile {
		String name; // file index (test01.txt -> 01)
		boolean isJava; // is this a java test?
		String input;
		String output;
		
		IOFile(String filename, String contents) {
			boolean isInput = true;
			if (filename.startsWith("izhod"))
				isInput = false;

			// save contents
			this.input = "";
			this.output = "";
			if (isInput)
				this.input = contents;
			else
				this.output = contents;

			// determine if this is a java test
			if (filename.endsWith(".java"))
				this.isJava = true;

			// strip "vhod"/"izhod" and ".txt"/".java" from filename to get file index
			int startTrim = 0;
			if (filename.startsWith("vhod"))
				startTrim = 4;
			else if (filename.startsWith("izhod"))
				startTrim = 5;
			else if (filename.startsWith("Test"))
				startTrim = 4;

			int endTrim = 0;
			if (filename.endsWith(".txt"))
				endTrim = 4;
			else if (filename.endsWith(".java"))
				endTrim = 5;

			this.name = filename.substring(startTrim, filename.length() - endTrim);
			if (DEBUG) System.out.println("Made an "+this);
		}

		// merges with another IOFile (treat it as an input)
		public IOFile addInput(IOFile in) {
			this.isJava = in.isJava();
			this.input = in.getInput();
			return this;
		}

		// merges with another IOFile (treat it as an output)
		public IOFile addOutput(IOFile out) {
			this.output = out.getOutput();
			return this;
		}

		public String getName() {
			return this.name;
		}
		public String getInput() {
			return this.input;
		}
		public String getOutput() {
			return this.output;
		}
		public boolean isJava() {
			return this.isJava;
		}

		public String toString() {
			return "IOFile["+this.name+": input("+this.input.length()+"), output("+this.output.length()+"), isJava("+this.isJava+")]";
		}
	}
}