package com.jmeter;

/**
 * Created by zhouzhangyin on 16/2/3.
 */


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.jmeter.utils.Funcations.decryptThreeDESECB;

/**
 * HTTP请求对象
 *
 * @author OUWCH
 */
public class HttpRequester {

    public static HttpClient httpclient;

    public static HttpPost httppost;

    static List params = new ArrayList<BasicNameValuePair>();

    static UrlEncodedFormEntity uef;

    public HttpRequester(String url) {

        httpclient = new DefaultHttpClient();

        httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,TestConstants.Default_Connection_TimeOut);//连接超时时间

        httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT,TestConstants.Default_So_TimeOut);//数据传输超时时间

        httppost=new HttpPost("http://"+url);
    }


    /**
     *
     * 发起post请求
     * @return
     */

   public static String sendPost(){




       try {

           uef=new UrlEncodedFormEntity(params, HTTP.UTF_8);

           httppost.setEntity(uef);
           // 设置参数给Post
       } catch (UnsupportedEncodingException e) {

           e.printStackTrace();
       }



        // 执行
       HttpResponse response = null;

       try {
           response = httpclient.execute(httppost);

           HttpEntity entity = response.getEntity();

          // 显示结果
           BufferedReader reader = null;

           reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));

           char[] b = new char[1024];

           int length =reader.read(b);

           String resp=new String(b,0,length);

           resp= decryptThreeDESECB(resp,TestConstants.KEY);

           return resp;

       } catch (Exception e) {

           e.printStackTrace();
       }
          return null;
   }


    /**
     * 添加请求参数
     * @param key
     * @param value
     * @return
     */
    public static List<BasicNameValuePair> addParams(String key,String value) {

        params.add(new BasicNameValuePair(key,value));

        return params;
    }








}