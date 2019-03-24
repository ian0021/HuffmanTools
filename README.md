# HuffmanTools

1. encode file.
mvn  clean compile exec:java -Dexec.args="encode oui.txt dec.dec"

Step 1. getFrequency.
Step 2. huffmanCoding.
Step 3. transfor file to huffman code.
Huffman compress finish.
This process lasts ：3.171seconds
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.945 s
[INFO] Finished at: 2019-03-24T17:53:27+08:00
[INFO] ------------------------------------------------------------------------


2.decode file.
mvn  clean compile exec:java -Dexec.args="decode dec.dec out-ext.txt"
File extracting...
This process lasts ：6.901seconds
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 10.514 s
[INFO] Finished at: 2019-03-24T17:54:10+08:00
