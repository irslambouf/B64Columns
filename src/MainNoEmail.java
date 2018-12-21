import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

public class MainNoEmail {
    public static void main(String[] args){
        long start = System.nanoTime();
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
            MessageDigest sha256 = null;
            try{
                sha256 = MessageDigest.getInstance("SHA-256");
            }catch (NoSuchAlgorithmException e){
                e.printStackTrace();
            }
            byte[] line;
            int count = 0;
            while ((line = reader.readLine()) != null){
                count++;
                UtilBytes uLine = new UtilBytes(line);

                ArrayList<Integer> indexes = uLine.getAllIndexes(delimiter);

                for (int i=0; i< indexes.size(); i++){
                    // Email & password
                    if (i==0 || i==1){
                        if (i ==0){
                            fos.write(encoder.encode(uLine.getBytesOfRangeLower(0, indexes.get(i))));                        // B64 email
                            fos.write("\t".getBytes());

                            if (sha256 != null){
                                fos.write(bytesToHex(sha256.digest(uLine.getBytesOfRangeLower(0, indexes.get(i)))).getBytes());  // sha256
                            }
                        }

                        if (i==1){
                            UtilBytes uPassword = new UtilBytes(uLine.getBytesOfRange(indexes.get(i-1)+delimiter.length, indexes.get(i)));

                            fos.write(encoder.encode(uPassword.getData()));
                            fos.write("\t".getBytes());

                            if(sha256 != null){
                                fos.write(bytesToHex(sha256.digest(uPassword.getData())).getBytes());
                            }
                        }
                    }else {
                        //fos.write(uLine.getBytesOfRange(indexes.get(i-1)+delimiter.length, indexes.get(i)));
                        fos.write(encoder.encode(uLine.getBytesOfRange(indexes.get(i-1)+delimiter.length, indexes.get(i))));
                    }

                    fos.write(delimiter);
                }

                if (indexes.size() == 3){
                    fos.write(uLine.getBytesOfRange(indexes.get(indexes.size()-1)+delimiter.length, uLine.size()-1));   // only hash
                }else {
                    fos.write(encoder.encode(uLine.getBytesOfRange(indexes.get(indexes.size()-1)+delimiter.length, uLine.size()-1)));
                    fos.write("\t".getBytes());
                    if(sha256 != null){
                        fos.write(bytesToHex(sha256.digest(uLine.getBytesOfRange(indexes.get(indexes.size()-1)+delimiter.length, uLine.size()-1))).getBytes());
                    }
                }

                fos.write("\n".getBytes());
                fos.flush();

                if (count % 100000 == 0){
                    System.out.println(count);
                }
            }
        } catch (IOException e) {
            System.err.println("Output file can't be written to, exiting...");
        }

        System.out.println(System.nanoTime() - start);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
