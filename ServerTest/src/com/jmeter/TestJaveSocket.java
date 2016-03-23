package com.jmeter;

import com.jmeter.protocol.MsgProto;
import com.jmeter.utils.BuildMsg;
import com.jmeter.utils.Funcations;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;

import static com.jmeter.utils.VideoTranscoding.changeVideoByte;
import static com.sun.org.apache.xml.internal.security.utils.JavaUtils.getBytesFromFile;


public class TestJaveSocket extends AbstractJavaSamplerClient {
	
	
	private Logger log=getLogger();
	
	private SampleResult results;

	SocketClient sc;

	byte[] bVoice;

	byte[] sbVoice;


	String vc;
	
	private String host;

	private String port;

	private String user_id;

	private String RequestTimeout;

	private String ResponseTimeout;

	private String socketType;

	private String isWatchSendVoice;

	private MsgProto.Message message;

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

		 user_id = context.getParameter( "user_id" , "" );

		 socketType=context.getParameter("socketType(登录:1 长连接:2 通知:3)","");

		 isWatchSendVoice=context.getParameter("isVoice","");

//		  watchId = context.getParameter("watchId", "");

	   }




	public Arguments getDefaultParameters(){
		
		
		log.info("execute getDefaultParameters...");
		
		Arguments params= new Arguments();
		
		params.addArgument("host", "");
		
		params.addArgument("port","");
		
		params.addArgument("RequestTimeout","");
		
		params.addArgument("ResponseTimeout","");
		
		params.addArgument("user_id","");

		params.addArgument("socketType(登录:1 长连接:2 通知:3)","");

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


		if(socketType.equals("1")){

			message = BuildMsg.loginByUserId(user_id);

		}else if(socketType.equals("2")){

            message=BuildMsg.getLinkMessage(MsgProto.MessageType.Link_Request);

		}else if(socketType.equals("3")){

			

		}

		sc.sndByte(BuildMsg.getByteArrayByMessage(message));

		log.info("发送的命令类型为: "+message.getMsgType()+" 登录id: "+message.getRequest().getLoginRequest().getUserId()+" 序列:  "+message.getSequence());

		results.setSamplerData("发送的命令类型为: "+message.getMsgType()+" 登录id: "+message.getRequest().getLoginRequest().getUserId()+" 序列:  "+message.getSequence());

		log.info("socket 是否连接:  "+sc.isConnected());

		while((sc != null && sc.isConnected())){
			
	  try{


		  try {
			    log.info("设置的请求超时时间为: "+RequestTimeout);

			    sc.setRespTimeOut(Integer.parseInt(ResponseTimeout));

			    log.info("开始接收返回数据");

				MsgProto.Message mmResp = MsgProto.Message.parseFrom(sc.rcvProtoBuf());


				
				log.info("socket返回: "+" 消息类型为: "+ mmResp.getMsgType() + " 序列为: "+mmResp.getSequence());
				
				results.setResponseData(" 消息类型为: "+ mmResp.getMsgType() + " 序列为: "+mmResp.getSequence());
				
				results.setDataType(SampleResult.TEXT);
				
				
			 if(mmResp.getResponse().hasResult()){

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


	
	
	

    
   