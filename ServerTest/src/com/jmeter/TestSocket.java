package com.jmeter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;


import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Logger;

import static com.jmeter.utils.Funcations.bytesToHexString;
import static com.jmeter.utils.VideoTranscoding.changeVideoByte;
import static com.sun.org.apache.xml.internal.security.utils.JavaUtils.getBytesFromFile;


public class TestSocket extends AbstractJavaSamplerClient {
	
	
	private Logger log=getLogger();
	
	private SampleResult results;

	SocketClient sc;

	byte[] bVoice;

	byte[] sbVoice;


	String vc;
	
	private String host;

	private String port;

	private String Data;

	private String RequestTimeout;

	private String ResponseTimeout;

	private String filePath;

	private String isWatchSendVoice;

	private String watchId;

	public void setupTest(JavaSamplerContext context){
		
		listParameters(context);	
		
	 }
	

	
	private void listParameters(JavaSamplerContext context) {
	
      String name;
      
      for ( Iterator argsIt= context.getParameterNamesIterator(); 
    		  
    		   argsIt.hasNext(); 
    		    
    		      log.debug(( new StringBuilder()).append(name).append( "=" ).append(context.getParameter(name)).toString()))

                    name= (String) argsIt.next();
	
       }

	
	
	 private void setupValues(JavaSamplerContext context) {

	      host = context.getParameter( "host" , "" );

	      port =context.getParameter( "port" , "" );

	      RequestTimeout = context.getParameter( "RequestTimeout" , "" );

	      ResponseTimeout = context.getParameter( "ResponseTimeout" , "" );
	      
	      Data = context.getParameter( "Data" , "" );

		  filePath=context.getParameter("filePath","");

		 isWatchSendVoice=context.getParameter("isVoice","");

		  watchId = context.getParameter("watchId", "");

	   }




	public Arguments getDefaultParameters(){
		
		
		log.info("execute getDefaultParameters...");
		
		Arguments params= new Arguments();
		
		params.addArgument("host", "");
		
		params.addArgument("port","");
		
		params.addArgument("RequestTimeout","");
		
		params.addArgument("ResponseTimeout","");
		
		params.addArgument("Data","");

		params.addArgument("filePath","");

		params.addArgument("isWatchSendVoice","");

		params.addArgument("watchId","");
		
		
		return params;
		
		
	}
	
	
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		
		log.info("exec runTest...");
		
		results = new SampleResult();
		
		results.sampleStart();
		
		setupValues(context);

		sc = new SocketClient(host, Integer.parseInt(port), RequestTimeout);

		log.info("发送的ip及端口为: "+host+":"+port);

		log.info("isWatchSendVoice="+isWatchSendVoice);

       if(!isWatchSendVoice.equals("") || !isWatchSendVoice.equals("true") || !isWatchSendVoice.equals("yes")){

		   sc.setId(watchId);

		   try {

			      bVoice=getBytesFromFile(filePath);

		    } catch (IOException e) {

			     e.printStackTrace();

			   log.info("读取文件异常");
		   }

		   sc.setAmr(bVoice);

		   byte[] newVoice = changeVideoByte(sc.getAmr());

		   sc.setLen(newVoice.length);

//		   sc.setAmr(sc.getChars(sc.getBytesFromFile(filePath)));
//		   sbVoice=getSb();

//		   vc=bytesToHexString(newVoice);

//		   sbVoice=bytesToHexString(addToNewByte(getHeadSb(),sc.getAmr()));

		   sbVoice=addToNewByte(getHeadSb(),newVoice);

//		   sbVoice=getHeadSb().append(vc).append("]").toString();

		   log.info("发送的语音命令为: "+sbVoice);

           sc.sndByte(sbVoice);

		   results.setSamplerData(new String(sbVoice));

	   }else {

		   sc.sndData(Data);

		   log.info("发送的字符串命令为: "+Data);

		   results.setSamplerData(Data);
	   }


		while((sc != null && sc.isConnected())){
			
	  try{


		  try {
			    log.info("设置的请求超时时间为: "+RequestTimeout);

			    sc.setRespTimeOut(Integer.parseInt(ResponseTimeout));

			    log.info("开始接收返回数据");

				String resp = sc.rcvData();
				
				log.info("socket返回: "+resp);
				
				results.setResponseData(resp);
				
				results.setDataType(SampleResult.TEXT);
				
				
				if(resp.contains("*39*")) {

					log.info("$$$$$手表不在线$$$");

					results.setResponseMessage("返回失败");

					results.setSuccessful(false);

				}else if(resp!=null){

						results.setSuccessful(true);
				}else{

					results.setResponseMessage("返回失败");

					results.setSuccessful(false);
				}


				
				
			}catch(SocketTimeoutException e){
				
				log.info("--读返回数据超时了--");
				
				break;
			}
		 } catch (Exception e) {
				// TODO Auto-generated catch block
				log.info("读取异常: "+e.getMessage());

		        break;
			}
			 
		 }
		 
		sc.closeConnection();
		 
		results.sampleEnd();
		
		
		return results;
		
	}



	public StringBuilder  getHeadSb(){

		StringBuilder sb = new StringBuilder();

		sb.append("[");

		sb.append("3G");

		sb.append("*");

		sb.append(sc.getId());

		sb.append("*");

		sb.append(sc.getLen());

		sb.append("*");

		sb.append("TK");


		sb.append(",");

		return  sb;
	}




	 public byte[] addToNewByte(StringBuilder sb,byte[] voice){

		  byte[] head = sb.toString().getBytes();

		  int len = head.length + voice.length+1;

		  log.info("新数组的长度为: "+len);

		  byte[] newbyte= new  byte[len];

		  int k=0;

          for(int i=0;i<head.length;i++){


			  newbyte[k]=head[i];

			  k++;


		  }


		 for (int j=0;j<voice.length;j++){

			 newbyte[k]=voice[j];

			 k++;

		 }

		 newbyte[len-1]=0x5D;

		 return newbyte;

	 }





}


	
	
	

    
   