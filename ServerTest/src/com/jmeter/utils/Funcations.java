package com.jmeter.utils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by zhouzhangyin on 16/2/3.
 */
public class Funcations {


    public static String encryptThreeDESECB(String src,String key)
    {
        try{
            DESedeKeySpec dks = new DESedeKeySpec(key.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);

            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey);
            byte[] b=cipher.doFinal(src.getBytes());

            BASE64Encoder encoder = new BASE64Encoder();
            return encoder.encode(b).replaceAll("\r", "").replaceAll("\n", "");
        }catch (Exception e){
            e.printStackTrace();

            return src;
        }


    }


    //3DESECB解密,key必须是长度大于等于 3*8 = 24 位
    public static String decryptThreeDESECB(String src,String key)
    {

        try{
            //--通过base64,将字符串转成byte数组
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] bytesrc = decoder.decodeBuffer(src);
            //--解密的key
            DESedeKeySpec dks = new DESedeKeySpec(key.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey securekey = keyFactory.generateSecret(dks);

            //--Chipher对象解密
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, securekey);
            byte[] retByte = cipher.doFinal(bytesrc);

            return new String(retByte);
        }catch (Exception e){
            e.printStackTrace();
            return src;
        }

    }




    /**
     * byte数组转换成16进制字符数组
     * @param src
     * @return
     */
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 从文件中获取字符数组
     * @param filePath
     * @return
     */

    public  byte[] getBytesFromFile(String filePath) {
        byte[] ret = null;
        File file = new File(filePath);
        try {
            if (file  == null) {
                // log.error("helper:the file is null!");
                return null;
            }
            FileInputStream in = new FileInputStream(file);

            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

            byte[] b = new byte[4096];
            int n;

            while ((n = in.read(b)) != -1) {

                out.write(b, 0, n);
            }
            in.close();

            out.close();

            ret = out.toByteArray();

        } catch (IOException e) {
            // log.error("helper:get bytes from file process error!");
            e.printStackTrace();
        }
        return ret;
    }













}
