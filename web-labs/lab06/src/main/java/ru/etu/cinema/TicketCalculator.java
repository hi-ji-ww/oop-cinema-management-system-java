package ru.etu.cinema;
import java.math.BigDecimal;
/** Расчет стоимости билетов без погрешности двоичной арифметики. */
public final class TicketCalculator {
 private TicketCalculator() {}
 /** Рассчитывает сумму заказа.
  * @param count количество от 1 до 10
  * @param student признак льготы 20 процентов
  * @return сумма в рублях
  * @throws IllegalArgumentException если количество вне диапазона */
 public static BigDecimal total(int count,boolean student) {
  if(count<1||count>10) throw new IllegalArgumentException("Количество билетов должно быть от 1 до 10");
  BigDecimal total=new BigDecimal("400.00").multiply(BigDecimal.valueOf(count));
  return student?total.multiply(new BigDecimal("0.80")).setScale(2):total;
 }
}
