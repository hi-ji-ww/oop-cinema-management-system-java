package ru.etu.cinema;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import javax.servlet.http.*;
/** Выводит афишу на русском или английском языке. */
public class LocalizedServlet extends HttpServlet {
 private static final long serialVersionUID=1L;
 /** Выбирает ресурсы по параметру lang и форматирует цены.
  * @param req запрос
  * @param res ответ
  * @throws IOException при ошибке вывода */
 @Override public void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException {
  String lang="en".equals(req.getParameter("lang"))?"en":"ru";
  Locale locale="en".equals(lang)?Locale.US:new Locale("ru","RU");
  ResourceBundle b=ResourceBundle.getBundle("messages",locale);
  NumberFormat money=NumberFormat.getCurrencyInstance(locale);
  money.setCurrency(Currency.getInstance("RUB"));
  res.setLocale(locale); res.setContentType("text/html;charset=UTF-8");
  PrintWriter out=res.getWriter();
  out.print("<!doctype html><html lang='"+lang+"'><head><meta charset='UTF-8'><title>"+b.getString("title")
   +"</title><link rel='stylesheet' href='style.css'></head><body><h1>"+b.getString("title")
   +"</h1><a href='catalog?lang=ru'>Русский</a> | <a href='catalog?lang=en'>English</a>"
   +"<table><tr><th>"+b.getString("film")+"</th><th>"+b.getString("genre")+"</th><th>"+b.getString("price")+"</th></tr>");
  String[] names={"city","space","road"}; int[] prices={350,450,400};
  for(int i=0;i<names.length;i++) out.print("<tr><td>"+b.getString(names[i])+"</td><td>"
   +b.getString(i==0?"comedy":"drama")+"</td><td>"+money.format(prices[i])+"</td></tr>");
  out.print("</table></body></html>");
 }
}
