package com.github.easycall.core.util;

@FunctionalInterface
public interface UncheckedFunction<R> {

    R apply() throws Exception;
}

