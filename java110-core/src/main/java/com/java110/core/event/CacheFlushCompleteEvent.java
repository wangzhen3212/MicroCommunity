package com.java110.core.event;

import org.springframework.context.ApplicationEvent;

/**
 * 缓存刷新完成事件
 * 当系统启动时缓存刷新完成后发布此事件
 * 其他模块可以监听此事件来执行需要在缓存加载完成后才能进行的业务
 */
public class CacheFlushCompleteEvent extends ApplicationEvent {

    public CacheFlushCompleteEvent(Object source) {
        super(source);
    }
}