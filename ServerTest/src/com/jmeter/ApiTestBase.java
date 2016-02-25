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
public class ApiTestBase extends AbstractJavaSamplerClient {


    public Logger log=getLogger();

    public SampleResult results;

    public String host;

    public String port;

//    private String Data;

    public String RequestTimeout;

    public String ResponseTimeout;

    public String path;


    //初始化方法
    public void setupTest(JavaSamplerContext context){

        listParameters(context);

    }


   // Jmeter 使用多线程测试时，需要加入循环使用参数的方法

    /**

     * 该函数主要功能是循环获取参数列表中的数据

     * 没有这个函数，工具无法实现对参数的循环调用

     * @param context

     */
    public void listParameters(JavaSamplerContext context) {

        String name;

        for (Iterator argsIt = context.getParameterNamesIterator();

             argsIt.hasNext();

             log.info("--循环取参(加密前): "+( new StringBuilder()).append(name).append( "=" ).append(context.getParameter(name)).toString()))

            name= (String) argsIt.next();

    }

    /**

     * 将 jmeter 参数界面传入的参数赋值给参数

     * @param context

     */

    public void setupValues(JavaSamplerContext context) {

        host = context.getParameter( "host","").trim();

        port =context.getParameter( "port","").trim();

        path =context.getParameter("path","").trim();

        RequestTimeout = context.getParameter( "RequestTimeout","").trim();

        ResponseTimeout = context.getParameter( "ResponseTimeout","").trim();

//        Data = context.getParameter( "Data", "");

    }


// 设置 jmeter 中的参数列表

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

    /**

     * 测试执行的循环体，根据线程数和循环次数的不同可执行多次，类似于 LoadRunner 中的 Action 方法

     * @param javaSamplerContext

     */

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
//
//定义一个事务，表示这是事务的起始点，类似于 LoadRunner 的 lr.start_transaction
//
//    results=new SampleResult();
//
//       if(h || port==null || path==null){
//
//            log.info("---host,port及path 不能为空");
//
//           results.setSuccessful(false);
//
//        }
//
//
//
    return null;
  }
}
