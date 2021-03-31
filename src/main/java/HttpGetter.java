import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @description: Http客户端
 * @author: zmj
 * @date 2021/3/29 9:59
 */
public class HttpGetter {
    private String baseUrl; //通讯路径

    //peer为ip:port
    public HttpGetter(String peer) {
        this.baseUrl = "http://" + peer + "/catzCache/";
    }

    /**
     * @Param group
     * @Description: 缓存的命名空间
     * @param key
     * @Description: 传入key用来选择节点
     * */
    public String get(String group, String key) {
        String result = null;
        HttpURLConnection conn = null;
        InputStream input = null;
        BufferedReader reader = null;
        try{
            URL url = new URL(this.baseUrl + group + "/" + key); //拼接访问地址
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 11; Windows NT 5.1)");
            conn.connect();
            if (conn.getResponseCode() == 200){
                input = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                StringBuilder sbf = new StringBuilder();
                String tmp = null;
                while ((tmp = reader.readLine()) != null){
                    sbf.append(tmp);
                    sbf.append("\r\n");
                }
                result = sbf.toString();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (input != null) {
                try {
                    input.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
            conn.disconnect();
        }
        return result;
    }
}
