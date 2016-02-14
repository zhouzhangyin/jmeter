package com.jmeter;

import com.jmeter.utils.EncryptionAndDecryption;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Logger;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by zhouzhangyin on 16/2/2.
 */
public class TestHttp extends ApiTestUtil  {

    private Logger log=getLogger();

//    private SampleResult results;

    private  String code;

    private  String platform;

    private  String app_v;

    private  String apiversion;

    private String data;




    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

    }


    @Override
    public void listParameters(JavaSamplerContext context) {
        super.listParameters(context);
    }




    @Override
    public Arguments getDefaultParameters() {

        Arguments params = super.getDefaultParameters();

        params.addArgument("apiversion","");

        params.addArgument("app_v","");

        params.addArgument("platform","");

        params.addArgument("code","");

        params.addArgument("data","");

        return params;
    }

    @Override
    public void setupValues(JavaSamplerContext context) {

        super.setupValues(context);

        code=context.getParameter("code");

        platform=context.getParameter("platform");

        app_v=context.getParameter("app_v");

        apiversion=context.getParameter("apiversion");

        data= EncryptionAndDecryption.encryptThreeDESECB(context.getParameter("data"),TestConstants.KEY);


    }


//    public Map getValues(){
//
//        m.put("host",host);
//        m.put("port",port);
//        m.put("path",path);
//        m.put("RequestTimeout",RequestTimeout);
//        m.put("ResponseTimeout",ResponseTimeout);
//        m.put("apiversion",apiversion);
//        m.put("code",code);
//        m.put("platform",platform);
//        m.put("app_v",app_v);
//        m.put("data",data);
//
//        return m;
//    }





    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        log.info("exec runTest...");

        results = new SampleResult();
        //发起事务
        results.sampleStart();

        setupValues(context);

        if(host.trim().isEmpty() || port.trim().isEmpty()
                || path.trim().isEmpty() || code.trim().isEmpty()
                || app_v.trim().isEmpty() || apiversion.trim().isEmpty()
                || platform.trim().isEmpty() || data.trim().isEmpty() ){

            log.info("---host,port及等不能为空");

            results.setSuccessful(false);

        }else {

            HttpRequester request = new HttpRequester(host + ":" + port + path);
           //添加参数前需先清空,不然设置循环次数时会重复加参
            HttpRequester.params.clear();

            HttpRequester.addParams("code", code);

            HttpRequester.addParams("app_v", app_v);

            HttpRequester.addParams("apiversion", apiversion);

            HttpRequester.addParams("platorm", platform);

            HttpRequester.addParams("data", data);


            for (int i = 0; i < HttpRequester.params.size(); i++) {

                log.info("!!!" + HttpRequester.params.get(i));

            }
            //发起post请求
            String resp = request.sendPost();

            ;
            //设置请求数据

            results.setSamplerData(HttpRequester.params.toString());

            //        results.setSamplerData(HttpRequester.httppost.getEntity().toString());

//        results.setSamplerData(HttpRequester.httppost.getParams().toString());
            //设置返回数据
            results.setResponseData(resp);
           //设置数据类型
            results.setDataType(SampleResult.TEXT);


            if (resp != null) {

                log.info("@@@@@" + resp);

                results.setSuccessful(true);

            } else {

                results.setResponseMessage("返回失败");

                results.setSuccessful(false);
            }


        }

      //结束事物
        results.sampleEnd();


        return results;
    }


    @Override
    public void teardownTest(JavaSamplerContext context){

        HttpRequester.params.clear();

        log.info("!!!Test End!!!");
    }






}
