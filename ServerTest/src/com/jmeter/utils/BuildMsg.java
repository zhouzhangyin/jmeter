package com.jmeter.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jmeter.protocol.MsgProto;

/**
 * Created by zhouzhangyin on 16/3/15.
 */
public class BuildMsg {

      public static int sequence = 0;

      public  static MsgProto.Message loginByUserId(String user_id){

      //创建登录请求builder
       MsgProto.LoginRequest.Builder mlBuilder = MsgProto.LoginRequest.newBuilder();
      //set所需参数
          mlBuilder.setUserId(user_id);

          mlBuilder.setSign("1");
       //  创建登录请求
          MsgProto.LoginRequest ml = mlBuilder.build();

         // 创建登录请求builder
          MsgProto.Request.Builder mrBuilder = MsgProto.Request.newBuilder().setLoginRequest(ml);

      //创建消息builder
          MsgProto.Message.Builder mmBuilder = MsgProto.Message.newBuilder();
      //把登录请求放进message
          mmBuilder.setRequest(mrBuilder);

       //把消息类型放进message
          mmBuilder.setMsgType(MsgProto.MessageType.Login_Request);

       //把sequence放进message
          mmBuilder.setSequence(sequence);
       //构建message
          MsgProto.Message mm = mmBuilder.build();


          return  mm;

      }

    /**
     * 获取长连接
     * @param messageType
     * @return
     */

    public static MsgProto.Message getLinkMessage(MsgProto.MessageType messageType){
        MsgProto.Message.Builder messageBuilder = MsgProto.Message.newBuilder();
        messageBuilder.setMsgType(messageType);
        messageBuilder.setSequence(sequence);

        return messageBuilder.build();
    }



    /**
     * 根据byte数组获取Message
     * @return
     */
    public MsgProto.Message getMessageByteArray(byte [] bytes){

        byte[] proBufLengthByte = new byte[4];
        System.arraycopy(bytes, 3, proBufLengthByte, 0, 4);
        int dataLength =Funcations.byteArray2Int(proBufLengthByte);
        if (dataLength==0)
            return null;
        byte[] proBufByte = new byte[dataLength];
        System.arraycopy(bytes, 19, proBufByte, 0, dataLength);
        MsgProto.Message.Builder mergeFrom = null;
        try {
            mergeFrom = MsgProto.Message.getDefaultInstance().newBuilderForType().mergeFrom(proBufByte);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        assert mergeFrom != null;
        MsgProto.Message message = mergeFrom.build();
//        MsgProto.MessageType msgType = build.getMsgType();
        return message;
    }




    /**
     * 根据Message获取Byte数组
     * @return
     */
    public static  byte[] getByteArrayByMessage(MsgProto.Message message){
        int serializedSize = message.getSerializedSize();
        byte[] result = new byte[19 + serializedSize];
        System.arraycopy(Funcations.int2Byte(1), 0, result, 0, 1);//协议头标志：1 类型：char,1byte
        System.arraycopy(Funcations.int2Byte(1), 0, result, 1, 1);//协议版本：1 类型：char,1byte
        System.arraycopy(Funcations.int2Byte(19), 0, result, 2, 1);//头部长度：19 类型：char,1byte
        System.arraycopy(Funcations.int2ByteArray(serializedSize), 0, result, 3, 4);//数据长度：程序计算,4byte
        System.arraycopy(Funcations.int2ByteArray(sequence), 0, result, 7, 4);//序列号：程序自增计算,4byte
        System.arraycopy(Funcations.int2ByteArray(0), 0, result, 11, 4);//保留字段：填0,4byte
        System.arraycopy(Funcations.int2ByteArray(0), 0, result, 15, 4);//保留字段：填0,4byte
        System.arraycopy(message.toByteArray(), 0, result, 19, serializedSize);
        sequence++;
        return result;
    }




}
