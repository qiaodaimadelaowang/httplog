package com.github.gobars.httplog.snack.core.utils;

import com.github.gobars.httplog.snack.core.exts.FieldWrap;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Clob;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;

/** Bean工具类 */
public class BeanUtil {
  public static final Map<String, Class<?>> CLZ_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Collection<FieldWrap>> FIELDS_CACHE = new HashMap<>();

  @SneakyThrows
  public static Class<?> loadClass(String clzName) {
    Class<?> clz = CLZ_CACHE.get(clzName);
    if (clz == null) {
      clz = Class.forName(clzName);
      CLZ_CACHE.put(clzName, clz);
    }

    return clz;
  }

  /** 获取一个类的所有字段 （已实现缓存） */
  public static Collection<FieldWrap> getAllFields(Class<?> clz) {
    String key = clz.getName();

    Collection<FieldWrap> list = FIELDS_CACHE.get(key);
    if (list != null) {
      return list;
    }

    synchronized (FIELDS_CACHE) {
      list = FIELDS_CACHE.get(key);
      if (list != null) {
        return list;
      }

      Map<String, FieldWrap> map = new LinkedHashMap<>();
      scanAllFields(clz, map);

      list = map.values();

      FIELDS_CACHE.put(key, list);
      return list;
    }
  }

  /** 扫描一个类的所有字段 */
  private static void scanAllFields(Class<?> clz, Map<String, FieldWrap> fields) {
    if (clz == null) {
      return;
    }

    for (Field f : clz.getDeclaredFields()) {
      int mod = f.getModifiers();

      if (!Modifier.isTransient(mod) && !Modifier.isStatic(mod)) {
        f.setAccessible(true);

        if (!fields.containsKey(f.getName())) {
          fields.put(f.getName(), new FieldWrap(f));
        }
      }
    }

    Class<?> sup = clz.getSuperclass();
    if (sup != Object.class) {
      scanAllFields(sup, fields);
    }
  }

  /** 将 Clob 转为 String */
  public static String clobToString(Clob clob) {
    Reader reader;
    StringBuilder buf = new StringBuilder();

    try {
      reader = clob.getCharacterStream();

      char[] chars = new char[2048];
      for (; ; ) {
        int len = reader.read(chars, 0, chars.length);
        if (len < 0) {
          break;
        }
        buf.append(chars, 0, len);
      }
    } catch (Exception ex) {
      throw new RuntimeException("read string from reader error", ex);
    }

    String text = buf.toString();

    try {
      reader.close();
    } catch (Exception ex) {
      throw new RuntimeException("read string from reader error", ex);
    }

    return text;
  }
}
