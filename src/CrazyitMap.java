import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CrazyitMap<K, V> extends HashMap<K, V> {
    /*
    根据value来删除指定项，用于删除异常联系人
     */
    void removeByValue(Object value) {
        for (Object key : keySet()) {
            if (get(key) == value) {
                remove(key);
                break;
            }
        }
    }

    /*
    获取所有value组成的Set，用于公聊的接受者
     */
    Set<V> valueSet() {
        Set<V> result = new HashSet<>();
        for (K key : keySet()) {
            result.add(get(key));
        }
        return result;
    }

    /*
    根据value查找key，用于私聊的发起者
     */
    K getKeyByValue(V val) {
        for (K key : keySet()) {
            if (get(key).equals(val) && get(key) == val) {
                return key;
            }
        }
        return null;
    }

    /*
    重写HashMap的put方法，该方法不允许value重复
     */
    public V put(K key, V value) {
        for (V val : valueSet()) {
            if (val.equals(value) && val.hashCode() == value.hashCode()) {
                throw new RuntimeException("MyMap实例中不允许有重复value!");
            }
        }
        return super.put(key, value);
    }
}
