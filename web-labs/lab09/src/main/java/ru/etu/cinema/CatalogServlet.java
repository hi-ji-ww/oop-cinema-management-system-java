package ru.etu.cinema;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.*;
/** Формирует афишу кинотеатра с фильтром по жанру. */
public class CatalogServlet extends HttpServlet {
 private static final long serialVersionUID=1L;
 private String cinema;
 @Override public void init() throws ServletException {
  cinema=getInitParameter("cinemaName");
  if(cinema==null) cinema="Кинотеатр Север";
 }
 /** Обрабатывает параметр genre и печатает таблицу сеансов.
  * @param req HTTP запрос
  * @param res HTTP ответ
  * @throws IOException при ошибке записи ответа */
 @Override public void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException {
  req.setCharacterEncoding("UTF-8");
  res.setContentType("text/html;charset=UTF-8");
  String genre=req.getParameter("genre");
  if(genre==null) genre="all";
  if(!genre.equals("all")&&!genre.equals("drama")&&!genre.equals("comedy")) {
   res.sendError(400,"Неизвестный жанр"); return;
  }
  PrintWriter out=res.getWriter();
  out.print("<!doctype html><html lang='ru'><head><meta charset='UTF-8'><title>Афиша</title>"
   +"<link rel='stylesheet' href='style.css'></head><body><h1>"+Html.escape(cinema)+"</h1>"
   +"<form method='get' action='catalog'><label>Жанр <select name='genre'>"
   +"<option value='all'>Все жанры</option><option value='drama'>Драма</option>"
   +"<option value='comedy'>Комедия</option></select></label><button>Показать</button></form>"
   +"<p>Фильтр: "+Html.escape(genre)+"</p><table><tr><th>Фильм</th><th>Жанр</th><th>Начало</th><th>Цена, руб.</th></tr>");
  String[][] rows={{"Огни города","comedy","18:00","350"},
   {"Космический рейс","drama","19:30","450"},{"Дорога домой","drama","21:00","400"}};
  int count=0;
  for(String[] row:rows) {
   if(!genre.equals("all")&&!genre.equals(row[1])) continue;
   out.print("<tr><td>"+Html.escape(row[0])+"</td><td>"+(row[1].equals("drama")?"Драма":"Комедия")
    +"</td><td>"+row[2]+"</td><td>"+row[3]+"</td></tr>"); count++;
  }
  out.print("</table><p>Найдено сеансов: "+count+"</p></body></html>");
 }
}
