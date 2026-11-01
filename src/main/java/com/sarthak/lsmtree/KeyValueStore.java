package com.sarthak.lsmtree;

import java.util.HashMap;
import java.util.Map;

public class KeyValueStore {
    private Map<String, String> map = new HashMap<>();

    public  void put(String key, String val) {
        map.put(key, val);
    }

    public String get(String key){
        return map.get(key);
    }

    public String delete(String key){
        return map.remove(key);
    }

    public static void main(String[] args) {
        KeyValueStore db = new KeyValueStore();
        db.put("Language", "Python");
        String lang = db.get("Language");
        
        System.out.println(lang);
    }
}




