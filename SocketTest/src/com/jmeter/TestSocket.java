package com.jmeter;
import java.net.SocketTimeoutException;
import java.util.Iterator;


import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Logger;


public class TestSocket extends AbstractJavaSamplerClient {
	
	
	private Logger log=getLogger();
	
	private SampleResult results;
	
	private String host;
	
	private String port;
	
	private String Data;
	
	private String RequestTimeout;
	
	private String ResponseTimeout;

	
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

	   }




	public Arguments getDefaultParameters(){
		
		
		log.info("execute getDefaultParameters...");
		
		Arguments params= new Arguments();
		
		params.addArgument("host", "");
		
		params.addArgument("port","");
		
		params.addArgument("RequestTimeout","");
		
		params.addArgument("ResponseTimeout","");
		
		params.addArgument("Data","");
		
		
		return params;
		
		
	}
	
	
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		
		log.info("exec runTest...");
		
		results = new SampleResult();
		
		results.sampleStart();
		
		setupValues(context);
		
		
		 if (host.length()>0) {

	         results.setSamplerData(Data);

	      }
		
		 
		 SocketClient sc = new SocketClient(host, Integer.parseInt(port), RequestTimeout);
		 
		 sc.sndData(Data);
		 
		 while((sc != null && sc.isConnected())){
			
	  try{
			 
			 
		  try {
			    sc.setRespTimeOut(Integer.parseInt(ResponseTimeout));
			    
				String resp = sc.rcvData(100);
				
				log.info("socket返回: "+resp);
				
				results.setResponseData(resp);
				
				results.setDataType(SampleResult.TEXT);
				
				
				if(resp!=null){
					
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
				log.info(e.getMessage());
			}
			 
		 }
		 
		sc.closeConnection();
		 
		results.sampleEnd();
		
		
		return results;
		
	}
	
	
	
	
	
	













}


	
	
	

    
   