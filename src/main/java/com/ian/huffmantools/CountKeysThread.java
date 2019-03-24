package com.ian.huffmantools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

//线程类
public class CountKeysThread implements Runnable
{
    private FileChannel fileChannel = null;
    private FileLock lock = null;
    private MappedByteBuffer mbBuf = null;
    private Map<Character, Integer> hashMap = null;
    private CountDownLatch countDownLatch;

    @SuppressWarnings("resource")
	public CountKeysThread(File file, long start, long size, CountDownLatch countDownLatch) //文件，起始位置，映射文件大小
    {
        this.countDownLatch = countDownLatch;

        try
        {
            // 得到当前文件的通道
            fileChannel = new RandomAccessFile(file, "rw").getChannel();
            // 锁定当前文件的部分
            lock = fileChannel.lock(start, size, false);
            // 对当前文件片段建立内存映射，如果文件过大需要切割成多个片段
            mbBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, start, size);
            // 创建HashMap实例存放处理结果
            hashMap = new HashMap<Character, Integer>();
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    //重写run()方法
    @Override
    public void run() 
    {
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(mbBuf);
        while (charBuffer.hasRemaining())
        {
            char c = (char)charBuffer.get();
            if (hashMap.get(c) == null)
            {
                hashMap.put(c,  1);
            }
            else
            {
                hashMap.put(c, hashMap.get(c) + 1);
            }
        }

        try
        {
            // 释放文件锁
            lock.release();
            // 关闭文件通道
            fileChannel.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        this.countDownLatch.countDown();
    }

    //获取当前线程的执行结果
    public Map<Character, Integer> GetResultMap()
    {
        return hashMap;
    }
}
