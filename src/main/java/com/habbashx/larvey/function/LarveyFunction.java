package com.habbashx.larvey.function;

import com.habbashx.larvey.semantic.LarveyValue;
import java.util.List;

public interface LarveyFunction {
    String name();
    LarveyValue invoke(List<LarveyValue> arguments);
}
