package com.wxmlabs.bloodstained;

import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BloodstainedRotN {
    private final static byte KEY_IV = 0x3A;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String needsFixSav = "Story_Slot0.sav";
        String localValidSav = "Story_Slot1.sav";

        File needsFixRotN = new File(needsFixSav);
        File needsFixFile = rotN(needsFixRotN);

        File localValidRotN = new File(localValidSav);
        File localValidFile = rotN(localValidRotN);

        File fixedFile = hackDlc(localValidFile, needsFixFile);
        File fixedRotN = rotN(fixedFile);
        System.out.println(fixedRotN);
    }

    private static File hackDlc(File validTpl, File needsFix) throws IOException, NoSuchAlgorithmException {
        byte[] begin = new byte[]{0x48, 0x61, 0x73, 0x44, 0x4c, 0x43, 0x73, 0x00}; // HasDLCs
        byte[] end = new byte[]{0x4e, 0x6f, 0x6e, 0x65, 0x00}; // None
        // find and get copy content
        byte[] copyContent;
        try (FileInputStream inStream = new FileInputStream(validTpl); ByteArrayOutputStream outStream = new ByteArrayOutputStream(0x0f0)) {
            //noinspection ResultOfMethodCallIgnored
            inStream.skip(0x30a);
            readToMatch(inStream, begin, null);
            readToMatch(inStream, end, outStream);
            outStream.flush();
            copyContent = outStream.toByteArray();
        }

        File fixedFile = new File(needsFix.getParentFile(), "Fixed_" + needsFix.getName());
        try (FileInputStream inStream = new FileInputStream(needsFix); DigestOutputStream outStream = new DigestOutputStream(new FileOutputStream(fixedFile), MessageDigest.getInstance("MD5"))) {
            outStream.on(true);
            readToMatch(inStream, begin, outStream);
            outStream.write(copyContent);
            readToMatch(inStream, end, null);
            while (inStream.available() > 16) {
                byte b = (byte) inStream.read();
                outStream.write(b);
            }
            outStream.on(false);
            outStream.flush();
            byte[] digest = outStream.getMessageDigest().digest();
            outStream.write(digest);
            outStream.flush();
        }
        return fixedFile;
    }

    private static File rotN(File inFile) throws IOException {
        File outFile = new File(inFile.getParentFile(), "RotN_" + inFile.getName());
        try (FileInputStream inStream = new FileInputStream(inFile); FileOutputStream outStream = new FileOutputStream(outFile)) {
            int key = KEY_IV;
            while (inStream.available() > 0) {
                outStream.write((inStream.read() & 0xFF) ^ key);
                if (++key == 0xFF) key = 0;
            }
            outStream.flush();
        }
        return outFile;
    }

    static void readToMatch(InputStream inStream, final byte[] match, OutputStream outStream) throws IOException {
        int matchedIndex = 0;
        while (inStream.available() > 0) {
            byte b = (byte) inStream.read();
            if (outStream != null) {
                outStream.write(b);
            }
            if (b == match[matchedIndex]) { // matched
                matchedIndex++;
                if (matchedIndex == match.length) { // match full
                    return;
                }
            } else {
                matchedIndex = 0;
            }
        }
    }
}
