package com.ian.huffmantools;

import java.io.IOException;

/**
 * Created by lep on 3/16/19.
 */
public class HuffmanTools {
    public static void main(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            System.out.println("Bad parameters. exit.");
            return;
        }

        long start = System.currentTimeMillis();
        if(args[0].equalsIgnoreCase("encode"))
        {
            Facade facade = new Facade(args[1], args[2]);
            facade.compress();
        }
        else if(args[0].equalsIgnoreCase("decode"))
        {
            Facade facade = new Facade(args[1], args[2]);
            facade.extract();
        }
        else{
        	System.out.println("Found invalid command. exit.");
        }
        long end = System.currentTimeMillis();
        System.out.println("This process lasts ：" + (end - start) / 1000.0 + "seconds");
        return;
    }
}
