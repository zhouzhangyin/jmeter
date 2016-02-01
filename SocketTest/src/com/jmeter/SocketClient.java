package com.jmeter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;



	
	public class SocketClient {
		 
		 
		 
		private static final int Default_Request_TimeOut = 3*1000;
		private static final int Default_Response_TimeOut=10*1000;
		private Socket s;
		private OutputStream serverOutput=null;
		private InputStream serverInput =null;
		private String host;
		private int port=-1;
		private String timeout=null;
		 
		/**
		 * 构造方法完成初始化
		 * @param host,port,timeout
		 */
		public SocketClient(String host,int port,String timeOut) {
		 
		 
		this.host=host;
		this.port=port;
		this.timeout=timeOut;
		openConnection();
		
		}
		
		public boolean isConnected(){
			return s != null && s.isConnected() && !s.isClosed();
		}
		 
		public void setRespTimeOut(int timeout){
			
			try {
				s.setSoTimeout(timeout);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		/**
		 * 打开连接的输入输出流
		 * @return boolean
		 */
		public boolean openConnection() {
		 
		 
		try {
		 
		//连接telnet服务器
		s = new Socket(host, port);
		 
		if(timeout != null && !timeout.equals("")) {
		 
		s.setSoTimeout(Integer.parseInt(timeout));
		 
		}
		else {
		 
		s.setSoTimeout(Default_Request_TimeOut);//3秒
		 
		}
		
		serverOutput = s.getOutputStream();
		serverInput = s.getInputStream();
		 
		}
		catch(Exception e) {
		 

		this.closeConnection();
		return false;
		 
		}
		return true;
		 
		}
		 
		/**
		 *关闭连接的输入输出流
		 *@author mick.ge
		 */
		public  void closeConnection() {
		 
		 
		try{
		 
		//关闭输出
		if(serverOutput!=null) {
		 
		serverOutput.close();
		 
		}
		 
		}catch(Exception e) {
		 
		 
		}
		 
		 
		try{
		 
		//关闭输入
		if(serverInput!=null) {
		 
		serverInput.close();
		 
		}
		 
		}catch(Exception e) {
		 
		 
		}
		 
		try{
		 
		if(s!=null) {
		 
		s.close();//关闭socket
		 
		}
		 
		}
		catch(Exception e) {
		 
	
		}
		 
		 
		}
		 
		/**
		 * 发送数据
		 * @param sndStr
		 * @return boolean
		 */
		public boolean sndData(String sndStr) {
		 
		 
		try {
		 
		serverOutput.write(sndStr.getBytes());
		serverOutput.flush();
		return true;
		 
		}
		catch(SocketTimeoutException ste) {
		     
		    closeConnection();
		    return false;
		     
		}   
		catch(Exception e) {
		 
		closeConnection();
		return false;
		 
		}
		 
		}
		 
		/**
		 * 接收数据
		 * @param lenSize
		 * @return 数据字符串
		 */
		public  String rcvData(int lenSize) throws Exception{
		 
		     
			byte[] b = new byte[100];
			int length = serverInput.read(b);
			String result = new String(b,0,length);
			return result;
			
//		    char[] line=null;
//		    char[] lenBuffer=new char[lenSize];
//		    String readLine=null;
//		 
//		    try {
//		 
//		 
//		    //读取规定字节数的字符
//		    for(int i=0;i<lenSize;i++) {
//		 
//		       lenBuffer[i]=(char)serverInput.read();
//		 
//		}
//		     
//		    //取得消息总长度
//		    int messageLength=Integer.valueOf((new String(lenBuffer)).trim()).intValue();
//		    
//		    line=new char[messageLength];
//		 
//		//将已读取的n个字节传给line
//		for (int i=0;i<lenSize;i++) {
//		 
//		     line[i]=lenBuffer[i];
//		 
//		}
//		 
//		//将剩余的n个字节传给line
//		for(int i=lenSize;i<messageLength;i++) {
//		 
//		     line[i]=(char)serverInput.read();
//		 
//		}
//		 
//		//生成字符串对象,写入日志
//		readLine=new String(line);
//		     
//		}
//		    catch(SocketTimeoutException ste) {
//		     
//		    readLine=null;
//		     
//		}   
//		    catch (Exception ex) {
//		 
//		    readLine=null;
//		     
//		}
//		     
//		    finally {
//		 
////		    this.closeConnection();
//		     
//		}
//		    return readLine;
//		  
		}
		 
		}
	
	
