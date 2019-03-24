package com.ian.huffmantools;

import java.util.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileNotFoundException;
 
abstract class HuffmanTree implements Comparable<HuffmanTree> {
    public final int frequency; // the frequency of this tree
    public HuffmanTree(int freq) { frequency = freq; }
 
    // compares on the frequency
    public int compareTo(HuffmanTree tree) {
        return frequency - tree.frequency;
    }
}
 
class HuffmanLeaf extends HuffmanTree {
    public final char value;
 
    public HuffmanLeaf(int freq, char val) {
        super(freq);
        value = val;
    }
}
 
class HuffmanNode extends HuffmanTree {
    public final HuffmanTree left, right; // subtrees
 
    public HuffmanNode(HuffmanTree l, HuffmanTree r) {
        super(l.frequency + r.frequency);
        left = l;
        right = r;
    }
}
 
public class HuffmanCode {
    public static HuffmanTree buildTree(int[] charFreqs) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();

        for (int i = 0; i < charFreqs.length; i++)
            if (charFreqs[i] > 0)
                trees.offer(new HuffmanLeaf(charFreqs[i], (char)i));
 
        assert trees.size() > 0;
        while (trees.size() > 1) {
            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();
 
            trees.offer(new HuffmanNode(a, b));
        }
        return trees.poll();
    }

    public static HuffmanTree buildTree(HashMap<Character, Integer> charMap){
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();

	    Iterator<Character> iterator = charMap.keySet().iterator();	
        while (iterator.hasNext())
        {
            char key = (char) iterator.next();
			trees.offer(new HuffmanLeaf(charMap.get(key), key));
		}
 
        assert trees.size() > 0;
        while (trees.size() > 1) {
            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();
 
            trees.offer(new HuffmanNode(a, b));
        }
        return trees.poll();
    }

    public static void generateCodes(HuffmanTree tree, String prefix, Map<Character, String> codeMap) {
        assert tree != null;
        if (tree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf)tree;
            codeMap.put(leaf.value, prefix);
 
        } else if (tree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode)tree;
 
            // traverse left
            generateCodes(node.left, prefix + '0', codeMap);
 
            // traverse right
            generateCodes(node.right, prefix + '1', codeMap);
        }
    }

    @SuppressWarnings("resource")
	public static HashMap<Character, Integer> getFrequency(String fileName)
    {
        HashMap<Character, Integer> charMap = new HashMap<Character, Integer>();
        File file = new File(fileName);
        try
        {
			FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
			FileLock lock = fileChannel.lock(0, file.length(), false);
			MappedByteBuffer mbBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());

	        long len = file.length();
	
	        for (int i=0; i<len; i++)
	        {
	            char c = (char)mbBuf.get(i);
	            if (charMap.get(c) == null)
	            {
	                charMap.put(c, 1);
	            }
	            else
	            {
	                charMap.put(c, charMap.get(c) + 1);
	            }
	        }
	        
            lock.release();
            // 关闭文件通道
            fileChannel.close();
        }
		catch (FileNotFoundException e)  
        {   
            e.printStackTrace();
        } catch (IOException e)  
        {   
            e.printStackTrace();
        }  

        return charMap;
    }
}
