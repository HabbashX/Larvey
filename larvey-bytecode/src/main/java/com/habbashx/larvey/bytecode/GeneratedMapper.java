package com.habbashx.larvey.bytecode;

import com.habbashx.larvey.semantic.Configuration;

public interface GeneratedMapper<T> {
    T map(Configuration config);
}
