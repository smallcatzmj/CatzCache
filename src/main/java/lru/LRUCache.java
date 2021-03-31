package lru;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * @description: LRU最近最少使用
 * @author: zmj
 * @date 2021/3/24 16:03
 */
public class LRUCache<K, V> {
    private int size; //缓存队列大小
    private HashMap<K, Node> map; //存放节点的Hash映射
    private Node head; //LRU队列头节点
    private Node tail; //LRU队列尾节点
    //节点删除后的回调函数，BiConsumer接口表示一个带有两个参数(T，U)且不返回结果的操作
    private BiConsumer<K, V> callBack;

    //链表节点，双向队列
    class Node{
        K k;
        V v;
        Node pre;
        Node next;

        Node(K k, V v){
            this.k = k;
            this.v = v;
        }
    }

    //初始化
    public LRUCache(int size,BiConsumer<K, V> func){
        this.size = size;
        map = new HashMap<>();
        callBack = func;
    }

    /**
     * 添加/删除元素
     *1、元素存在，移动到队头
     *2、元素不存在，判断链表是否满
     *  a.满了，删除队尾元素，将元素加入队头，删除更新哈希表
     *  b.没满，直接将元素加入队头，更新哈希表
     **/
    public void put(K key, V value){
        Node node = map.get(key);
        //节点存在，更新节点值
        if (node != null){
            node.v = value;
            moveNode2Head(node);
        }else {
            Node newNode = new Node(key,value);
            //队列已经满了
            if (map.size() == size){
                Node delHead = removeTail();
                //删除队尾的键值映射
                map.remove(delHead.k);
                //回调
                if (callBack != null){
                    callBack.accept(delHead.k, delHead.v);
                }
            }
            //头部添加节点，添加键值映射
            addHead(newNode);
            map.put(key,newNode);
        }
    }
    public V get(K key){
        Node node = map.get(key);
        if (node != null){
            moveNode2Head(node);
            return node.v;
        }
        return null;
    }
    //添加新元素至队头
    public void addHead(Node newNode){
        if (newNode == null){
            return;
        }
        //缓存中没有节点，初始化头尾节点为新节点
        if (head == null){
            head = newNode;
            tail = newNode;
        }else {
            //连接新节点
            head.next = newNode;
            newNode.pre = head;
            //头节点指向新节点
            head = newNode;
        }
    }

    //将元素移至队头
    public void  moveNode2Head(Node node){
        if (head == node){
            return;
        }else if (tail == node){
            tail = node.next;
            tail.pre = null;
        }else {
            node.pre.next = node.next;
            node.next.pre = node.pre;
        }
        node.pre = head;
        node.next = null;
        head.next = node;
        head = node;
    }

    //删除队尾元素
    public Node removeTail(){
        if (tail == null){
            return null;
        }
        Node tmp = tail;
        //只有一个节点，头尾节点相同，置空
        if(head == tail){
            head = null;
            tail = null;
        }else {
            tail = tmp.next;
            tail.pre = null;
            tmp.next = null;
        }
        return tmp;
    }
}
