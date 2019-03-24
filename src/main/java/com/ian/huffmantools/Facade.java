package com.ian.huffmantools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Created by lep on 3/16/19.
 */
public class Facade{
    private String srcFilename;
    private String dstFilename;
    private long fileLength;
    private HashMap<Character, Integer> keyMap = null;
    private HashMap<Character, String> char2codeMap = null;
    private Vector<Object> listThreads = null;
    private Vector<Thread> listThread = null;
    private CountDownLatch countDownLatch;

    public Facade(String src, String dst) {
        this.srcFilename = src;
        this.dstFilename = dst;
        this.keyMap = new HashMap<Character, Integer>();
        this.char2codeMap = new HashMap<Character, String>();
        this.listThreads = new Vector<Object>();
        this.listThread = new Vector<Thread>();   
    }

    private int getFrequency() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(srcFilename + ".huf")),"UTF8"));

        int keyCounts = Integer.parseInt(in.readLine());
        char key;
        String split = ":";
        if (keyCounts == 1)
        {
            key = in.readLine().charAt(0);
            fileLength = keyCounts;
            keyMap.put(key, keyCounts);
        }
        else
        {
            for (int i = 0; i < keyCounts; i++){
                String line = in.readLine();

                if (line.length()< 2)
                {
                    //Newline character. frequency is in next line.
                    key = 10;
                    line = in.readLine();
                    String[] str = line.split(split);
                    keyMap.put(key, Integer.parseInt(str[1]));
                }
                else {
                    String[] str = line.split(split);
                    if (str.length > 2)
                    {
                        //this line contains split.
                        key = split.charAt(0);
                        keyMap.put(key, Integer.parseInt(str[2]));

                    }
                    else {
                        key = str[0].charAt(0);
                        keyMap.put(key, Integer.parseInt(str[1]));
                    }
                }
            }
            fileLength = Integer.parseInt(in.readLine());
        }

        return keyCounts;
    }

    private int getFrequency(File file) throws IOException {
        this.fileLength = file.length();
        long currentPos = 0;
        int cpuNum =  Runtime.getRuntime().availableProcessors();
        long splitSize = fileLength/cpuNum + fileLength%cpuNum;
        this.countDownLatch = new CountDownLatch(cpuNum);
        int keySize = 0;

        if (splitSize > 1024*1024*1024)
            splitSize = 1024*1024*1024;

        while (currentPos < fileLength)
        {
            CountKeysThread countKeysThread = null;
            if (currentPos + splitSize < fileLength)
            {
                RandomAccessFile raf = new RandomAccessFile(file,"rw");
                raf.seek(currentPos + splitSize);
                countKeysThread = new CountKeysThread(file, currentPos, splitSize, countDownLatch);
                currentPos += splitSize;
                raf.close();
            }
            else{
                countKeysThread = new CountKeysThread(file, currentPos, fileLength - currentPos, countDownLatch);
                currentPos = fileLength;
            }

            Thread thread = new Thread(countKeysThread);
            thread.start();
            listThreads.add(countKeysThread);
            listThread.add(thread);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MergeFrequency task = new MergeFrequency();
        FutureTask<HashMap<Character, Integer> > futureTask = new FutureTask<HashMap<Character, Integer>>(task);
        Thread thread = new Thread(futureTask);
        thread.start();

        try {
            keySize = futureTask.get().keySet().size();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return keySize;
    }

    private void writeCode2File() throws IOException {
        int keyCounts = keyMap.keySet().size();
        File decFile = new File(dstFilename+".huf");
        if(decFile.exists())
            decFile.delete();

        FileOutputStream fos  = new FileOutputStream(decFile,true);
        Iterator<Character> it = keyMap.keySet().iterator();
        OutputStreamWriter osw =  new OutputStreamWriter(fos,
                StandardCharsets.UTF_8);
        osw.write(String.valueOf(keyCounts) + "\n");
        String str  =  "";

        if (keyCounts == 1)
        {
            char key = (char) it.next();
            osw.write(key);
            osw.close();
            fos.close();
            return;
        }
        else
        {
            while(it.hasNext()){
                char key = (char) it.next();
                try {
                    str = String.valueOf(key) + ":"+ String.valueOf(keyMap.get(key)) + "\n";
                    osw.write(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            osw.write(Long.toString(fileLength));
            osw.close();
            fos.close();
        }

        String code_buffer ="";
        String current_code = "";
        RandomAccessFile raf = new RandomAccessFile(srcFilename, "rw");
        FileChannel fc = raf.getChannel();
        MappedByteBuffer mbBuf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        fos  = new FileOutputStream(dstFilename,true);
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(mbBuf);

        while (charBuffer.hasRemaining())
        {
            char key = (char)charBuffer.get();
            String code = char2codeMap.get(key);
            current_code = code_buffer+code;

            int len = current_code.length();
            int mul = len-len%8;

            if(len<8)
                code_buffer = current_code;
            else{
                int start_index=0;
                int end_index =8;
                while(end_index <= mul){
                    byte binary = (byte)Integer.parseInt(current_code.substring(start_index, end_index),2);
                    start_index=end_index;
                    end_index+=8;

                    fos.write(binary);
                }
                code_buffer = current_code.substring(mul, len);
            }
        }

        fc.close();
        raf.close();

		int tailLen = code_buffer.length();
		if (tailLen > 0)
		{   
            byte temp_code = 0;
            byte[] byteArray = code_buffer.getBytes();
            for(int i = 0; i < tailLen; i++)                                                                                                                                                                                    
            {   
                temp_code <<= 1;
                if(byteArray[i] == '1')
                    temp_code |= 1;
            }   
            temp_code <<= 8 - tailLen;
            fos.write(temp_code);
		}   

        fos.close();
    }

    public void compress() throws IOException {
        File srcFile = new File(srcFilename);
        File dstFile = new File(dstFilename);
        if (!srcFile.exists())
        {
            System.out.println("The file to be compress not exist. exit !!");
            return;
        }

        if (dstFile.exists())
        {
            dstFile.delete();
        }

        System.out.println("Step 1. getFrequency.");
        if(0 == getFrequency(srcFile))
        {
            System.out.println("File input is empty.exit.");
            return;
        }

        System.out.println("Step 2. huffmanCoding.");
        huffmanCoding();

        System.out.println("Step 3. transfor file to huffman code.");
        writeCode2File();

        System.out.println("Huffman compress finish.");
    }

	private void huffmanCoding()
    {
        HuffmanTree tree = HuffmanCode.buildTree(keyMap);
        HuffmanCode.generateCodes(tree,  "", char2codeMap);
    }

    public void extract() throws IOException {
        File srcFile = new File(srcFilename);
        File dstFile = new File(dstFilename);

        if (!srcFile.exists())
        {
            System.out.println("The file to be extracted not exist. exit !!");
            return;
        }

        if (dstFile.exists())
        {
            dstFile.delete();
        }

        System.out.println("File extracting...");

        FileInputStream fio = new FileInputStream(srcFile);
        PrintStream ps = new PrintStream(new FileOutputStream(dstFile));
        if(1 == getFrequency())
        {
           char key = keyMap.keySet().iterator().next();
           for (int i = 0; i < fileLength; i++)
           {
               ps.print(key);
           }
        }
        else {
            HuffmanTree root = HuffmanCode.buildTree(keyMap);
            HuffmanCode.generateCodes(root,  "", char2codeMap);
            HuffmanTree tmp = root;
            byte[] buff = new byte[1];
            int writeLen = 0;
            boolean ending = true;

            while (ending && (fio.read(buff) != -1)) {
                String binary_str = String.format("%8s", Integer.toBinaryString(buff[0] & 0xFF)).replace(' ', '0');
                if (binary_str.length() != 8) {
                    int extra_len = 8 - binary_str.length();
                    String padding_bits = "";
                    while (extra_len > 0) {
                        padding_bits += "0";
                        extra_len--;
                    }
                    binary_str = padding_bits + binary_str;
                }

                for (int i = 0; i < binary_str.length(); i++) {
                    if (tmp instanceof HuffmanNode) {
                        HuffmanNode node = (HuffmanNode) tmp;
                        if (binary_str.charAt(i) == '0')
                            tmp = node.left;
                        else
                            tmp = node.right;
                    }

                    if (tmp instanceof HuffmanLeaf) {
                        HuffmanLeaf node = (HuffmanLeaf) tmp;
                        ps.print(node.value);
                        tmp = (HuffmanNode) root;

                        writeLen++;
                    }

                    if (writeLen == fileLength)
                    {
                        ending = false;
                        break;
                    }
                }
            }

        }

        fio.close();
        ps.close();
    }

    class MergeFrequency implements Callable<HashMap<Character, Integer>> {
        @Override
        public HashMap<Character, Integer> call() throws Exception {
            for (int loop = 0; loop < listThreads.size(); loop++)
            {
                CountKeysThread thread = (CountKeysThread)listThreads.get(loop);
                Map<Character, Integer> hMap = thread.GetResultMap();

                Set<Character> keys = hMap.keySet();
                Iterator<Character> iterator = keys.iterator();
                while (iterator.hasNext())
                {
                    char key = (Character) iterator.next();

                    if (keyMap.get(key) == null)
                    {
                        keyMap.put(key, hMap.get(key));
                    }
                    else
                    {
                        keyMap.put(key, keyMap.get(key) + hMap.get(key));
                    }
                }
            }

            return keyMap;
        }
    }
}
