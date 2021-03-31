package util;

import java.io.*;

/**
 * @description: 字节数组对象转换工具类
 * @author: zmj
 * @date 2021/3/26 11:04
 */
public class ByteArrayUtil {
    //对象转字节数组
    public static <T> byte[] obj2Byte(T obj) {
        byte[] bytes = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream sOut;
        try {
            sOut = new ObjectOutputStream(out);
            sOut.writeObject(obj);
            sOut.flush();
            bytes = out.toByteArray();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    //字节数组转对象
    public static Object byte2Obj(byte[] bytes) {
        Object obj = null;
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        ObjectInputStream sInput;
        try {
            sInput = new ObjectInputStream(input);
            obj = sInput.readObject();
        }catch (Exception e){
            e.printStackTrace();
        }
        return obj;
    }
}
