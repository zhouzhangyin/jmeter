package com.jmeter.utils;

/**
 * Created by zhouzhangyin on 16/2/25.
 */
public class VideoTranscoding {



    public static  byte[] changeVideoByte(byte[] video){

        int videoLength = video.length;


        for(int k=0;k<video.length;k++){

            switch(video[k]) {
                case 0x7D:

                    videoLength++;
                    break;
                case 0x5B:
                    videoLength++;
                    break;
                case 0x5D:

                    videoLength++;
                    break;
                case 0x2C:

                    videoLength++;
                    break;
                case 0x2A:

                    videoLength++;
                    break;
                default:

                    break;

            }


        }


        byte[] nb = new byte[videoLength];

        int j=0;

        for(int i=0;i<video.length;i++){

            switch(video[i])
            {
                case 0x7D:
//					log.info("开始转义了1");
                    nb[j]=0x7D;
                    nb[j+1]=0x01;
                    j++;
                    break;
                case 0x5B:
//					log.info("开始转义了2");
                    nb[j]=0x7D;
                    nb[j+1]=0x02;
                    j++;
                    break;
                case 0x5D:

//					log.info("开始转义了3");
                    nb[j]=0x7D;
                    nb[j+1]=0x03;
                    j++;
                    break;
                case 0x2C:

//					log.info("开始转义了4");
                    nb[j]=0x7D;
                    nb[j+1]=0x04;
                    j++;
                    break;
                case 0x2A:

//					log.info("开始转义了5");
                    nb[j]=0x7D;
                    nb[j+1]=0x05;
                    j++;
                    break;
                default:
//					log.info("不用转义了");
                    nb[j]=video[i];
                    j++;
                    break;
            }

        }

//        nb[videoLength]=0x5D;

        return nb;
    }




}
