package com.wxmlabs.bloodstained;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

// default save path '%USERPROFILE%\AppData\Local\BloodstainedRotN\Saved\SaveGames'
public class BloodstainedSave {
    private static final byte KEY_IV = 0x3A;
    private static final int DIGEST_LEN = 16;
    private static final String DIGEST_ALGORITHM = "MD5";
    private static final byte[] Begin_of_GVAS = new byte[]{0x47, 0x56, 0x41, 0x53, 0x02, 0x00, 0x00, 0x00}; // GVAS
    static final byte[] End_of_None = new byte[]{0x4e, 0x6f, 0x6e, 0x65, 0x00}; // None

    private boolean rotN = false;
    private byte[] content;
    private byte[] digest = new byte[DIGEST_LEN];

    public void load(InputStream input) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream output = new ByteArrayOutputStream(input.available());
        while (input.available() > 0) {
            int read = input.read(buffer);
            output.write(buffer, 0, read);
        }
        byte[] rawContent = output.toByteArray();
        content = new byte[rawContent.length - DIGEST_LEN];
        System.arraycopy(rawContent, 0, content, 0, content.length);
        System.arraycopy(rawContent, content.length, digest, 0, digest.length);
        try {
            MessageDigest md5 = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] sumDigest = md5.digest(content);
            if (!Arrays.equals(digest, sumDigest)) {
                throw new IOException(String.format("invalid digest. needs %s, but %s", hex(sumDigest), hex(digest)));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (content[0] != Begin_of_GVAS[0]
            && content[1] != Begin_of_GVAS[1]
            && content[2] != Begin_of_GVAS[2]
            && content[3] != Begin_of_GVAS[3]
            && content[4] != Begin_of_GVAS[4]
            && content[5] != Begin_of_GVAS[5]
            && content[6] != Begin_of_GVAS[6]
            && content[7] != Begin_of_GVAS[7]
        ) {
            rotN = true;
            content = rotN();
        }
    }

