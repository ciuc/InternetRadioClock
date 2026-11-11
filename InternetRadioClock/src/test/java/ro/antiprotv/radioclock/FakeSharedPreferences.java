package ro.antiprotv.radioclock;

import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** An in-memory SharedPreferences for unit tests; edits apply at once. */
public class FakeSharedPreferences implements SharedPreferences {

  private final Map<String, Object> values = new HashMap<>();

  @Override
  public Map<String, ?> getAll() {
    return new HashMap<>(values);
  }

  @Override
  public String getString(String key, String defValue) {
    return values.containsKey(key) ? (String) values.get(key) : defValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getStringSet(String key, Set<String> defValues) {
    return values.containsKey(key) ? (Set<String>) values.get(key) : defValues;
  }

  @Override
  public int getInt(String key, int defValue) {
    return values.containsKey(key) ? (Integer) values.get(key) : defValue;
  }

  @Override
  public long getLong(String key, long defValue) {
    return values.containsKey(key) ? (Long) values.get(key) : defValue;
  }

  @Override
  public float getFloat(String key, float defValue) {
    return values.containsKey(key) ? (Float) values.get(key) : defValue;
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    return values.containsKey(key) ? (Boolean) values.get(key) : defValue;
  }

  @Override
  public boolean contains(String key) {
    return values.containsKey(key);
  }

  @Override
  public Editor edit() {
    return new FakeEditor();
  }

  @Override
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

  @Override
  public void unregisterOnSharedPreferenceChangeListener(
      OnSharedPreferenceChangeListener listener) {}

  private class FakeEditor implements Editor {
    private final Map<String, Object> changes = new HashMap<>();
    private final Set<String> removals = new HashSet<>();
    private boolean clear;

    @Override
    public Editor putString(String key, String value) {
      changes.put(key, value);
      return this;
    }

    @Override
    public Editor putStringSet(String key, Set<String> value) {
      changes.put(key, value);
      return this;
    }

    @Override
    public Editor putInt(String key, int value) {
      changes.put(key, value);
      return this;
    }

    @Override
    public Editor putLong(String key, long value) {
      changes.put(key, value);
      return this;
    }

    @Override
    public Editor putFloat(String key, float value) {
      changes.put(key, value);
      return this;
    }

    @Override
    public Editor putBoolean(String key, boolean value) {
      changes.put(key, value);
      return this;
    }

    @Override
    public Editor remove(String key) {
      removals.add(key);
      return this;
    }

    @Override
    public Editor clear() {
      clear = true;
      return this;
    }

    @Override
    public boolean commit() {
      if (clear) {
        values.clear();
      }
      for (String key : removals) {
        values.remove(key);
      }
      values.putAll(changes);
      return true;
    }

    @Override
    public void apply() {
      commit();
    }
  }
}
