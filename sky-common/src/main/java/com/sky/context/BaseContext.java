package com.sky.context;

/**
 * 工具类，在同一线程中传递已登录用户id，该类的存储在不同线程有不同结果，依赖ThreadLocal
 * ThreadLocal 并不是一个Thread，而是Thread的局部变量。
 * ThreadLocal为每个线程提供单独一份存储空间，具有线程隔离的效果，只有在线程内才能获取到对应的值，线程外则不能访问。
 */
public class BaseContext {
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