    public void store(OutputStream outputStream) throws IOException {
        byte[] content;
        if (rotN) {
            content = rotN();
        } else {
            content = this.content;
        }
        try {
            DigestOutputStream outStream = new DigestOutputStream(outputStream, MessageDigest.getInstance(DIGEST_ALGORITHM));
            outStream.write(content);
            outStream.on(false);
            digest = outStream.getMessageDigest().digest();
            outStream.write(digest);
            outStream.flush();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    int indexOf(final byte[] tag, int fromPos) {
        int matchedCount = 0;
        if (fromPos < 0) {
            fromPos = 0;
        }
        for (int i = fromPos; i < content.length; i++) {
            byte b = content[i];
            if (b == tag[matchedCount]) { // matched
                if (++matchedCount == tag.length) { // match full
                    return i - (matchedCount - 1);
                }
            } else {
                matchedCount = 0;
            }
        }
        return -1;
    }

    private byte[] rotN() {
        int key = KEY_IV;
        byte[] rotedBytes = new byte[content.length];
        for (int i = 0; i < content.length; i++) {
            rotedBytes[i] = (byte) ((content[i] & 0xFF) ^ key);
            if (++key == 0xFF) key = 0;
        }
        return rotedBytes;
    }

    BloodstainedDlc getDlc() {
        return loadObject(BloodstainedDlc.class);
    }

    void setDlc(BloodstainedDlc dlc) {
        storeObject(dlc);
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends BloodstainedObject> T loadObject(Class<T> tClass) {
        try {
            T t = tClass.newInstance();
            byte[] bTag = t.getBeginTag();
            byte[] eTag = t.getEndTag();
            int bIdx = indexOf(bTag, 0x30a);
            int eIdx = indexOf(eTag, bIdx);
            if (bIdx > 0 && eIdx > 0) {
                int tContentLength = eIdx - bIdx - bTag.length;
                byte[] tContent = new byte[tContentLength];
                System.arraycopy(content, bIdx + bTag.length, tContent, 0, tContentLength);
                t.setContent(tContent);
                return t;
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T extends BloodstainedObject> void storeObject(T t) {
        byte[] bTag = t.getBeginTag();
        byte[] eTag = t.getEndTag();
        int bIdx = indexOf(bTag, 0x30a);
        int eIdx = indexOf(eTag, 0x30a + bIdx);
        byte[] tContent = t.getContent();
        ByteArrayOutputStream newContent = new ByteArrayOutputStream(content.length);
        newContent.write(content, 0, bIdx);
        newContent.write(bTag, 0, bTag.length);
        newContent.write(tContent, 0, tContent.length);
        newContent.write(eTag, 0, eTag.length);
        newContent.write(content, eIdx + eTag.length, content.length - (eIdx + eTag.length));
        content = newContent.toByteArray();
    }
//    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
//        String needsFixSav = "Story_Slot0.sav";
//        String localValidSav = "Story_Slot1.sav";
//
//        MessageDigest md5 = MessageDigest.getInstance("MD5");
//        Provider p = md5.getProvider();
//        System.out.println(p.getService("MessageDigest", "MD5"));
//        File needsFixRotN = new File(needsFixSav);
//        File needsFixFile = rotN(needsFixRotN);
//        System.out.println("needsFixFile " + checkSaveFile(needsFixRotN));
//
//        File localValidRotN = new File(localValidSav);
//        File localValidFile = rotN(localValidRotN);
//        System.out.println("localValidFile " + checkSaveFile(localValidFile));
//
//        File fixedFile = hackDlc(localValidFile, needsFixFile);
//        System.out.println("fixedFile " + checkSaveFile(fixedFile));
//        File fixedRotN = rotN(fixedFile);
//        System.out.println(fixedRotN);
//    }

//    private static boolean checkSaveFile(File file) throws IOException, NoSuchAlgorithmException {
//        try (FileInputStream inStream = new FileInputStream(file)) {
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            ByteArrayOutputStream buffer = new ByteArrayOutputStream(inStream.available());
//            while (inStream.available() > DIGEST_LEN) {
//                buffer.write(inStream.read());
//            }
//            byte[] digest = md5.digest(buffer.toByteArray());
//            byte[] originDigest = new byte[DIGEST_LEN];
//            int read = inStream.read(originDigest);
//            System.out.println("buffer " + buffer.size());
//            System.out.println("read " + read);
//            System.out.println(hex(originDigest));
//            System.out.println(hex(digest));
//            return Arrays.equals(originDigest, md5.digest());
//        }
//    }

//    private static File hackDlc(File validTpl, File needsFix) throws IOException, NoSuchAlgorithmException {
//        byte[] begin = new byte[]{0x48, 0x61, 0x73, 0x44, 0x4c, 0x43, 0x73, 0x00}; // HasDLCs
//        byte[] end = new byte[]{0x4e, 0x6f, 0x6e, 0x65, 0x00}; // None
//        // find and get copy content
//        byte[] copyContent;
//        try (FileInputStream inStream = new FileInputStream(validTpl); ByteArrayOutputStream outStream = new ByteArrayOutputStream(0x0f0)) {
//            //noinspection ResultOfMethodCallIgnored
//            inStream.skip(0x30a);
//            readToMatch(inStream, begin, null);
//            readToMatch(inStream, end, outStream);
//            outStream.flush();
//            copyContent = outStream.toByteArray();
//        }
//
//        File fixedFile = new File(needsFix.getParentFile(), "Fixed_" + needsFix.getName());
//        try (FileInputStream inStream = new FileInputStream(needsFix); DigestOutputStream outStream = new DigestOutputStream(new FileOutputStream(fixedFile), MessageDigest.getInstance("MD5"))) {
//            outStream.on(true);
//            readToMatch(inStream, begin, outStream);
//            outStream.write(copyContent);
//            readToMatch(inStream, end, null);
//            while (inStream.available() > DIGEST_LEN) {
//                byte b = (byte) inStream.read();
//                outStream.write(b);
//            }
//            outStream.on(false);
//            outStream.flush();
//            byte[] digest = outStream.getMessageDigest().digest();
//            outStream.write(digest);
//            outStream.flush();
//        }
//        return fixedFile;
//    }

//    private static File rotNFile(File inFile) throws IOException {
//        File outFile = new File(inFile.getParentFile(), "RotN_" + inFile.getName());
//        try (FileInputStream inStream = new FileInputStream(inFile); FileOutputStream outStream = new FileOutputStream(outFile)) {
//            int key = KEY_IV;
//            while (inStream.available() > 0) {
//                outStream.write((inStream.read() & 0xFF) ^ key);
//                if (++key == 0xFF) key = 0;
//            }
//            outStream.flush();
//        }
//        return outFile;
//    }

//    static void readToMatch(InputStream inStream, final byte[] match, OutputStream outStream) throws IOException {
//        int matchedIndex = 0;
//        while (inStream.available() > 0) {
//            byte b = (byte) inStream.read();
//            if (outStream != null) {
//                outStream.write(b);
//            }
//            if (b == match[matchedIndex]) { // matched
//                matchedIndex++;
//                if (matchedIndex == match.length) { // match full
//                    return;
//                }
//            } else {
//                matchedIndex = 0;
//            }
//        }
//    }

    private final static String HEX_DICT = "0123456789abcdef";

    private static String hex(byte[] data) {
        StringBuilder hex = new StringBuilder();
        for (int d : data) {
            hex.append(HEX_DICT.charAt(d >> 4 & 0x0f));
            hex.append(HEX_DICT.charAt(d & 0x0f));
        }
        return hex.toString();
    }
}
