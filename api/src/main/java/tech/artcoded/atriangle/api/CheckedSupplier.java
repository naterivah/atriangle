package tech.artcoded.atriangle.api;

import java.io.IOException;
import java.util.function.Supplier;

@FunctionalInterface
public interface CheckedSupplier<T> {
  T get() throws Exception;

  default T safeGet(){
    try {
      return get();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
