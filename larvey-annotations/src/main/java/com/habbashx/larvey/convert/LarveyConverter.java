package com.habbashx.larvey.convert;

public interface LarveyConverter<S, T> {
    T convert(S value);
}
