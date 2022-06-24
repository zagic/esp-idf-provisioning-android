package com.espressif.certificateLoader;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import com.espressif.AppConstants;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class certificateLoader {

    public enum deviceType{
        IOTCORE_DEMO;
    }
    private static final String SUFFIX_CERT_ID = "_cert_id_file";
    private static final String SUFFIX_CERT_PEM = "_cert_pem_file";
    private static final String SUFFIX_PRIVATE_KEY = "_private_key_pem_file";

    public static List<CertificateGroup> getCertificateGroups(){
        verifyFolder();
        File file = new File(AppConstants.APP_PATH);
        File[] subFile = file.listFiles();
        Map<String,CertificateGroup> CertificateList = new HashMap<>();

        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                if (filename.trim().endsWith(SUFFIX_CERT_ID)) {
                    String tmpName = StringUtils.substringBeforeLast(filename,SUFFIX_CERT_ID);
                    if(!CertificateList.containsKey(tmpName)){
                        CertificateGroup tmp = new  CertificateGroup();
                        tmp.DeviceName =tmpName;
                        tmp.hasId =true;
                        CertificateList.put(tmpName,tmp);
                    }else{
                        CertificateList.get(tmpName).hasId =true;
                    }
                }else if (filename.trim().endsWith(SUFFIX_CERT_PEM)) {
                    String tmpName = StringUtils.substringBeforeLast(filename,SUFFIX_CERT_PEM);
                    if(!CertificateList.containsKey(tmpName)){
                        CertificateGroup tmp = new  CertificateGroup();
                        tmp.DeviceName =tmpName;
                        tmp.hasCertificatePem =true;
                        CertificateList.put(tmpName,tmp);
                    }else{
                        CertificateList.get(tmpName).hasCertificatePem =true;
                    }
                }else if (filename.trim().endsWith(SUFFIX_PRIVATE_KEY)) {
                    String tmpName = StringUtils.substringBeforeLast(filename,SUFFIX_PRIVATE_KEY);
                    if(!CertificateList.containsKey(tmpName)){
                        CertificateGroup tmp = new  CertificateGroup();
                        tmp.DeviceName =tmpName;
                        tmp.hasPrivateKey =true;
                        CertificateList.put(tmpName,tmp);
                    }else{
                        CertificateList.get(tmpName).hasPrivateKey =true;
                    }
                }

            }
        }
        return  new ArrayList<>(CertificateList.values());
    }

    public static void verifyFolder(){

        try {
            // Create the root dir to store dbs
            File rootDirFile = new File(AppConstants.APP_PATH);
            if (!rootDirFile.exists() && !rootDirFile.mkdirs()) {
                throw new RuntimeException("Failed to create directory structure for " + AppConstants.APP_PATH);
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    public static String loadCertId(String name)  {
        String fileName = name+SUFFIX_CERT_ID;
        File tempFile = new File(AppConstants.APP_PATH+fileName);
        if (tempFile.isDirectory())
        {
            Log.d("TestTest", "Cert ID doesn't not exist.");
            return null;
        }
        try (FileReader fr = new FileReader(tempFile))
        {
            char[] chars = new char[(int) tempFile.length()];
            fr.read(chars);

            String fileContent = new String(chars);
            return fileContent;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String loadCertPem(String name)  {
        String fileName = name+SUFFIX_CERT_PEM;
        File tempFile = new File(AppConstants.APP_PATH+fileName);
        if (tempFile.isDirectory())
        {
            Log.d("TestTest", "Cert Pem doesn't not exist.");
            return null;
        }
        try (FileReader fr = new FileReader(tempFile))
        {
            char[] chars = new char[(int) tempFile.length()];
            fr.read(chars);

            String fileContent = new String(chars);
            return fileContent;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String loadPrivateKey(String name)  {
        String fileName = name+SUFFIX_PRIVATE_KEY;
        File tempFile = new File(AppConstants.APP_PATH+fileName);
        if (tempFile.isDirectory())
        {
            Log.d("TestTest", "Private key doesn't not exist.");
            return null;
        }
        try (FileReader fr = new FileReader(tempFile))
        {
            char[] chars = new char[(int) tempFile.length()];
            fr.read(chars);

            String fileContent = new String(chars);
            return fileContent;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static class CertificateGroup{
        public String DeviceName;
        public deviceType type;
        public boolean hasId = false;
        public boolean hasCertificatePem = false;
        public boolean hasPrivateKey = false;
    }
}
