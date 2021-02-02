# Tj2

A FOSS alternative to tj.exe, written in Java  
tj.exe is a program used to test Java programs used by the uni-lj Programiranje 1 course.  

To use, copy Tj2.java into the folder with your program and run  
`javac *.java`  
to compile your program and tests, then run  
`java Tj2 \<program\> \[time-limit\] \[input-dir\]`  
to evaluate your program with Tj2 (parameters in \[square brackets\] are optional).  

To permanently install Tj2, copy it to some folder on your computer, then run  
`javac Tj2.java`  
to compile Tj2 in that folder. After adding the folder to your classpath using  
`export CLASSPATH="${CLASSPATH};/home/yon/tj2"`  
you will be able to run `java Tj2` from anywhere, without having to copy or recompile Tj2.