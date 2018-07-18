package com.github.easycall.util;

@FunctionalInterface
public interface UncheckedFunction<R> {

    R apply() throws Exception;
}

