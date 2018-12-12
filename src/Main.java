import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Base64;

public class Main {
    public static void main(String[] args){
        if (args.length < 3){
            System.err.println("Usage:java -jar B64Columns [delimiter] [in filename] [out filename]");
            return;
        }

        byte[] delimiter = StringEscapeUtils.unescapeJava(args[0]).getBytes();

        if (delimiter.length == 0){
            System.err.println("Delimiter length is 0, exiting...");
            return;
        }

        File in = new File(args[1]);
        if (!in.exists()){
            System.err.println("Input file does not exist, exiting...");
            return;
        }

        File out = new File(args[2]);
        if (out.exists()){
            Scanner scanner = new Scanner(System.in);
            String input = null;
            do {
                System.out.print("Output file already exists, do you want to override (Y/N)? ");
                input = scanner.next();
            } while (input == null || !(input.equals("Y") || input.equals("N")));

            if (input.equalsIgnoreCase("N")) {
                System.out.println("File will not be overwritten, exiting...");
                return;
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(out);
            FileAndFolderReaderBinaryV2 reader = new FileAndFolderReaderBinaryV2(args[1]);
            Base64.Encoder encoder = Base64.getEncoder();
            byte[] line;
            while ((line = reader.readLine()) != null){
                UtilBytes uLine = new UtilBytes(line);

                ArrayList<Integer> indexes = uLine.getAllIndexes(delimiter);

                for (int i=0; i< indexes.size(); i++){
                    if (i==0){
                        byte[] email = uLine.getBytesOfRangeLower(0, indexes.get(i));

                        String sEmail = new String(email);

                        int atIndex = sEmail.indexOf('@');

                        fos.write(sEmail.substring(0, atIndex).getBytes());
                        fos.write("\t".getBytes());
                        fos.write(sEmail.substring(atIndex+1, sEmail.length()).getBytes());
                        fos.write("\t".getBytes());

                        fos.write(encoder.encode(email));
                    }else {

                        fos.write(encoder.encode(uLine.getBytesOfRange(indexes.get(i-1)+delimiter.length, indexes.get(i))));


                    }

                    fos.write(delimiter);
                }
                //fos.write(new String(uLine.getBytesOfRange(indexes.get(indexes.size()-1)+delimiter.length, uLine.size()-1)).getBytes());

                fos.write(encoder.encode(uLine.getBytesOfRange(indexes.get(indexes.size()-1)+delimiter.length, uLine.size()-1)));
                fos.write("\n".getBytes());
                fos.flush();
            }
        } catch (IOException e) {
            System.err.println("Output file can't be written to, exiting...");
        }
    }
}
