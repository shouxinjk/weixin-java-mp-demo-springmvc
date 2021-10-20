package com.ilife.util;

import java.util.HashMap;
import java.util.Map;

public class CacheSingletonUtil {

    private static volatile CacheSingletonUtil cacheSingletonUtil;
    private static Map<String,Object> cacheSingletonMap;

    private CacheSingletonUtil(){
        cacheSingletonMap = new HashMap<String, Object>();
    }

    /*
     * 单例模式有两种类型
     * 懒汉式：在真正需要使用对象时才去创建该单例类对象
     * 饿汉式：在类加载时已经创建好该单例对象，等待被程序使用
     */

    // 懒汉式单例模式
    public static CacheSingletonUtil getInstance(){
        if (cacheSingletonUtil == null){// 线程A和线程B同时看到cacheSingletonUtil = null，如果不为null，则直接返回cacheSingletonUtil
            synchronized (CacheSingletonUtil.class) {// 线程A或线程B获得该锁进行初始化
                if (cacheSingletonUtil == null) {// 其中一个线程进入该分支，另外一个线程则不会进入该分支
                    cacheSingletonUtil = new CacheSingletonUtil();
                }
            }
        }
        return cacheSingletonUtil;
    }

    /**
     * 添加到内存
     */
    public void addCacheData(String key,Object obj){
        cacheSingletonMap.put(key,obj);
    }

    /**
     * 从内存中取出
     */
    public Object getCacheData(String key){
        return cacheSingletonMap.get(key);
    }

    /**
     * 从内存中清除
     */
    public void removeCacheData(String key){
        cacheSingletonMap.remove(key);
    }

}
