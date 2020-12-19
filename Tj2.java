// TODO: remove unneccessary dependencies
//import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
//import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

public class Tj2 {
	public static void main(String[] args) {
		System.out.print("Got "+args.length+" args: ");
		for (String arg: args) {
			System.out.print(arg+" ");
		}
		System.out.println();

		if (args.length < 1 || args.length > 3) {
			System.out.println("usage: tj.exe <program> <input-dir> <output-dir>");
			System.exit(0);
		}

		String program = args[0];
		Path input_dir;
		Path output_dir;

		if (args.length > 1)
			input_dir = Paths.get(args[1]);
		else
			input_dir = Paths.get(".");

		if (args.length > 2)
			output_dir = Paths.get(args[2]);
		else
			output_dir = Paths.get(".");

		// read all input files ("vhod*.txt")
		HashMap<String,String> vhodi = new HashMap<String,String>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(input_dir, "vhod*.txt")) {
			for (Path file : stream) {
				System.out.println("Reading: "+file.toString());
				List<String> read = Files.readAllLines(file);
				
				// flatten List<String> to a single \n-separated string
				StringBuilder buffer = new StringBuilder();
				for (int line = 0; line < read.size(); line++) {
					buffer.append(read.get(line));
					buffer.append("\n");
				}

				String filename = file.getFileName().toString();
				String fileIndex = filename.substring(4, filename.length()-4); // cut away "vhod" and ".txt"

				vhodi.put(fileIndex, buffer.toString());
			}
		}
		catch (IOException e) {
			System.out.println("Napaka pri branju iz "+dir);

			// System.out.println("Sporocilo napake: "+e.getMessage());
			
			// e.getMessage() is useless for some reason... Workaround:
			// print stacktrace to a String and print the first line,
			// which should be the error message
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String stacktrace = sw.toString();
			String firstline = stacktrace.split("\\r?\\n")[0]; // split by newline and grab the first element

			System.out.println("Sporocilo napake: "+firstline);

			// this is equally functional :(
			// but I'm too proud of my hacky solution
			//System.out.println("Sporocilo napake: "+e.toString());

			System.exit(0);
		}


		// TODO:
		// run program for each input
		// save outputs to files
		// lp
	}
}
