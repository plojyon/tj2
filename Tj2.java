import java.security.Permission;

import java.io.*;
import java.nio.file.*;
import java.util.List;

import javax.lang.model.util.ElementScanner6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.reflect.*;

public class Tj2 {
	private static boolean DEBUG = false;

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 3 || (args[0] != null && args[0].equals("help"))) {
			System.out.println("uporaba: Tj2 <program> [input-dir] [output-dir]");
			System.exit(0);
		}

		String program = args[0];
		Path inputDir;
		Path outputDir;

		int timeLimit = 1; // TODO: implement time limit

		if (args.length > 1)
			inputDir = Paths.get(args[1]);
		else
			inputDir = Paths.get(".");

		if (args.length > 2)
			outputDir = Paths.get(args[2]);
		else
			outputDir = Paths.get(".");

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
		// associate inputs with outputs
		for (String izhod: izhodi.keySet()) {
			if (vhodi.containsKey(izhod))
				izhodi.get(izhod).addInput(vhodi.get(izhod));
			else if (javaTests.containsKey(izhod))
				izhodi.get(izhod).addInput(javaTests.get(izhod));
			else
				System.out.println("Izhod "+izhod+" nima pripadajocega vhoda! Ignoriram ...");
		}

		// Create Test objects
		ArrayList<Test> tests = new ArrayList<Test>();
		Method main = getMain(program);
		for (String fname: izhodi.keySet()) {
			IOFile file = izhodi.get(fname);
			Method method = file.isJava()? getMain("Test"+file.getName()) : main;
			tests.add(new Test(file, method, 1));
		}

		// run all tests
		int okCount = 0;
		int notOkCount = 0;
		for (Test test: tests) {
			if (DEBUG) System.out.println("Testiram "+test.getName());
			
			Result result = test.run(timeLimit);
			System.out.println(test.getName()+" ... "+colorize(result));

			if (result == Result.OK) okCount++;
			else notOkCount++;
		}
		
		System.out.println("Tocke: "+okCount+"/"+(okCount+notOkCount)+" Lp");

		// TODO: uncomment when ready
		//System.out.println(saveOutputFile(outputDir, tests));
	}

	private static String saveOutputFile(Path outputDir, ArrayList<Test> results) {
		boolean ok = false;
		/*
			Test
				getName() // get name of test (usually the index number, but Tj2 supports anything)
				getInput() // return given input (will return souce of .java test if it's a java test)
				isJava() // is this a java test?
				getExpectedOut() // return expected stdout
				getOut() // return stdout
				getErr() // return stderr
				getResult() // return Result enum (OK, WA, RTE, TLE)
		*/
		if (ok)
			return "Izhodna datoteka pripravljena na "+outputDir;
		else 
			return "Ni mogoce shraniti izhodne datoteke: Error: method saveOutputFile not implemented";
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
			System.err.println("Razred " + program + " ne obstaja!");
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
	private static class Test {
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
		public Result run(int timeLimit) {
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
			try {
				this.main.invoke(null, new Object[] {new String[]{}});
			} catch (ExitTrappedException e) {
				// this is thrown by the securityManager when the program calls System.exit()
				// do nothing here, the output might still be OK
			} catch (InvocationTargetException e) {
				// this is for every other exception by the program.
				// this counts as a RTE
				
				// print stack trace to stderr
				StringWriter errata = new StringWriter();
				e.printStackTrace(new PrintWriter(errata));
				this.stderr += errata.toString();
			}
			catch (IllegalAccessException e) {
				// wtf
				this.stderr += "Tj2: IllegalAccessException when invoking main";
			}
			catch (IllegalArgumentException e) {
				// program expected arguments??
				this.stderr += "Tj2: IllegalArgumentException when invoking main";
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

			if (DEBUG) System.out.println(this.getName()+" output length: "+this.stdout.length());
			if (DEBUG) System.out.println(this.getName()+" expected length: "+this.file.getOutput().length());
			return this.result;
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
		public void addInput(IOFile in) {
			this.isJava = in.isJava();
			this.input = in.getInput();
		}

		// merges with another IOFile (treat it as an output)
		public void addOutput(IOFile out) {
			this.output = out.getOutput();
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