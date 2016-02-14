package com.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Logger;

import java.util.Iterator;

/**
 * Created by zhouzhangyin on 16/2/2.
 */
public class ApiTestUtil extends AbstractJavaSamplerClient {


    public Logger log=getLogger();

    public SampleResult results;

    public String host;

    public String port;

//    private String Data;

    public String RequestTimeout;

    public String ResponseTimeout;

    public String path;


    public void setupTest(JavaSamplerContext context){

        listParameters(context);

    }



    public void listParameters(JavaSamplerContext context) {

        String name;

        for (Iterator argsIt = context.getParameterNamesIterator();

             argsIt.hasNext();

             log.debug(( new StringBuilder()).append(name).append( "=" ).append(context.getParameter(name)).toString()))

            name= (String) argsIt.next();

    }



    public void setupValues(JavaSamplerContext context) {

        host = context.getParameter( "host","");

        port =context.getParameter( "port","");

        path =context.getParameter("path","");

        RequestTimeout = context.getParameter( "RequestTimeout","");

        ResponseTimeout = context.getParameter( "ResponseTimeout","");

//        Data = context.getParameter( "Data", "");

    }




    public Arguments getDefaultParameters(){


        log.info("execute getDefaultParameters...");

        Arguments params= new Arguments();

        params.addArgument("host", "");

        params.addArgument("port","");

        params.addArgument("path","");

        params.addArgument("RequestTimeout","");

        params.addArgument("ResponseTimeout","");

//        params.addArgument("Data","");


        return params;


    }





    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {



        results=new SampleResult();

        if(host==null || port==null || path==null){

            log.info("---host,port及path 不能为空");

            results.setSuccessful(false);

        }



        return results;
    }
}
