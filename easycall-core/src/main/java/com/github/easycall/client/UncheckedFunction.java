package com.github.easycall.client;

@FunctionalInterface
public interface UncheckedFunction<R> {

    R apply() throws Exception;
}

