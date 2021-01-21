import java.security.Permission;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.HashMap;
import java.lang.reflect.*;

public class Tj2 {
	//private static HashMap<String,Result> results = new HashMap<String,Result>(); // 01 > OK; 02 > WA; ...
	private static boolean DEBUG = false;

	public static void main(String[] args) {
		if (args.length < 1 || args.length > 3) {
			System.out.println("uporaba: Tj2 <program> <input-dir> <output-dir>");
			System.exit(0);
		}

		String program = args[0];
		Path input_dir;
		Path output_dir;
		int time_limit = 1;

		if (args.length > 1)
			input_dir = Paths.get(args[1]);
		else
			input_dir = Paths.get(".");

		if (args.length > 2)
			output_dir = Paths.get(args[2]);
		else
			output_dir = Paths.get(".");

		
		HashMap<String,String> vhodi   = getFiles(input_dir, "vhod*.txt");
		HashMap<String,String> izhodi  = getFiles(input_dir, "izhod*.txt");
		
		if (vhodi.size() != izhodi.size()) {
			System.out.println("Pozor: neenako stevilo vhodov in izhodov!");
		}
		if (DEBUG) System.out.println("Prebrano "+vhodi.size()+" vhodov in "+izhodi.size()+" izhodov.");

		int ok_count = 0;
		int not_ok_count = 0;
		try {
			Method main = Class.forName(program).getMethod("main", String[].class);

			for (String vhod: vhodi.keySet()) {
				if (DEBUG) System.out.println("Testiram "+vhod);
				if (!izhodi.containsKey(vhod)) {
					System.out.println("POZOR: "+vhod+" nima izhodne datoteke! Ignoriram ...");
					continue;
				}
				Result result = testProgram(main, vhodi.get(vhod), izhodi.get(vhod), time_limit);
				System.out.println(vhod+" ... "+colorize(result));

				if (result == Result.OK) ok_count++;
				else not_ok_count++;
			}
		}
		catch (ClassNotFoundException e) {
			System.err.println("Razred "+program+" ne obstaja!");
			System.exit(0);
		}
		catch (NoSuchMethodException e) {
			System.err.println("Razred "+program+" nima metode main!");
			System.exit(0);
		}
		System.out.println("Tocke: "+ok_count+"/"+(ok_count+not_ok_count)+" Lp");
	}

	private static enum Result {
		WA, // wrong answer
		TLE, // time limit exceeded
		RTE, // runtime error (exception thrown or stderr is non-empty)
		OK // okay
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

	// run the main method of program with given input, expected output and time limit
	// returns the test result
	// also writes the outputs to a global HashMap<String,String> outputs
	// TODO: make this async?
	public static Result testProgram(Method main, String input, String expected_out, int time_limit) {
		// change stdin and stdout to a custom PrintStream, to capture program output
		ByteArrayInputStream  in_stream  = new ByteArrayInputStream(input.getBytes());
		ByteArrayOutputStream out_stream = new ByteArrayOutputStream();
		ByteArrayOutputStream err_stream = new ByteArrayOutputStream();
		InputStream origIn  = System.in;
		PrintStream origOut = System.out; // make a backup of the original stdout
		PrintStream origErr = System.err;
		System.setIn(in_stream);
		System.setOut(new PrintStream(out_stream));
		System.setErr(new PrintStream(err_stream));
		
		Object[] arguments = new Object[1]; // need to create empty object of String args[] expected by main
		arguments[0] = new String[] {}; // TODO: is there a way to do this inline with main.invoke?
		forbidSystemExitCall();
		try {
			main.invoke(null, arguments);
		} catch (ExitTrappedException e) {
			// this is thrown by the securityManager when the program calls System.exit()
			// do nothing here, the output might still be OK
			if (DEBUG) origOut.println("Program called System.exit()");
		} catch (InvocationTargetException e) {
			// this is for every other exception by the program.
			// this counts as a RTE
			//e.printStackTrace(); // TODO: print it to err_stream
			restoreEnv(origIn, origOut, origErr);
			if (DEBUG) System.out.println("Program threw an exception");
			return Result.RTE;
		}
		catch (IllegalAccessException e) {
			// wtf
			restoreEnv(origIn, origOut, origErr);
			if (DEBUG) System.out.println("IllegalAccessException when invoking main");
			return Result.RTE;
		}
		catch (IllegalArgumentException e) {
			// program expected arguments??
			restoreEnv(origIn, origOut, origErr);
			if (DEBUG) System.out.println("IllegalArgumentException when invoking main");
			return Result.RTE;
		}
		restoreEnv(origIn, origOut, origErr);

		// if program broke lines with \r\n, repace with \n only
		String out = out_stream.toString().replaceAll("\\r", "");

		// TODO: write stdout/stderr/stacktrace to HashMap<String,String> outputs

		if (err_stream.toString().length() > 0) {
			return Result.RTE;
		}
		
		if (out.equals(expected_out))
			return Result.OK;
		else
			return Result.WA;
	}

	// restore the environment: set the original stdin, stdout and stderr and re-enable System.exit
	private static void restoreEnv(InputStream origIn, PrintStream origOut, PrintStream origErr) {
		System.setIn(origIn);
		System.setOut(new PrintStream(origOut));
		System.setErr(new PrintStream(origErr));
		enableSystemExitCall();
	}

	// read all files matching a given pattern (like "vhod*.txt") in directory dir
	// returns HashMap, where keys are the file indices (filename without "vhod" and ".txt") and values are file contents
	private static HashMap<String,String> getFiles(Path dir, String pattern) {
		HashMap<String,String> files = new HashMap<String,String>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, pattern)) {
			for (Path file : stream) {
				if (DEBUG) System.out.println("Berem "+file);
				List<String> read = Files.readAllLines(file);
				
				// flatten List<String> to a single \n-separated string
				StringBuilder buffer = new StringBuilder();
				for (int line = 0; line < read.size(); line++) {
					buffer.append(read.get(line));
					buffer.append("\n");
				}

				String filename = file.getFileName().toString();

				// strip "vhod"/"izhod" and ".txt" from filenames for HashMap keys
				if (filename.startsWith("vhod"))
					filename = filename.substring(4, filename.length()-4); // "vhod" and ".txt"
				else if (filename.startsWith("izhod"))
					filename = filename.substring(5, filename.length()-4); // "izhod" and ".txt"

				files.put(filename, buffer.toString());
			}
		}
		catch (IOException e) {
			System.out.println("Napaka pri branju iz "+dir.toString());
			System.out.println("Sporocilo napake: "+e.toString());
			System.exit(0);
		}
		return files;
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
}