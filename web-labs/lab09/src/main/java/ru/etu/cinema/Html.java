package ru.etu.cinema;
/** Экранирование данных перед включением в HTML. */
public final class Html {
 private Html() {}
 /** Возвращает безопасное текстовое представление значения.
  * @param value исходная строка
  * @return экранированная строка */
 public static String escape(String value) {
  if(value==null) return "";
  return value.replace("&","&amp;").replace("<","&lt;")
   .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
 }
}
