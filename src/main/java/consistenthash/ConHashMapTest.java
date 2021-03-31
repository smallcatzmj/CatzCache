package consistenthash;

/**
 * @description: 一致性Hash测试
 * @author: zmj
 * @date 2021/3/29 9:14
 */
public class ConHashMapTest {
    public static void main(String[] args) {
        ConHashMap conHashMap = new ConHashMap(3);
        conHashMap.add("1","50","100");
        System.out.println(conHashMap.get("0"));
        System.out.println(conHashMap.get("2"));
        System.out.println(conHashMap.get("8"));
        System.out.println(conHashMap.get("999"));
        conHashMap.add("150");
        System.out.println(conHashMap.get("0"));
        System.out.println(conHashMap.get("2"));
        System.out.println(conHashMap.get("8"));
        System.out.println(conHashMap.get("999"));
    }
}
